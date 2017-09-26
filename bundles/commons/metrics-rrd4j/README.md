# Apache Sling RRD4J metrics reporter

This is a bundle that stores metrics on the local filesystem using
[RRD4J](https://github.com/rrd4j/rrd4j).

Build this bundle with Maven:

    mvn clean install

The reporter will not store metrics by default. You need to configure it and
tell the reporter what metrics to store.

Go to the Apache Felix Web Console and configure 'Apache Sling Metrics reporter
writing to RRD4J'. The reporter will start storing metrics once data sources
have been added and the configuration is saved. Please note, the metrics file
is recreated/cleared whenever the configuration is changed.