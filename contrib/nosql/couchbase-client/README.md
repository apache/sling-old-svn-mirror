Apache Sling NoSQL Couchbase Client
===================================

Provides preconfigured clients for accessing a [Couchbase](http://www.couchbase.com/) NoSQL database.

* Wraps the [Couchbase Java Client 2.x](https://github.com/couchbase/couchbase-java-client) and exports it via OSGi
* Includes private dependencies of Couchbase Java Client
* The couchbase access parameters can be configured via OSGi configuration.
* Multiple couchbase clients can be configured for different couchbase hosts and/or buckets
