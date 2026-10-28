#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -n "${ADB:-}" ]]; then
    adb_bin="$ADB"
elif command -v adb >/dev/null 2>&1; then
    adb_bin="$(command -v adb)"
else
    sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
    adb_bin="$sdk_dir/platform-tools/adb"
fi
if [[ ! -x "$adb_bin" ]]; then
    echo "ADB not found. Set ADB to the full path of your adb executable." >&2
    exit 1
fi

echo "Use a disposable test device: this captures bundled samples and resets sorting choices."
./gradlew connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=edu.utsa.cs3443.picpick.ScreenshotTest
mkdir -p docs/screenshots
"$adb_bin" pull /data/local/tmp/picpick-screenshots/. docs/screenshots/
echo "Real device captures saved to docs/screenshots/."
