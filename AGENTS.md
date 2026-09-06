# Project working agreement

## Product and code map
- ZhihuPaper is a Chinese, account-free Android reader. Preserve local-first storage and the absence of ads/analytics.
- `app/` contains the product; `photoview/` contains the vendored AndroidX image viewer. Do not replace vendored code casually; preserve notices.
- `activity/`, `fragment/`, `adapter/` own UI; `http/` owns API validation; `db/` owns SQLite; `task/` owns background work; `util/` contains shared helpers.
- Follow the existing Java/XML design. Architecture migrations require explicit task scope.

## Changes and invariants
- Follow `.editorconfig`; avoid unrelated formatting. UI strings belong in resources.
- Keep network and database work off the UI thread. Register UI tasks with the Activity/view lifecycle and clear callbacks on teardown.
- Keep HTTPS validation and WebView file/content restrictions. Never introduce Java bridges for remote article content.
- Preserve saved articles, notes, highlights and reading state. Schema changes require a version bump, explicit migration and migration tests; never use destructive reset as a migration.
- Permission changes must update README, PRIVACY.md and the in-app privacy text together.
- Do not add credentials, machine paths, APK/AAB or generated output to Git. `gradle-wrapper.jar` is required source tooling and must remain.

## Environment and validation
- JDK 21 is pinned in `.java-version`; Java source/target remains 8. Use the checked-in Gradle wrapper, Android SDK 36 and Build-Tools 35.0.0.
- Point ignored `local.properties` at the local SDK or use ANDROID_HOME. Do not commit machine-specific JAVA_HOME.
- Run `./scripts/verify-ci.sh` for code/build changes. It builds Debug and instrumentation APKs, runs JVM tests and both App Lint variants without release signing keys.
- Run `./scripts/verify-release.sh` for release validation when real signing configuration is available. Do not generate substitute release keys or claim a signed gate passed from an unsigned build.
- Instrumentation compilation is not execution. For device-sensitive changes, run `ANDROID_SERIAL=<serial> ./gradlew --no-daemon :app:connectedDebugAndroidTest` when a test device is available; report missing runtime coverage.
- Image saving regression: API 21–22 install permission; API 23–28 grant/deny/retry/permanent denial; API 29+ no storage prompt. Check permission-result delivery after activity recreation and verify saved media.
- Report commands/results and limitations. Never describe skipped/cached checks as newly executed device tests.
