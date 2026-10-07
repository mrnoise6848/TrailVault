# TrailVault

Record your route. Keep it yours.

## The problem

Many route tracking apps revolve around accounts, social feeds and cloud services.
A walk or ride should be easy to record, review and keep on your own device.

## The solution

TrailVault is a focused, local-first Android route recorder built with Kotlin and
Jetpack Compose: **Record → Pause → Save → Review → Export**.

## Features

- Real GPS recording for walking, hiking and cycling, with quality filtering.
- Pause/resume, active duration, estimated distance/speed and qualified elevation gain.
- Location foreground service, recording notification and pause control.
- Durable local points and paused recovery after process interruption.
- Route history, detail maps, rename, favorites and confirmed deletion.
- Activity, notes, tags, local search and recent/favorite/activity filters.
- GPX 1.0/1.1 track/route import and segmented GPX 1.1 export via Android's document picker.
- Optional OpenStreetMap tiles with explicit privacy consent and local caching.

## Getting started

Open the existing project in Android Studio with its configured toolchain and SDK.
No API key, account or backend is required. Preserve the pinned project configuration.
Build a debug APK with `./gradlew :app:assembleDebug` (this does not execute tests).
Install on Android 10/API 29 or later. Choose Start Route, grant precise location and,
optionally, notifications. Move outdoors for GPS fixes. Pause or Finish & Save;
My Routes provides history, import and route management. Export from route details.

## Screenshots

No device screenshots have been captured. No fabricated route screenshots are included.

## Architecture

Existing single app module and Compose theme retained. Activity handles document/permission
launchers and lightweight screen selection; a repository owns local queries; a service
controls the recording engine. SQLite stores points once and caches completed statistics.
See [architecture](docs/architecture.md) and [decisions](docs/decisions/).

## Background tracking

The user starts a location foreground service while the app is visible. It can continue
with the screen off or app backgrounded, subject to Android/device restrictions. Pausing
removes GPS listeners. A paused session keeps its notification while the service is alive.
After process termination/reboot/force-stop, reopening restores the saved session **paused**;
there is no unattended GPS restart or reconstructed missing travel. Checkpoints occur every
five seconds. See [failure handling](docs/failure-handling.md).

## GPX

Document-picker import/export needs no broad storage permission. Import rejects malformed,
empty, excessive and invalid-coordinate files (16 MiB, 100,000 points, XML depth 32).
Waypoint-only files are unsupported. Unknown timestamps are never fabricated. Export
preserves every point, optional altitude and segment boundaries; preview simplification
never affects the file. See [GPX](docs/gpx.md).

## Battery and performance

Five-second/three-meter GPS updates, no updates while paused, constant-memory live statistics,
bounded point/map windows, streamed exports, IO database work and bounded map caching.
See [performance](docs/performance.md) and [statistics](docs/statistics.md).

## Privacy

No accounts, analytics, location uploads or backend. Private local storage; app route data
is excluded from Android backup/transfer. Export before uninstalling. Online map tiles are
off by default; enabling them discloses your IP and viewed areas to OpenStreetMap. A chosen
cloud document provider can upload an explicitly exported GPX. See [privacy](docs/privacy.md).

## Limitations

- Runtime GPS/background/permission/battery behavior has not been verified on a device.
- Cached tiles are best-effort; complete offline maps are not implemented.
- osmdroid 6.1.20 is mature but upstream is archived; reassess before long-term distribution.
- GPS metrics are estimates. Elevation gain requires trustworthy vertical accuracy.
- Process termination can lose the most recent interval of active duration, up to ~5 seconds.
- Map previews are simplified; live map shows up to 6,000 recent accepted points.
- Imported multiple tracks are combined into one route with separate segments.
- Interface text is English; translations and device visual QA remain future work.

## Testing and validation

No unit, instrumentation, UI, integration or benchmark tests were executed during these
implementation phases, as required by the specification. Existing sample tests are preserved.
Compilation/debug packaging and static review are distinct from runtime testing. See
[implementation report](docs/implementation-report.md) for the final recorded build outcome.

## Roadmap

Device validation, release profiling, localization, maintained map-provider evaluation and
optional user-managed offline maps. Domain/storage boundaries permit future summaries or
route similarity without adding an AI backend to this MVP.

## License

A project source-code license has not been selected by the owner. osmdroid is Apache-2.0;
its license and notices are bundled in app assets. OpenStreetMap attribution and tile-use
policy apply to map data. See [third-party notices](app/src/main/assets/third_party_notices.txt).
