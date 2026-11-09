package za.co.graphicdesign.pageharvest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface SaveOutcome {
    data object Saved : SaveOutcome
    data object Duplicate : SaveOutcome
}

class ThumbnailSaver(
    private val context: Context,
    private val registry: DownloadRegistry
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(40, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun folder(config: HarvestConfig): DocumentFile {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(config.treeUri))
            ?: throw IOException("The selected storage folder is unavailable.")
        val safe = config.folderName.trim().replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .take(70).ifBlank { "collection" }
        val existing = root.findFile(safe)
        return when {
            existing != null && existing.isDirectory -> existing
            existing != null -> throw IOException("A file already uses that folder name.")
            else -> root.createDirectory(safe) ?: throw IOException("Could not create folder.")
        }
    }

    suspend fun prepare(config: HarvestConfig) = withContext(Dispatchers.IO) {
        folder(config)
    }

    suspend fun count(config: HarvestConfig): Int = withContext(Dispatchers.IO) {
        registry.count(config.collectionKey)
    }

    suspend fun alreadySaved(config: HarvestConfig, image: Thumbnail): Boolean =
        withContext(Dispatchers.IO) {
            registry.hasUrl(config.collectionKey, image.url)
        }

    suspend fun save(
        config: HarvestConfig,
        image: Thumbnail,
        pageUrl: String,
        userAgent: String,
        cookie: String?
    ): SaveOutcome = withContext(Dispatchers.IO) {
        if (registry.hasUrl(config.collectionKey, image.url)) {
            return@withContext SaveOutcome.Duplicate
        }
        if (!image.url.startsWith("https://", ignoreCase = true)) {
            throw IOException("Only HTTPS images can be downloaded.")
        }
        val request = Request.Builder().url(image.url)
            .header("User-Agent", userAgent)
            .header("Referer", pageUrl)
            .apply { if (!cookie.isNullOrBlank()) header("Cookie", cookie) }
            .build()

        val bytes = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            if (response.body == null) throw IOException("Empty image response.")
            if (response.body!!.contentLength() > MAX_SOURCE_BYTES) {
                throw IOException("Image exceeds the size limit.")
            }
            response.body!!.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    total += size
                    if (total > MAX_SOURCE_BYTES) throw IOException("Image too large.")
                    output.write(buffer, 0, size)
                }
                output.toByteArray()
            }
        }

        val jpeg = makeJpeg(bytes, config.quality)
        val hash = MessageDigest.getInstance("SHA-256").digest(jpeg)
            .joinToString("") { "%02x".format(it) }
        if (registry.hasHash(config.collectionKey, hash)) {
            return@withContext SaveOutcome.Duplicate
        }
        val filename = "thumb_${hash.take(20)}.jpg"
        val targetFolder = folder(config)
        // A deterministic hash name also minimizes duplicate orphan files after a crash.
        val existing = targetFolder.findFile(filename)
        val file = existing ?: targetFolder.createFile("image/jpeg", filename)
            ?: throw IOException("Could not create $filename")
        try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                it.write(jpeg)
            } ?: throw IOException("Could not write JPEG file.")
            registry.record(
                config.collectionKey,
                SavedRow(image.url, pageUrl, filename, hash, System.currentTimeMillis())
            )
        } catch (error: Exception) {
            if (existing == null) file.delete()
            throw error
        }
        SaveOutcome.Saved
    }

    suspend fun writeManifest(config: HarvestConfig) = withContext(Dispatchers.IO) {
        val collectionFolder = folder(config)
        val file = collectionFolder.findFile("manifest.csv")
            ?: collectionFolder.createFile("text/csv", "manifest.csv")
            ?: throw IOException("Could not create the manifest.")
        val rows = registry.list(config.collectionKey)
        context.contentResolver.openOutputStream(file.uri, "wt")?.bufferedWriter()?.use { out ->
            out.appendLine("filename,source_url,page_url,sha256,saved_at_epoch_ms")
            rows.forEach { row ->
                fun cell(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""
                out.appendLine(
                    listOf(row.name, row.sourceUrl, row.pageUrl, row.sha256, row.time.toString())
                        .joinToString(",") { cell(it) }
                )
            }
        } ?: throw IOException("Failed to write CSV.")
    }

    private fun makeJpeg(bytes: ByteArray, quality: Int): ByteArray {
        val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, probe)
        if (probe.outWidth < 1 || probe.outHeight < 1) {
            throw IOException("Unsupported image format (SVG may require screenshot mode).")
        }
        var sample = 1
        while (maxOf(probe.outWidth / sample, probe.outHeight / sample) > MAX_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("Could not decode thumbnail.")
        try {
            val factor = minOf(
                1f,
                MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height)
            )
            val width = (decoded.width * factor).toInt().coerceAtLeast(1)
            val height = (decoded.height * factor).toInt().coerceAtLeast(1)
            val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(opaque)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(
                    decoded, null, Rect(0, 0, width, height),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                return ByteArrayOutputStream().use { out ->
                    if (!opaque.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                        throw IOException("JPEG encoding failed.")
                    }
                    out.toByteArray()
                }
            } finally {
                opaque.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    companion object {
        private const val MAX_SOURCE_BYTES = 12 * 1024 * 1024L
        private const val MAX_DIMENSION = 1600
    }
}
