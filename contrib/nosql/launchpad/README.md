Apache Sling NoSQL Sample Launchpad
===================================

Variant of the Sling Launchpad to run NoSQL Resource Providers in standalone mode.

To run with NoSQL MongoDB resource provider:

```
java -Dsling.run.modes=nosql-mongodb -jar target/org.apache.sling.nosql.launchpad-1.0.0-SNAPSHOT.jar -c sling -f -
```

To run with NoSQL Couchbase resource provider:

```
java -Dsling.run.modes=nosql-couchbase -jar target/org.apache.sling.nosql.launchpad-1.0.0-SNAPSHOT.jar -c sling -f -
```
