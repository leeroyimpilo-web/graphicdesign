package za.co.graphicdesign.pageharvest

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * The automation runs inside the user's visible WebView. It never bypasses login,
 * paywalls, challenges or access controls, and it does not open source asset links.
 */
class BrowserAgent {
    private var browser: WebView? = null

    fun attach(webView: WebView) {
        browser = webView
    }

    fun detach(webView: WebView) {
        if (browser === webView) browser = null
    }

    val userAgent: String
        get() = browser?.settings?.userAgentString ?: "PageHarvest Android"

    fun cookiesFor(url: String): String? = CookieManager.getInstance().getCookie(url)

    fun open(url: String) {
        browser?.loadUrl(url) ?: error("Browser has not been initialized.")
    }

    suspend fun openPage(config: HarvestConfig, page: Int) {
        withContext(Dispatchers.Main) {
            if (config.mode == PaginationMode.URL_PAGE) {
                val original = Uri.parse(config.url)
                val builder = original.buildUpon().clearQuery()
                for (key in original.queryParameterNames) {
                    if (key != config.pageParameter) {
                        original.getQueryParameters(key).forEach { value ->
                            builder.appendQueryParameter(key, value)
                        }
                    }
                }
                builder.appendQueryParameter(config.pageParameter, page.toString())
                open(builder.build().toString())
            } else if (page == 1) {
                open(config.url)
            }
        }
        waitForPage()
    }

    suspend fun waitForPage() {
        delay(500)
        repeat(30) {
            val ready = runCatching { evaluate("document.readyState") }.getOrNull() ?: ""
            if (ready.contains("complete") || ready.contains("interactive")) {
                delay(700)
                return
            }
            delay(250)
        }
        delay(700)
    }

    suspend fun snapshot(selector: String): PageSnapshot {
        val quotedSelector = JSONObject.quote(selector.ifBlank { "img" })
        val script = """
            (function() {
                try {
                    const selector = $quotedSelector;
                    const nodes = Array.from(document.querySelectorAll(selector)).slice(0, 8000);
                    const urls = new Set();
                    const images = [];
                    for (const node of nodes) {
                        const img = node.tagName === "IMG" ? node : node.querySelector("img");
                        if (!img) continue;
                        const rect = img.getBoundingClientRect();
                        if (rect.width < 90 || rect.height < 60) continue;
                        const style = window.getComputedStyle(img);
                        if (style.display === "none" || style.visibility === "hidden") continue;
                        let src = img.currentSrc || img.getAttribute("src") || "";
                        if (!src || /^(data:|blob:)/i.test(src)) {
                            src = img.getAttribute("data-src") ||
                                img.getAttribute("data-lazy-src") ||
                                img.getAttribute("data-original") || "";
                        }
                        if (!src) continue;
                        let absolute;
                        try { absolute = new URL(src, location.href).href; }
                        catch (_) { continue; }
                        if (!absolute.startsWith("https://") || urls.has(absolute)) continue;
                        urls.add(absolute);
                        images.push({url: absolute, alt: (img.alt || "").slice(0, 120)});
                        if (images.length >= 5000) break;
                    }
                    const candidates = Array.from(
                        document.querySelectorAll('a, button, [role="button"]')
                    );
                    const label = el => (
                        (el.getAttribute("aria-label") || "") + " " +
                        (el.textContent || "") + " " +
                        (el.getAttribute("title") || "")
                    ).trim().replace(/\s+/g, " ").toLowerCase();
                    const active = el => !el.disabled &&
                        el.getAttribute("aria-disabled") !== "true" &&
                        getComputedStyle(el).visibility !== "hidden";
                    const next = candidates.some(el => active(el) &&
                        (el.rel === "next" ||
                        /^(next|next page|older|›|»|→|>)$/i.test(label(el))));
                    const more = candidates.some(el => active(el) &&
                        /(load more|show more|view more|more results)/i.test(label(el)));
                    return JSON.stringify({
                        url: location.href, images: images,
                        next: next, more: more,
                        height: Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)
                    });
                } catch (e) {
                    return JSON.stringify({error: String(e)});
                }
            })()
        """.trimIndent()

        val json = JSONObject(decode(evaluate(script)))
        if (json.has("error")) throw IllegalArgumentException(json.getString("error"))
        val arr = json.getJSONArray("images")
        val thumbs = (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            Thumbnail(item.getString("url"), item.optString("alt"))
        }
        return PageSnapshot(
            json.getString("url"), thumbs, json.optBoolean("next"),
            json.optBoolean("more"), json.optInt("height")
        )
    }

    suspend fun advance(config: HarvestConfig): Boolean {
        if (config.mode == PaginationMode.URL_PAGE) return true
        val selector = JSONObject.quote(config.nextSelector)
        val kind = JSONObject.quote(config.mode.name)
        val script = """
            (function() {
                const mode = $kind;
                const custom = $selector;
                const candidates = Array.from(
                    document.querySelectorAll('a, button, [role="button"]')
                );
                const label = el => (
                    (el.getAttribute("aria-label") || "") + " " +
                    (el.textContent || "") + " " +
                    (el.getAttribute("title") || "")
                ).trim().replace(/\s+/g, " ").toLowerCase();
                const active = el => !el.disabled &&
                    el.getAttribute("aria-disabled") !== "true";
                const next = candidates.find(el => active(el) &&
                    (el.rel === "next" ||
                    /^(next|next page|older|›|»|→|>)$/i.test(label(el))));
                const more = candidates.find(el => active(el) &&
                    /(load more|show more|view more|more results)/i.test(label(el)));
                let target = null;
                if (mode === "CSS_NEXT" && custom) {
                    try { target = document.querySelector(custom); }
                    catch (_) { return "invalid-css"; }
                } else if (mode === "NEXT") target = next;
                else if (mode === "LOAD_MORE") target = more;
                else if (mode === "AUTO") target = more || next;
                if (target) {
                    target.click();
                    return "clicked";
                }
                if (mode === "AUTO" || mode === "SCROLL") {
                    window.scrollTo(0, document.documentElement.scrollHeight);
                    return "scrolled";
                }
                return "none";
            })()
        """.trimIndent()
        return decode(evaluate(script)) in setOf("clicked", "scrolled")
    }

    private fun decode(value: String): String =
        JSONArray("[$value]").getString(0)

    private suspend fun evaluate(js: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val view = browser
            if (view == null) {
                continuation.cancel(IllegalStateException("Browser is closed."))
            } else {
                view.evaluateJavascript(js) { result ->
                    if (continuation.isActive) continuation.resume(result ?: "null")
                }
            }
        }
    }
}
