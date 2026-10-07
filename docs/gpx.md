# GPX

GPX 1.1 export uses Android XmlSerializer with namespace, metadata, track name/activity,
notes, coordinate points, UTC timestamps and optional altitude. Segment boundaries preserve
pauses/gaps. TrailVault extensions carry active duration and tags. Filenames are sanitized
and include local start date. SAF CreateDocument lets the user choose the destination; no
broad storage permissions. Writing happens on IO and closes the stream. User-selected cloud
document providers may upload exported files: exporting is an explicit user action.
