# Platform location

Use LocationManager GPS_PROVIDER for real fixes without Play Services or a new location SDK.
Request precise and coarse permissions together in context; approximate-only access does
not meet the route recording accuracy requirement. Explain rationale, recognize settings
situations and recheck revocation. No ACCESS_BACKGROUND_LOCATION permission is required
for an explicitly user-started, visible location foreground service. See Android guidance:
https://developer.android.com/develop/background-work/services/fgs/service-types#location

Persist fixes before publishing. Filter freshness/accuracy/jumps, mark pauses and gaps as
segment boundaries. Duration uses monotonic elapsed time; point times use GPS wall times.
