# Location quality

GPS updates requested every 5 seconds with 3 m displacement. Reject nonfinite/out-of-range
coordinates, monotonic fix age >30 s, absent/negative accuracy or accuracy >75 m,
out-of-order timestamps and jumps >70 m/s within a minute. This generous speed threshold
allows cycling. Retain missing altitude/speed as unavailable. Gaps >60 s create a segment
boundary so statistics never connect a signal outage with a straight line. Pauses do the same.
Accuracy is approximate; distance still includes GPS noise. No coordinates are logged.
