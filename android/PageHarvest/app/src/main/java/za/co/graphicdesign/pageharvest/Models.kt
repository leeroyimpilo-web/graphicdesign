package za.co.graphicdesign.pageharvest

import org.json.JSONObject

enum class PaginationMode(val label: String) {
    AUTO("Auto"), NEXT("Next button"), LOAD_MORE("Load more"),
    SCROLL("Infinite scroll"), URL_PAGE("URL parameter"), CSS_NEXT("CSS next");

    companion object {
        fun fromSaved(value: String): PaginationMode =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

data class Thumbnail(val url: String, val alt: String)

data class PageSnapshot(
    val pageUrl: String,
    val images: List<Thumbnail>,
    val hasNext: Boolean,
    val hasMore: Boolean,
    val height: Int
) {
    val signature: String
        get() = pageUrl + "|" + images.joinToString("|") { it.url }
}

data class HarvestConfig(
    val url: String,
    val treeUri: String,
    val folderName: String,
    val mode: PaginationMode,
    val maxPages: Int,
    val imageSelector: String,
    val nextSelector: String,
    val pageParameter: String,
    val quality: Int
) {
    val collectionKey: String get() = "$treeUri|$folderName"

    fun toJson() = JSONObject().apply {
        put("url", url)
        put("treeUri", treeUri)
        put("folderName", folderName)
        put("mode", mode.name)
        put("maxPages", maxPages)
        put("imageSelector", imageSelector)
        put("nextSelector", nextSelector)
        put("pageParameter", pageParameter)
        put("quality", quality)
    }.toString()

    companion object {
        fun fromJson(json: String): HarvestConfig {
            val o = JSONObject(json)
            return HarvestConfig(
                o.getString("url"), o.getString("treeUri"),
                o.getString("folderName"),
                PaginationMode.fromSaved(o.optString("mode", "AUTO")),
                o.optInt("maxPages", 100).coerceIn(1, 10000),
                o.optString("imageSelector", "img"),
                o.optString("nextSelector", ""),
                o.optString("pageParameter", "page"),
                o.optInt("quality", 85).coerceIn(50, 100)
            )
        }
    }
}

data class HarvestState(
    val url: String = "",
    val folderName: String = "text-effects",
    val treeUri: String = "",
    val mode: PaginationMode = PaginationMode.AUTO,
    val maxPages: String = "100",
    val imageSelector: String = "img",
    val nextSelector: String = "",
    val pageParameter: String = "page",
    val quality: Int = 85,
    val status: String = "Enter an HTTPS URL and choose a folder.",
    val page: Int = 0,
    val saved: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val previewCount: Int = 0,
    val isRunning: Boolean = false,
    val hasSavedJob: Boolean = false
)
