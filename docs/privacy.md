# Privacy

TrailVault stores routes and points in the app's private SQLite database on the device.
No account, backend, analytics, advertising SDK or remote route processing. GPS access
exists only during an explicitly started recording. Pausing/finishing removes updates.
Exact coordinates are never logged. Android cloud backup and device transfer of app data
are excluded; uninstalling may permanently remove routes. Export important routes first.

Online map access is off by default. Enabling it lets OpenStreetMap receive IP address and
requested tile areas, which can reveal approximate location. No track payload is uploaded.
Disabling online maps stops new requests; in-flight requests may finish. Cached map data
is private, bounded and evictable. OSM attribution remains visible.

GPX export includes precise coordinates, timestamps and optional altitude/notes/tags.
Users choose its destination through Android's document picker. A selected cloud provider
may upload that file. Imported GPX is parsed locally and has explicit size/point limits.
Only precise/coarse foreground location, location foreground-service, notifications and
map network permissions are declared. No background-location or shared-storage permission.
