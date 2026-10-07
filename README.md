# TrailVault

**Record the route. Keep the record.**

A walk or ride can be worth saving without posting it to a feed or creating a cloud account. TrailVault records GPS routes on Android, keeps them in a local library, and exports GPX so the record can travel with you.

Start a route, pause when you stop, and save it when you finish. Review the map and estimated distance, duration and speed; add a name, activity, notes or tags to make it easier to find later. Favorites and local search help turn recorded tracks into a usable history.

## Recording through real interruptions

GPS recording has gaps: poor fixes, pauses, lost permissions and process interruption. TrailVault preserves those boundaries in the route instead of connecting every point into one continuous trip.

A user-started location foreground service owns recording. Accepted fixes pass quality checks and are stored as they arrive. Pausing removes the GPS listener; resuming starts a new segment. Active duration excludes pauses. Permission/storage failures leave a paused route that can be recovered or finished later.

After process death, reboot or force-stop, reopening restores the saved session **paused**. Recording does not restart unattended, and missing travel is not reconstructed. Duration checkpoints run every five seconds, so abrupt termination can lose the latest interval. [Recording engine](app/src/main/java/com/noise/trailvault/recording/RecordingEngine.kt) · [Recovery behavior](docs/failure-handling.md)

## A small map preview, a complete GPX

The live map uses at most 6,000 recent accepted points, and saved previews can be simplified. SQLite retains the route; GPX export streams its full point set, including segment boundaries and optional altitude. Rendering a manageable preview does not reduce the exported track.

GPX 1.0/1.1 track/route import supports multiple segments, validates coordinates and keeps unknown timestamps unknown. Imports are bounded at 16 MiB, 100,000 points and XML depth 32. Waypoint-only files are unsupported; multiple tracks become one route with separate segments. [GPX format and limits](docs/gpx.md)

Distance and speed remain GPS estimates. Elevation gain requires qualified vertical accuracy and a 5 m hysteresis to suppress small fluctuations. [Statistics definitions](docs/statistics.md)

## Take it for a short walk

Android 10 / API 29 or newer, using the project's configured toolchain:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Choose Start Route, allow precise location and move outdoors for fixes. Try Pause/Resume and recording with the screen off, then Finish & Save. Open My Routes, inspect the details, export through the document picker and import the GPX again.

## Maps and local ownership

Online OpenStreetMap tiles are optional and off by default. Enabling them reveals the public IP and viewed areas to the tile service. Cached tiles are best-effort rather than a complete offline-map package. Route storage itself does not require online tiles.

Routes are excluded from app backup/transfer. There is no account, analytics or location-upload service; a chosen cloud document provider may upload an explicitly exported GPX. Export before uninstalling. [Privacy](docs/privacy.md)

## Implementation status

The [build record](docs/implementation-report.md) reports successful compilation and debug packaging. Device GPS, background/lifecycle, battery and visual validation remain pending; no runtime or performance result is claimed. Android/device restrictions can affect background recording.

The single Compose module separates recording service, repository and UI; platform location, SQLite and XML APIs support the core workflow. [Architecture](docs/architecture.md) · [Performance bounds](docs/performance.md)

The archived osmdroid dependency needs reassessment for long-term distribution. No project source license is selected; [third-party notices](app/src/main/assets/third_party_notices.txt) cover library/map obligations.
