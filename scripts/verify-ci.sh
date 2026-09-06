#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest
