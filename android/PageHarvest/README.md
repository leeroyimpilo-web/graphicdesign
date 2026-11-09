# PageHarvest — native Android thumbnail collector

A Kotlin / Jetpack Compose Android application inside the **private** `graphicdesign` repository.

## What is implemented (v0.1)

- Paste an **HTTPS** search/gallery URL and load it in an embedded Android WebView.
- Sign in normally if the site allows access. No login or CAPTCHA bypass is included.
- Scan images by CSS selector (defaults to `img`) with size/visibility filtering.
- Pagination: **Auto**, **Next**, **Load More**, **Infinite Scroll**, **URL page parameter**, and **CSS Next**.
- Save a chosen collection into a user-selected Android folder via the Storage Access Framework (no storage-wide permissions).
- Convert supported bitmap thumbnails to JPEG, maximum 1600px on the longest side.
- Deduplicate by source URL and JPEG SHA-256, with a local SQLite registry.
- Generate `manifest.csv` with source URLs and download provenance.
- Save the active collection configuration to local preferences. Start again after an interruption to replay pages and skip downloaded images.
- Respectful sequential fetching and explicit maximum page count.

## IMPORTANT limitations

- Android's WebView is not a complete desktop browser automation framework. Some AJAX, hash-only, cursor/API, canvas, virtualized, and heavily scripted galleries require a custom adapter.
- **Auto** tries Load More, then Next, then scrolling. When a site uses opaque or unlabelled controls, use **CSS Next**.
- The detector downloads the displayed `<img>` resource only; it does not follow the thumbnail link to fetch original files. A website can still serve its original resolution to an `<img>`. JPEG conversion scales any large downloaded bitmap down to 1600px.
- Lazy-loaded images may require manually scrolling the preview or configuring a more specific selector.
- This initial version **requires the app to remain open and the screen active** during a run. Android can pause a WebView or terminate its process in the background. Resume is replay + deduplication, not guaranteed return to the exact DOM cursor position.
- Authenticated downloads may fail when image URLs require custom JavaScript headers, short-lived signatures, or anti-hotlink checks. Errors are counted instead of bypassed.
- Only HTTPS sources and images are accepted. SVG, GIF animation and CSS backgrounds are not supported by the bitmap decoder.
- No claim of complete or universal pagination. Check the preview and respect each site's terms/robots restrictions.

## Licensing and data boundary

Keep the existing `text-effects/` Magnific archive separate from `training-approved/`.
**Collecting or saving a thumbnail does not grant permission for AI training, embedding, retrieval, reproduction, or redistribution.**
Review the source license and the repository's [rights instructions](../../docs/RIGHTS_AND_SOURCES.md) before use.

## Build

Open **`android/PageHarvest/`** as a project in Android Studio using JDK 17.
If your setup doesn't have a Gradle wrapper, install Gradle 8.13 and run:

```bash
cd android/PageHarvest
gradle :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
Alternatively, GitHub Actions builds a debug APK on each push to the Android project; download its artifact from the **Actions** tab.

- Android Gradle Plugin 8.13.2
- Kotlin 2.3.10 / matching Compose compiler plugin
- Java 17, compile SDK 36, min SDK 26, target SDK 35
- Android WebView + SAF + SQLite + OkHttp

## Quick test

1. Install the debug APK.
2. Paste a permitted HTTPS result page and tap **Open website**.
3. Tap **Detect images**; if the count includes banners, enter a narrow CSS selector such as `.result-card img`.
4. Choose the pagination method (Auto is a starting point).
5. Enter a folder name, set the maximum page count, and choose a phone storage folder.
6. Tap **Start collection** and keep PageHarvest visible.
7. Open the chosen folder to inspect the JPEGs and `manifest.csv`.

**Version 0.1 is source code ready for CI compilation, not a tested APK.**
