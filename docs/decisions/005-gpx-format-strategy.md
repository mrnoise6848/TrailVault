# GPX strategy

Use Android XmlPullParser/XmlSerializer rather than add a GPX library. GPX is parsed as
bounded structured XML, not regular expressions; DTD declarations are rejected, namespaces
and point hierarchy validated, no external resolver installed. GPX 1.0/1.1 tracks/routes
are supported; waypoint-only files are rejected. Imports transact only after validation.
Exports stream all durable points and preserve segments. No invented timestamps/altitude.
A namespaced active-duration extension preserves supplied duration metadata up to one year;
otherwise elapsed duration is derived only when all point timestamps are ordered and known.
