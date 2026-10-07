# Architecture

The existing single Android module, com.noise.trailvault namespace, Compose theme,
AGP 9.4.1, Kotlin 2.4.20, SDK 37/minimum 29 and JVM configuration are preserved.
The starting app is a Compose greeting; no navigation, database or GPS infrastructure exists.
Existing sample tests are retained and are not executed during implementation.

Integration: Activity → Compose screens → application-scoped repository → SQLite.
Recording: started location foreground service → platform LocationManager → durable points.
State: immutable domain values exposed through StateFlow. File access uses SAF.
GPX uses Android's XML pull parser/serializer; no external parser required.
Map is isolated in an AndroidView adapter. No accounts or remote route processing.
All disk operations belong on IO; the service owns location callbacks, not the UI.

## Final integration

- `TrailApplication`: application-scoped recording owner, error/completion flows and repository.
- `RecordingService`: location foreground lifecycle, five-second checkpoints, quiet notification.
- `RecordingEngine`: serialized start/pause/resume/finish/recovery, accepted-fix persistence,
  constant-memory statistics and a bounded recent map window. No Activity references.
- `TrailStore`: SQLite schema v2, upgrade path, cascades, transactions, streamed points,
  cached statistics, bounded detail previews and metadata-only updates.
- `TrailRepository`: IO boundary for history, detail, metadata, deletion, import/export.
- `Gpx`: bounded XML pull import and cursor-fed XML serialization using platform APIs.
- Compose: recording, history, detail, metadata dialogs and osmdroid AndroidView adapter;
  screen/document selection survives Activity recreation. Flows collect while STARTED.

One new library: osmdroid 6.1.20 (Apache-2.0, upstream archived). No added Gradle modules,
location SDK, database wrapper, GPX parser library or Compose lifecycle dependency.
No versions, SDK levels, namespace/application ID or wrapper configuration were changed.
The configured daemon JVM remains 25 and source/target compatibility remains Java 11.
The initial sample tests were inspected and retained without execution.

Future AI work belongs above the repository/domain boundary and requires separate user
approval for any backend or route disclosure. No AI functionality or backend is introduced.
