# Offline behavior

Recording, route storage, history, details and GPX processing do not need a network.
Map tile networking is off by default. Enabling it explains the disclosure of IP and
viewed areas to OpenStreetMap. Disabling stops new requests; already-started requests may
complete. Cached tiles are evictable and are not a complete offline region download.
Both map screens distinguish durable route data from best-effort cached/online tiles.
There is no tile prefetch or offline-region promise. User-selected SAF providers may need
network access for import/export even though GPX processing itself is local.
