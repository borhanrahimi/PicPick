# Developing PicPick

## Architecture

```text
app/src/main/
├── java/edu/utsa/cs3443/picpick/
│   ├── BaseActivity.java        # Theme, insets, navigation, import/export, removal dialogs
│   ├── HomeActivity.java        # Review, gestures, progress, undo, welcome guide
│   ├── PhotoGridActivity.java   # Gallery/category grids, search, filters, long-press removal
│   ├── FolderActivity.java      # Collection summary and categories
│   ├── SettingsActivity.java    # Appearance, storage, controls, guide, credits
│   ├── FullscreenActivity.java  # Photo inspection and copy removal
│   ├── ZoomImageView.java       # Bounded pinch zoom, pan, and double-tap handling
│   ├── PhotoImportModel.java    # Imports retained through activity recreation
│   ├── PhotoImages.java         # Background decoding, EXIF handling, memory cache
│   └── model/
│       ├── Photo.java
│       ├── PhotoManager.java    # Library, local persistence, classifications, undo, removal
│       ├── PhotoFilters.java    # Name/status/source filtering
│       ├── ReviewGestures.java  # Horizontal-swipe classification
│       └── Csv.java             # CSV escaping and formula protection
├── assets/                     # Nine bundled samples
└── res/                        # Layouts, themes, icons, text, FileProvider rules
```

The app targets SDK 35 and supports API 26+. Android Gradle Plugin runs on JDK 17; source compatibility is Java 11.

## Data flow

The picker supplies selected URIs to `PhotoImportModel`. A background worker validates each image and copies it to the internal `imports/` directory, using a SHA-256 content hash for deduplication. The main thread updates the library and preferences once copying finishes. Interrupted copies that reached their final filename can be recovered on the next process start.

`PhotoManager` is the shared source of truth for visible photos, classifications, and the last 100 undo changes. UI screens refresh when resumed. Reset, sample-visibility changes, and skipped-photo requeueing clear undo history so earlier actions cannot conflict with the new queue.

`PhotoImages` decodes images off the UI thread, applies EXIF orientation, and caches sampled bitmaps. The review card has stable dimensions so loading an image cannot push the action row away.

CSV reports use a FileProvider restricted to `cache/exports/`. Imported photos are not exposed by this provider.

## Build and host tests

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Host tests cover CSV escaping and formula protection, combined gallery filters, and gesture thresholds that distinguish a sorting swipe from a tap or vertical scroll.

## Device tests

Use a disposable emulator or test installation: tests reset sample classifications and can temporarily import or remove test copies.

```sh
./gradlew connectedDebugAndroidTest
```

Device coverage includes saved classifications, undo after reload/recreation, CSV synchronization, sample visibility, import deduplication, reset confirmation, swipe sorting, search/filter restoration, viewer zoom/reset, contributor links, imported-copy removal, and screenshots of the actual screens.

Disable window, transition, and animator duration scales on the test emulator for reliable UI interactions. Also manually check older Android versions, landscape, large fonts, real cloud-backed photo providers, and sharing apps before release.

## Refresh README screenshots

Connect one disposable test device or start an emulator, then run:

```sh
./scripts/capture-screenshots.sh
```

Set `ADB` to the full path to your `adb` executable if it is not discoverable. The script runs `ScreenshotTest` and copies real PNG captures into `docs/screenshots/`.

The screenshot test uses the bundled sample library, sets up representative sorting choices, captures Review/Gallery/Folders/Settings/Credits/dark Review, and restores sample choices and system appearance afterward. Images are device captures, not design mockups.

## Manual acceptance checks

- First launch shows the welcome guide; Settings can reopen it.
- Import cancellation leaves the library unchanged; duplicates and unsupported images are reported.
- A horizontal swipe sorts once; vertical scrolling does not sort.
- Pinch/double-tap zoom and reset work after an image loads.
- Search, status, and source filters combine and survive recreation.
- Removing an imported copy requires confirmation, updates storage/counts, and leaves the original intact.
- Reset, sample toggles, skipped review, and undo keep progress and reports consistent.
- All four contributor names open the supplied LinkedIn profiles.
