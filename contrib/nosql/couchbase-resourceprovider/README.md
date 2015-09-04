Apache Sling NoSQL Couchbase Resource Provider
==============================================

Sling ResourceProvider implementation that uses [Couchbase](http://www.couchbase.com/) NoSQL database as persistence.

Based on the "Apache Sling NoSQL Generic Resource Provider" and "Apache Sling NoSQL Couchbase Client".


Configuration on deployment
---------------------------

* To use the resource provider you have to to create a factory configuration for "Apache Sling NoSQL Couchbase Client" with clientId = ´sling-resourceprovider-couchbase´ and propert couchbase host and bucket configuration.
* Additionally a factory configuration for "Apache Sling NoSQL Couchbase Resource Provider Factory" defines the root of the resource tree that should be stored in Couchbase


Couchbase Views for path-based access
-------------------------------------

For list and delete operations two couchbase views have to be defined and published in the bucket that is used by the resource provider.

Steps to create those views:
* Log into Couchbase Console
* Go to "Views" and select the correct bucket
* Add a new design document via "Create Development View" and name it "\_design/dev\_resourceIndex" (the prefix "\_design/dev\_" is added automatically)
* Use the name "ancestorPath" for the first view that is created together with the design document
* Paste the view code from [ancestorPath.js](src/main/couchbase-views/ancestorPath.js) into the editor and save it
* Create another view named "parentPath", paste the view code from [parentPath.js](src/main/couchbase-views/parentPath.js) and save it
* Publish the design document so the views are production views


Run integration tests
---------------------

To run the integration tests you have to set up a real couchbase server and run the tests with this command line (inserting the correct parameters for couchbase host and bucket):

```
mvn -Pcouchbase-integration-test -DcouchbaseHosts=localhost:8091 -DbucketName=test integration-test
```

