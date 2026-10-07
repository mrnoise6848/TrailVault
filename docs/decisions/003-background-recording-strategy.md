# Recording lifetime and pauses

A location foreground service is started by a visible Activity after precise permission is
confirmed. Pausing removes GPS listeners and freezes active duration. Resuming adds a segment
so paused travel is not counted or drawn. Active time uses elapsedRealtime, not wall-clock.
Route points are persisted before publishing. Metadata checkpoints bound duration loss.
After process termination a durable session is recovered paused; automatic restart does not
pretend to have recorded an unobserved gap. Resume requires a user action while visible.
No background location permission, boot receiver, wake lock or battery exemption request.
