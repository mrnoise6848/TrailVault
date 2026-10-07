# Failure handling

GPS outages report status and retain durable points. Permission loss removes GPS updates
and pauses the route, with settings/re-request flow before resume. Storage failure pauses
recording and surfaces an error; the prior committed points/checkpoint remain recoverable.
Completion is transactional and a failed finish retains a paused route for retry. A service
start failure is reported, not swallowed. Process recreation recovers a paused route.
SAF errors and invalid/large GPX files produce feedback; successful exports are acknowledged.
Foreground service uses START_NOT_STICKY: no unattended restart or invented gap recording.
Five-second checkpoints can lose up to one interval of active duration on abrupt process kill.
Android force-stop and device reboot require reopening the app; GPS gaps are not reconstructed.
