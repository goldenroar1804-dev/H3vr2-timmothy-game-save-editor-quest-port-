# Timmy Quest Editor

Standalone Android/Quest 3 save editor prototype for H3VR2 save files. This is an external app, not an in-game mod.

## Build APK

Push to `main` or run Actions → Build Quest APK. The workflow uploads `app-debug.apk` as an artifact.

## Current functionality
- Android Storage Access Framework file picker (select a file, or select a folder where supported)
- Preview file contents as UTF-8 or hex
- gzip decompression/compression
- JSON validation and editing
- Export edited file and separate backup

Android scoped storage restrictions may prevent direct access to another app's `Android/data` folder. The app currently relies on file picker permissions and does not bypass OS security restrictions.

Save-path hint from user: `/storage/emulated/0/Android/data/com.h3vr2/Savedata/` (verify actual package/layout on device).
