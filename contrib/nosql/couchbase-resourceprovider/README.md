Apache Sling NoSQL Couchbase Resource Provider
==============================================

Sling ResourceProvider implementation that uses [Couchbase](http://www.couchbase.com/) NoSQL database as persistence.

Based on the "Apache Sling NoSQL Generic Resource Provider" and "Apache Sling NoSQL Couchbase Client".

Couchbase Server 4.0 with N1QL support is required for this implementation.


Configuration on deployment
---------------------------

* To use the resource provider you have to to create a factory configuration for "Apache Sling NoSQL Couchbase Client" with clientId = ´sling-resourceprovider-couchbase´ and propert couchbase host and bucket configuration.
* Additionally a factory configuration for "Apache Sling NoSQL Couchbase Resource Provider Factory" defines the root of the resource tree that should be stored in Couchbase


Run integration tests
---------------------

To run the integration tests you have to set up a real couchbase server and run the tests with this command line (inserting the correct parameters for couchbase host and bucket):

```
mvn -Pcouchbase-integration-test -DcouchbaseHosts=localhost:8091 -DbucketName=test integration-test
```
