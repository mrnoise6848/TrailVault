# Battery and performance

GPS: 5 second minimum interval / 3 m displacement; no listener while paused or idle.
Service checkpoints and updates its quiet notification every 5 seconds. No wake lock,
alarm, polling server, battery-exemption request or repeated full-route statistic work.
Live statistics accumulate in constant memory. Live point window capped at 6,000; full data
remains in SQLite. Detail preview samples at most ~4,001 points; map renders ~2,001 points.
Polyline segments keep their original IDs; simplified previews never join pauses.
GPX export streams a cursor; completed statistics stream the same stored data. Import is
bounded to 16 MiB/100,000 points and uses one transaction. History uses cached summaries.
SQLite operations and GPX run on IO. Map cache bounded to 64 MiB; no tile prefetch.
Long route history is a metadata list, not an in-memory collection of all track coordinates.
Physical-device battery profiling and runtime validation are still required before release.
