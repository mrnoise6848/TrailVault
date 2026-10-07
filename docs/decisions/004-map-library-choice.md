# Map library

Use osmdroid 6.1.20, Apache-2.0, a mature Android raster map renderer with local tile caching.
The upstream repository was archived in November 2024: this is an explicit maintenance
limitation, not a claim of active support. It has no account/API key or location upload SDK.
Only this dependency is added. Its Java Android API fits the existing single Compose module.
Review a maintained replacement before long-term distribution; no upstream source copied.
OSM standard HTTPS tiles, attribution, identifying User-Agent, bounded cache; no bulk download
or prefetch. Respect https://operations.osmfoundation.org/policies/tiles/ .
Network tile access requires explicit user opt-in; tile requests reveal viewed areas and IP.
https://github.com/osmdroid/osmdroid and Maven Central list source and license.
