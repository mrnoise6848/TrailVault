# TrailVault

**Record a walk or ride, keep the route locally, and export the actual points—not just the map preview.**

A route recorder can be useful without an account, feed or cloud library. The harder part is keeping an honest record when GPS fixes are poor, recording pauses, permissions disappear or Android interrupts the process. Connecting the last point to a new fix across those gaps can imply travel that was never observed.

TrailVault records quality-filtered GPS points in local storage, separates route segments at pauses and gaps, and restores interrupted sessions paused. The workflow is **Record → Pause → Save → Review → Export**, with optional map tiles rather than a mandatory online service.

## A route is more than a line

- Accepted points are stored as recording progresses. Quality checks reject stale, inaccurate and implausible fixes.
- Active duration excludes pauses; distance is calculated within segments. Speed and elevation are estimates, with unavailable values kept explicit.
- Elevation gain requires qualified vertical accuracy and uses a 5 m hysteresis to suppress small altitude fluctuations. Imported altitude without quality does not become a confident gain estimate.
- History supports titles, activities, notes, tags, favorites and local search. Metadata edits leave route geometry intact.

See [recording engine](app/src/main/java/com/noise/trailvault/recording/RecordingEngine.kt) and [statistics definitions](docs/statistics.md).

## Recording and interrupted-session recovery

```text
Visible Start Route + precise-location permission
    → location foreground service
    → quality check → durable points + live statistics
    → pause / finish, or interruption
    → saved route, or recovered paused session
```

A user-started foreground service can continue when the app is backgrounded or the screen is off, subject to Android and device restrictions. Pausing removes location listeners. Permission or storage failure pauses the route and surfaces a reason; failed completion leaves a paused route for retry.

The service uses `START_NOT_STICKY`. Reopening after process death, reboot or force-stop restores saved state **paused**, with a new segment on resume. Missing travel is not reconstructed. Duration checkpoints occur every five seconds; abrupt termination can lose roughly the most recent checkpoint interval. See [failure handling](docs/failure-handling.md).

## Preview, storage and GPX have different jobs

SQLite retains the route points; live maps show at most 6,000 recent accepted points and saved previews are simplified. Export streams the full route from a cursor, preserving points, segment boundaries and optional altitude rather than serializing the simplified preview.

GPX 1.0/1.1 track/route import supports multiple segments, rejects DTD declarations and validates coordinates before storage. Limits are 16 MiB, 100,000 points and XML depth 32. Unknown timestamps stay unknown; waypoint-only files are unsupported. Multiple tracks become one route with separate segments. See [GPX contract](docs/gpx.md).

The app uses a single Kotlin/Compose module: a repository handles local queries, the foreground service owns recording, and Activity launchers handle permissions and document picking. Platform location, SQLite and XML APIs supply the core workflow. [Architecture](docs/architecture.md), [performance bounds](docs/performance.md) and [decisions](docs/decisions/) explain the separation.

## Try a short route first

Use Android Studio and the configured toolchain, or:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

On Android 10 / API 29 or newer, choose Start Route, allow precise location and optionally notifications, then move outdoors for GPS fixes. Pause/resume, turn the screen off during recording, and Finish & Save. Open My Routes, inspect the saved route, export GPX through the document picker and import it again.

Use a route suitable for public sharing if capturing the recording screen and saved-route details. No product screenshots are included yet.

## Local ownership and optional networking

No account, analytics, location-upload service or backend is implemented. Route data is private and excluded from app backup/transfer. Export before uninstalling.

Online OpenStreetMap tiles are off by default. Enabling them discloses the public IP and viewed areas to the tile service; a chosen cloud document provider may upload an explicitly exported GPX. Cached tiles are best-effort, not a complete offline-map package. See [privacy](docs/privacy.md).

## Evidence and remaining limits

The [implementation report](docs/implementation-report.md) records successful Kotlin compilation and debug packaging. It explicitly states that device GPS, lifecycle, permission, battery and visual behavior have not been runtime-validated. Existing tests are templates; no substantive test or benchmark evidence is supplied. This documentation pass did not change that status.

Update cadence and bounded previews are implementation settings, not measured battery/performance results. osmdroid 6.1.20 is an archived upstream dependency that needs reassessment for long-term distribution. Device validation and maintained map-provider evaluation remain the main next steps.

No project source license has been selected. Map/library obligations are recorded in [third-party notices](app/src/main/assets/third_party_notices.txt).
