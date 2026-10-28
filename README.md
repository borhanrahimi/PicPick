# PicPick

**Less clutter. More moments worth keeping.**

PicPick is a native Android photo organizer built with Java and Material 3. Import photos, review them one at a time, and choose **Keep**, **Trash**, or **Skip**. Your originals stay untouched.

## What's inside

- **Your photos:** select up to 30 images per import through Android's photo picker (or its document-picker fallback).
- **A calmer review flow:** rounded photo cards, progress tracking, completion states, and explicit review of skipped photos.
- **Multi-step undo:** restore up to 100 recent decisions, including after rotation or reopening the app.
- **Gallery and folders:** responsive photo grids, category counts, empty states, and fullscreen previews.
- **Settings:** light, dark, or device appearance; sample-photo visibility; import; CSV sharing; and confirmed reset.
- **Private storage:** selected images are copied into app-private storage. No account, server, broad library permission, or network permission is required.
- **Shareable reports:** export filenames and sorting choices through the Android share sheet.

Trash is a classification, not file deletion. PicPick does not remove originals from your phone.

## Try the app

### On an Android device

Build the debug APK and install it on a connected Android phone or emulator:

```sh
./gradlew assembleDebug
./gradlew installDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### In your browser

A public demo link has not been configured in this repository yet.

1. Build the APK with `./gradlew assembleDebug`.
2. Upload it to your [Appetize](https://appetize.io/) account.
3. Start a session to try the app in a browser.
4. Copy the share URL and add it near the top of this README.

See Appetize's [Android upload requirements](https://docs.appetize.io/platform/app-management/uploading-apps/android) and [sharing documentation](https://docs.appetize.io/platform/sharing-apps). Hosting account limits apply. The hosted device's photo picker sees that virtual device's media, rather than your computer's photo library; use the bundled samples or add media to the virtual device.

### Suggested walkthrough

1. Open Settings and leave **Include sample photos** enabled.
2. Reset choices, then open Review.
3. Keep a photo, mark another as Trash, and undo both decisions.
4. Skip a photo. Finish the first pass, then tap **Review skipped photos**.
5. Browse Gallery and Folders, then open a sorted photo fullscreen.
6. Switch between light and dark appearance in Settings.
7. Tap **Add photos** to choose personal images. Try importing the same image twice to see duplicate detection.
8. Choose **Share sorting report** to export a CSV.
9. Close and reopen the app to check the saved library and undo history.

Add actual screenshots of Review, Folders, and Settings here after capturing them on a device. No simulated screenshots or placeholder demo links are included.

## Development setup

- JDK 17 for Android Gradle Plugin 8.8.1; app Java source compatibility is 11.
- Android SDK Platform 35 and appropriate build tools.
- Android 8.0 / API 26 or newer to run the app.

Android Studio is optional when the SDK and JDK are already installed. Open this folder in VS Code and run Gradle commands in its terminal. Android Studio provides SDK/device setup and visual layout previews if you prefer it.

Configure your SDK in an untracked `local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

On Windows, use `gradlew.bat` instead of `./gradlew`. See the [Android command-line build guide](https://developer.android.com/build/building-cmdline).

## Design and structure

A shared light/dark palette, reusable typography styles, Material cards, and four labeled navigation destinations keep the screens consistent. System-bar insets are handled centrally, and content can scroll on smaller displays.

```text
app/src/main/
├── java/edu/utsa/cs3443/picpick/
│   ├── BaseActivity.java        # Theme, navigation, insets, picker, export
│   ├── HomeActivity.java        # Review, progress, and undo
│   ├── PhotoGridActivity.java   # Shared Gallery/Keep/Trash/Skip behavior
│   ├── FolderActivity.java      # Category cards and collection summary
│   ├── SettingsActivity.java    # Appearance and library controls
│   ├── FullscreenActivity.java
│   ├── PhotoImportModel.java    # Background imports retained during rotation
│   ├── PhotoImages.java         # Sampled image loading, cache, EXIF rotation
│   ├── ImageAdapter.java
│   └── model/
│       ├── Photo.java
│       ├── PhotoManager.java    # Library, classifications, undo, persistence
│       └── Csv.java             # CSV escaping and formula protection
├── assets/                     # Nine bundled sample images
└── res/                        # Layouts, tokens, icons, styles, sharing rules
```

## Storage and behavior

- Imports are app-private copies, up to **25 MB** and **100 megapixels** per image. Unsupported or oversized images are reported in the import result.
- Identical file contents are deduplicated using SHA-256. The original image is never modified or deleted.
- The library and last 100 sorting changes are saved locally. Resetting choices, changing sample visibility, or requeuing skipped photos clears undo history.
- Hiding samples preserves their classifications. Reset affects visible photos; hidden sample decisions are preserved.
- CSV reports contain visible photo labels and classifications. Fields are quoted, quotes are escaped, and leading spreadsheet formulas are neutralized.
- Reports are shared through a FileProvider restricted to the report-cache directory. Photos are not exposed by that provider.
- Android backup and device transfer of app data are disabled. Clearing app storage or uninstalling removes imported copies and saved decisions; keep your original photos elsewhere.

The picker uses the [Android Photo Picker API](https://developer.android.com/training/data-storage/shared/photo-picker), and report sharing follows [FileProvider guidance](https://developer.android.com/reference/androidx/core/content/FileProvider).

## Verification

```sh
# Build, lint, and host-side CSV tests
./gradlew assembleDebug lintDebug testDebugUnitTest

# Compile device tests without a device
./gradlew assembleDebugAndroidTest

# Execute device tests on a connected emulator/device
./gradlew connectedDebugAndroidTest
```

Device tests cover classification persistence, multi-step undo after reload, sample visibility, reset, CSV synchronization, import copying/deduplication, and review/reset UI behavior. They reset sample classifications and should run on a disposable test installation.

Building and compiling tests does not replace visual testing. Check import/share dialogs, small-screen layouts, dark mode, and large text on an Android device before releasing.

## Next improvements

- Swipe gestures as an alternative to the labeled review buttons.
- Search and filter larger libraries.
- Selective removal of imported app copies, with clear confirmation.
- Accessibility and visual testing across screen sizes.
- CI builds, screenshots, and a downloadable APK release.

## Contributing and license

Describe the problem or feature in an issue. For changes, include what changed and how it was verified.

No license file is included yet. Add an explicit license before presenting this code as reusable open-source software.
