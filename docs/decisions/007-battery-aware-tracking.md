# Battery-aware tracking

Use platform GPS, five-second/three-meter updates, and stop listeners during pauses.
Avoid aggressive wake locks and battery exemption requests. Publish accepted fixes only;
checkpoint at five-second cadence. Constant-memory statistics and bounded map previews
avoid route-length-dependent recomputation. Export/statistics stream durable coordinates.
