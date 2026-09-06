#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
release_apk="$project_dir/app/build/outputs/apk/release/app-release.apk"
max_release_bytes=2883584
forbidden_apk_entry='(^|/)([^/]+\.(zip|keystore|jks|p12|pfx|key)|keystore\.properties|local\.properties|\.env(\.[^/]*)?)$'

cd "$project_dir"
./gradlew --no-daemon clean
./gradlew --no-daemon :app:assembleRelease
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug

if [[ ! -f "$release_apk" ]]; then
  echo "Release APK was not generated: $release_apk" >&2
  exit 1
fi

# Do not let a failed ZIP read look like an empty list of forbidden files.
unzip -tq "$release_apk" >/dev/null
apk_entries="$(unzip -Z1 "$release_apk")"
forbidden_entries="$(printf '%s\n' "$apk_entries" | grep -Ei "$forbidden_apk_entry" || true)"
if [[ -n "$forbidden_entries" ]]; then
  echo "Release APK contains forbidden archive or sensitive files:" >&2
  printf '%s\n' "$forbidden_entries" >&2
  exit 1
fi

if stat -f%z "$release_apk" >/dev/null 2>&1; then
  release_bytes="$(stat -f%z "$release_apk")"
else
  release_bytes="$(stat -c%s "$release_apk")"
fi

if (( release_bytes > max_release_bytes )); then
  echo "Release APK is too large: ${release_bytes} bytes (limit ${max_release_bytes})" >&2
  exit 1
fi

local_sdk_dir=""
if [[ -f local.properties ]]; then
  local_sdk_dir="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' local.properties | tail -n 1 | tr -d '\r')"
fi
android_sdk_root="${local_sdk_dir:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}}"
apksigner_path=""
if command -v apksigner >/dev/null 2>&1; then
  apksigner_path="$(command -v apksigner)"
elif [[ -d "$android_sdk_root/build-tools" ]]; then
  apksigner_path="$(find "$android_sdk_root/build-tools" -type f -name apksigner | sort | tail -n 1)"
fi

if [[ -n "$apksigner_path" ]]; then
  "$apksigner_path" verify --verbose "$release_apk"
else
  echo "apksigner not found; refusing to pass a Release without signature verification." >&2
  exit 1
fi

echo "Release gate passed: ${release_bytes} bytes"
