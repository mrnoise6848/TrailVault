# Local storage

Use Android SQLiteOpenHelper: metadata, ordered points with segment boundaries, and one
cached statistics row per completed route. No second serialized copy of coordinates.
Foreign keys cascade deletion; completing/importing routes uses a transaction. Queries run
on IO. In-progress points are committed individually before UI publication. No shared
storage permission: GPX uses Storage Access Framework. Schema upgrades preserve routes.
