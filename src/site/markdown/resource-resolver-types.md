## Resource Resolver Types

The Sling Mocks resource resolver implementation supports different "types" of adapters for the mocks.
Depending on the type an underlying JCR repository is used or not, and the data ist stored in-memory or in a real 
repository.

This pages lists all resource resolver types currently supported.

### RESOURCERESOLVER_MOCK (default)

* Simulates an In-Memory resource tree, does not provide adaptions to JCR API.
* Based on the [Sling resourceresolver-mock implementation][resourceresolver-mock] implementation
* You can use it to make sure the code you want to test does not contain references to JCR API.
* Behaves slightly different from JCR resource mapping e.g. handling binary and date values.
* This resource resolver type is very fast because data is stored in memory and no JCR mapping is applied.

### JCR_MOCK

* Based on the [JCR Mocks][jcr-mock] implementation
* Uses the productive [Sling JCR resource provider implementation][jcr-resource] internally to do the Resource-JCR mapping
* Is quite fast because data is stored only in-memory

### JCR_JACKRABBIT

* Uses a real JCR Jackrabbit implementation (not Oak) as provided by [sling/commons/testing][sling-comons-testing]
* Full JCR/Sling features supported e.g. observations manager, transactions, versioning
* Uses the productive [Sling JCR resource provider implementation][jcr-resource] internally to do the Resource-JCR mapping
* Takes some seconds for startup on the first access 
* All node types that are used when reading/writing data have to be registered

_Warnings/Remarks_

* The repository is not cleared for each unit test, so make sure us use a unique node path for each unit test.
* To import Sling content you have to fully register all node types required for the data
* The [sling/commons/testing][sling-comons-testing] dependency introduces a lot of further dependencies from
  jackrabbit and others, be careful that they do not conflict and are imported in the right order in your test project

To use this type you have to declare an additional dependency in your test project:

```xml
<dependency>
  <groupId>org.apache.sling</groupId>
  <artifactId>org.apache.sling.testing.sling-mock-jackrabbit</artifactId>
  <version>1.0.0-SNAPHOT</version>
  <scope>test</scope>
</dependency>
```


[jcr-mock]: http://sling.apache.org/testing/jcr-mock/
[jcr-resource]: http://svn.apache.org/repos/asf/sling/trunk/bundles/jcr/resource
[resourceresolver-mock]: http://svn.eu.apache.org/repos/asf/sling/trunk/testing/resourceresolver-moc
[sling-comons-testing]: http://svn.apache.org/repos/asf/sling/trunk/bundles/commons/testing
