package za.co.graphicdesign.pageharvest

import android.app.Application
import android.net.Uri
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class HarvestViewModel(app: Application) : AndroidViewModel(app) {
    private val jobs = JobStore(app)
    private val registry = DownloadRegistry(app)
    private val saver = ThumbnailSaver(app, registry)
    private val browser = BrowserAgent()
    private var runJob: Job? = null

    var uiState by mutableStateOf(
        jobs.lastConfig()?.let {
            HarvestState(
                url = it.url,
                folderName = it.folderName,
                treeUri = it.treeUri,
                mode = it.mode,
                maxPages = it.maxPages.toString(),
                imageSelector = it.imageSelector,
                nextSelector = it.nextSelector,
                pageParameter = it.pageParameter,
                quality = it.quality,
                status = "Previous collection found. Press Start to resume safely.",
                page = jobs.lastPage(),
                hasSavedJob = true
            )
        } ?: HarvestState()
    )
        private set

    fun update(transform: (HarvestState) -> HarvestState) {
        if (!uiState.isRunning) uiState = transform(uiState)
    }

    fun attach(webView: WebView) = browser.attach(webView)
    fun detach(webView: WebView) = browser.detach(webView)

    fun setFolder(tree: Uri) {
        if (uiState.isRunning) return
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                tree,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            uiState = uiState.copy(treeUri = tree.toString(), status = "Output folder selected.")
        } catch (error: Exception) {
            uiState = uiState.copy(status = "Folder permission failed: ${error.message}")
        }
    }

    fun navigate() {
        val url = uiState.url.trim()
        if (!isSecureUrl(url)) {
            uiState = uiState.copy(status = "Enter a valid HTTPS website URL.")
            return
        }
        runCatching { browser.open(url) }.onFailure {
            uiState = uiState.copy(status = "Browser unavailable: ${it.message}")
        }
    }

    fun preview() {
        viewModelScope.launch {
            try {
                val shot = browser.snapshot(uiState.imageSelector)
                uiState = uiState.copy(
                    previewCount = shot.images.size,
                    status = "Found ${shot.images.size} visible-sized images on this page. " +
                        "Next: ${shot.hasNext}, Load more: ${shot.hasMore}."
                )
            } catch (error: Exception) {
                uiState = uiState.copy(status = "Preview failed: ${error.message}")
            }
        }
    }

    fun start() {
        if (runJob?.isActive == true) return
        val state = uiState
        val pages = state.maxPages.toIntOrNull()
        val url = state.url.trim()
        when {
            !isSecureUrl(url) -> {
                uiState = uiState.copy(status = "An HTTPS URL is required.")
                return
            }
            state.treeUri.isBlank() -> {
                uiState = uiState.copy(status = "Choose an output folder first.")
                return
            }
            pages == null || pages !in 1..10000 -> {
                uiState = uiState.copy(status = "Maximum pages must be between 1 and 10,000.")
                return
            }
            state.folderName.isBlank() -> {
                uiState = uiState.copy(status = "Enter a collection folder name.")
                return
            }
        }
        val config = HarvestConfig(
            url = url,
            treeUri = state.treeUri,
            folderName = state.folderName,
            mode = state.mode,
            maxPages = pages!!,
            imageSelector = state.imageSelector.ifBlank { "img" },
            nextSelector = state.nextSelector,
            pageParameter = state.pageParameter.ifBlank { "page" },
            quality = state.quality
        )
        jobs.persist(config)
        uiState = state.copy(
            isRunning = true, failed = 0, skipped = 0,
            status = "Preparing collection…", hasSavedJob = true
        )

        runJob = viewModelScope.launch {
            try {
                saver.prepare(config)
                val existing = saver.count(config)
                uiState = uiState.copy(saved = existing)
                var previousSignature: String? = null
                var endReason = "Page limit reached"
                for (page in 1..config.maxPages) {
                    if (!isActive) break
                    if (page == 1 || config.mode == PaginationMode.URL_PAGE) {
                        browser.openPage(config, page)
                    }
                    var shot = browser.snapshot(config.imageSelector)
                    // AJAX transitions and lazy loading may settle after document.readyState.
                    if (shot.signature == previousSignature) {
                        repeat(3) {
                            if (shot.signature == previousSignature) {
                                delay(1000)
                                shot = browser.snapshot(config.imageSelector)
                            }
                        }
                    }
                    if (shot.signature == previousSignature && page > 1) {
                        endReason = "No new page results detected"
                        break
                    }
                    previousSignature = shot.signature
                    uiState = uiState.copy(
                        page = page,
                        previewCount = shot.images.size,
                        status = "Scanning page $page: ${shot.images.size} thumbnails"
                    )
                    jobs.savePage(page)
                    for (image in shot.images) {
                        if (!isActive) break
                        try {
                            if (saver.alreadySaved(config, image)) {
                                uiState = uiState.copy(skipped = uiState.skipped + 1)
                                continue
                            }
                            val outcome = saver.save(
                                config, image, shot.pageUrl,
                                browser.userAgent,
                                browser.cookiesFor(image.url)
                            )
                            if (outcome == SaveOutcome.Saved) {
                                uiState = uiState.copy(saved = uiState.saved + 1)
                            } else {
                                uiState = uiState.copy(skipped = uiState.skipped + 1)
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            uiState = uiState.copy(
                                failed = uiState.failed + 1,
                                status = "Skipped failed image: ${error.message}"
                            )
                        }
                        delay(250) // respectful, sequential traffic
                    }
                    if (page == config.maxPages) break
                    if (config.mode != PaginationMode.URL_PAGE) {
                        if (!browser.advance(config)) {
                            endReason = "No more pagination controls found"
                            break
                        }
                        delay(1100)
                        browser.waitForPage()
                    }
                }
                uiState = uiState.copy(status = "Writing manifest…")
                saver.writeManifest(config)
                jobs.clearPage()
                uiState = uiState.copy(
                    isRunning = false, hasSavedJob = false,
                    status = "Finished: $endReason. ${uiState.saved} JPEGs saved."
                )
            } catch (error: CancellationException) {
                withContext(NonCancellable) {
                    runCatching { saver.writeManifest(config) }
                }
                uiState = uiState.copy(
                    isRunning = false, status = "Paused. Reopen and Start to resume (duplicates skipped)."
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isRunning = false,
                    status = "Stopped: ${error.message}. Start again to retry safely."
                )
            }
        }
    }

    fun pause() {
        runJob?.cancel()
    }

    private fun isSecureUrl(input: String): Boolean = runCatching {
        val parsed = URI(input)
        parsed.scheme.equals("https", true) && !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null
    }.getOrDefault(false)

    override fun onCleared() {
        runJob?.cancel()
        registry.close()
        super.onCleared()
    }
}
