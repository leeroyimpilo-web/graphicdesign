package za.co.graphicdesign.pageharvest

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private val model: HarvestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF59D5E0),
                    onPrimary = Color(0xFF07141B),
                    secondary = Color(0xFFA0EAF0),
                    surface = Color(0xFF171E2D),
                    background = Color(0xFF0D1420)
                )
            ) {
                HarvestScreen(model)
            }
        }
    }
}

@Composable
private fun HarvestScreen(model: HarvestViewModel) {
    val state = model.uiState
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) model.setFolder(uri)
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "PAGEHARVEST",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                "Native Android • Smart pagination & JPEG thumbnail collecting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = { value -> model.update { it.copy(url = value) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
                label = { Text("HTTPS website / search URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = model::navigate, enabled = !state.isRunning) {
                    Text("Open website")
                }
                OutlinedButton(onClick = model::preview, enabled = !state.isRunning) {
                    Text("Detect images")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("LIVE SITE PREVIEW", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    BrowserPreview(model)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Use the embedded browser to load or sign in to websites you may access. " +
                            "Visible image results detected: ${state.previewCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text("Pagination method", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaginationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == state.mode,
                        enabled = !state.isRunning,
                        onClick = { model.update { it.copy(mode = mode) } },
                        label = { Text(mode.label) }
                    )
                }
            }

            if (state.mode == PaginationMode.CSS_NEXT) {
                OutlinedTextField(
                    value = state.nextSelector,
                    onValueChange = { v -> model.update { it.copy(nextSelector = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRunning,
                    label = { Text("Next button CSS selector (e.g. .pagination-next)") },
                    singleLine = true
                )
            }
            if (state.mode == PaginationMode.URL_PAGE) {
                OutlinedTextField(
                    value = state.pageParameter,
                    onValueChange = { v -> model.update { it.copy(pageParameter = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRunning,
                    label = { Text("Page parameter name") },
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = state.imageSelector,
                onValueChange = { v -> model.update { it.copy(imageSelector = v) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
                label = { Text("Thumbnail CSS selector") },
                supportingText = {
                    Text("Default: img — use a result-grid selector to exclude banners and icons.")
                },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.maxPages,
                    onValueChange = { v -> model.update { it.copy(maxPages = v) } },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f),
                    label = { Text("Max pages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.folderName,
                    onValueChange = { v -> model.update { it.copy(folderName = v) } },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1.5f),
                    label = { Text("Collection folder") },
                    singleLine = true
                )
            }
            Text("JPEG quality: ${state.quality}%", fontWeight = FontWeight.Medium)
            Slider(
                value = state.quality.toFloat(),
                onValueChange = { v -> model.update { it.copy(quality = v.toInt()) } },
                valueRange = 50f..100f,
                enabled = !state.isRunning
            )
            FilledTonalButton(
                onClick = { folderPicker.launch(null) },
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.treeUri.isBlank()) "Choose output folder" else "Change output folder ✓")
            }
            if (state.treeUri.isNotBlank()) {
                Text(
                    "Storage access granted. JPEGs and manifest.csv go in /${state.folderName}/",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = model::start,
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.hasSavedJob) "Start / resume" else "Start collection")
                }
                OutlinedButton(
                    onClick = model::pause,
                    enabled = state.isRunning
                ) {
                    Text("Pause")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("COLLECTION MONITOR", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        MonitorValue("Page", state.page)
                        MonitorValue("JPEGs", state.saved)
                        MonitorValue("Skipped", state.skipped)
                        MonitorValue("Failed", state.failed)
                    }
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                "Collect only material you are authorized to save. Downloading a thumbnail " +
                    "does not grant the right to redistribute it or use it for AI training. " +
                    "Keep this screen open during collection. No CAPTCHA or paywall bypass.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MonitorValue(label: String, count: Int) {
    Column {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BrowserPreview(model: HarvestViewModel) {
    val context = LocalContext.current
    var viewRef by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            viewRef?.let { view ->
                model.detach(view)
                view.stopLoading()
                view.destroy()
            }
            viewRef = null
        }
    }
    Box(
        Modifier.fillMaxWidth().height(300.dp)
            .background(Color.White)
    ) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setSupportMultipleWindows(false)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.mediaPlaybackRequiresUserGesture = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = request.url.scheme != "https"
                    }
                    model.attach(this)
                    viewRef = this
                    loadUrl("about:blank")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
