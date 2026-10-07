# GPX

GPX 1.1 export uses Android XmlSerializer with namespace, metadata, track name/activity,
notes, coordinate points, UTC timestamps and optional altitude. Segment boundaries preserve
pauses/gaps. TrailVault extensions carry active duration and tags. Filenames are sanitized
and include local start date. SAF CreateDocument lets the user choose the destination; no
broad storage permissions. Writing happens on IO and closes the stream. User-selected cloud
document providers may upload exported files: exporting is an explicit user action.

Import supports GPX 1.0/1.1 tracks and routes with multiple segments through OpenDocument.
XML is parsed on IO with a 16 MiB read limit, 100,000 point limit, depth 32 and bounded text.
DTD declarations are rejected; no external entity/network resolver is used. Missing or
nonfinite coordinates, malformed altitude/time, invalid XML and empty documents fail
before a database transaction. Missing timestamps remain unknown (zero sentinel internally);
export omits them, timing metrics stay unavailable when none exist. GPX waypoint-only files
are unsupported. Multiple tracks are combined as segments, retaining the first name.
