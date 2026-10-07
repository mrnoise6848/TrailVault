# Statistics

Distance uses Android's geodesic distance between accepted adjacent points in a segment.
Average speed = distance / active duration; pauses excluded. Moving time estimates intervals
(up to 60 seconds) with reported GPS speed >=0.8 m/s; without speed it is unavailable.
Elevation gain is available only with vertical accuracy <=10 m and at least two reliable
altitude samples; a 5 m hysteresis rejects small altitude noise. Missing altitude breaks
smoothing. Imported altitude without quality is displayed as unavailable gain.
All GPS distances and elevation are estimates, not survey-grade measurements.
