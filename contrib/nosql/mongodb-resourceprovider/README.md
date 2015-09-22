Apache Sling NoSQL MongoDB Resource Provider
============================================

Sling ResourceProvider implementation that uses [MongoDB](https://www.mongodb.org/) NoSQL database as persistence.

Based on the "Apache Sling NoSQL Generic Resource Provider".

All resource data is stored in one MongoDB database and one collection, using the path of the resource as "_id" property.


Configuration on deployment
---------------------------

* Create a factory configuration for "Apache Sling NoSQL MongoDB Resource Provider Factory" to define the root of the resource tree that should be stored in MongoDB, and MongoDB connection string, database name and collection name.


Run integration tests
---------------------

To run the integration tests you have to set up a real MongoDB server and run the tests with this command line (inserting the correct parameters):

```
mvn -Pmongodb-integration-test -DconnectionString=localhost:27017 -Ddatabase=sling -Dcollection=resources integration-test
```
