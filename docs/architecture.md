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
