Apache Sling NoSQL Generic Resource Provider
============================================

Generic implementation of a Sling ResourceProvider that helps writing ResourceProviders using NoSQL databases as persistence.

The generic implementation helps mapping the resource data to document-oriented key-value NoSQL databases like MongoDB or Couchbase.

Features:

* Defines a simplified "NoSqlAdapter" concept that is implemented for each NoSQL database. It boils down to simple get/put/list operations. Query support is optional.
* Complete implementation of Resource, ResourceProvider, ResourceProviderFactory and ValueMap based on the NoSqlAdapter
* "Transaction management" of Sling CRUD (commit/revert methods) is implemented
* ValueMap supports String, Integer, Long, Double, Date, Calendar and InputStream/byte\[\] (binary data) and arrays of them. Date/Calendar and binary data is serialized to a string before storing, so the NoSQL databases have not to support them directly.
* Sends resource notifications via OSGi EventAdmin
* Provides a "tests" JAR that can be used for integration tests with NoSQL databases to test the own adapter implementation
* Can be mounted as root provider without any JCR at all
