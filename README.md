# Timmy Quest Editor

Standalone Android/Quest 3 save editor prototype for H3VR2 save files. This is an external app, not an in-game mod.

## Build APK

Push to `main` or run Actions → Build Quest APK. The workflow uploads `app-debug.apk` as an artifact.

## Current functionality
- Android Storage Access Framework file picker (select a file, or select a folder where supported)
- gzip decompression/compression
- Boolean JSON values edited using a toggle control
- JSON validation and editing
- Export edited file and separate backup

Android scoped storage restrictions may prevent direct access to another app's `Android/data` folder. The app currently relies on file picker permissions and does not bypass OS security restrictions.

Save-path hint from user: `/storage/emulated/0/Android/data/com.h3vr2/Savedata/` (verify actual package/layout on device).


## UI refresh (2026-11-25)
- Dark slate theme with cyan accent and modern card styling
- Improved header, file status display, editor workspace and action buttons
- Responsive scroll layout for headset/mobile screens
- UI refresh only; save-format-specific controls are still pending sample data


## Current build status and limitations (2026-11-25)

- Field panels recursively enumerate JSON object fields and array elements and offer per-panel text filtering. Quick Edit is a name-based heuristic, not a confirmed H3VR2 field list.
- The raw editor supports JSON objects and arrays; gzip input is detected and re-compressed for export/save.
- Direct overwrite is guarded by backup export and confirmation. This is not an atomic-save guarantee: some Android storage providers may truncate or fail during replacement. Prefer exporting an edited copy and retaining the original.
- Save candidates are heuristic. Review the filename and contents before editing. Folder scan depth is limited and Android storage access may be denied for protected app folders.
- Inventory, store, presets/favorites, and field-guide schema are not verified or implemented. Generic field editing is not proof that the game save format is supported.
- Boolean values now use a toggle control instead of free-text entry.
- The latest commits must be confirmed by CI and tested on a physical Quest before release claims.

## Basic use

1. Open a file or connect a folder Android permits access to.
2. Review the file and export an untouched backup before direct overwrite.
3. Validate JSON, edit using field panels or Raw JSON, and export a separate edited copy first.
4. Keep the original backup. Test edits on copies whenever possible.
