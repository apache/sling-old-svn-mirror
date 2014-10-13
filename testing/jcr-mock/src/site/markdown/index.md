## About JCR Mocks

Mock implementation of selected JCR APIs.

### Maven Dependency

```xml
<dependency>
  <groupId>org.apache.sling</groupId>
  <artifactId>org.apache.sling.testing.jcr-mock</artifactId>
  <version>1.0.0-SNAPHOT</version>
</dependency>
```

### Documentation

* [Usage](usage.html)
* [API Documentation](apidocs/)
* [Changelog](changes-report.html)

### Implemented mock features

The mock implementation supports:

* Reading and writing all data (primitive values, arrays, binary data) via the JCR API
* Creating any number of nodes and properties (stored in-memory in a hash map)
* Register namespaces

The following features are *not supported*:

* Node types are supported in the API, but their definitions and constraints are not applied
* Versioning not supported
* Search not supported
* Transactions not supported
* Observation events can be registered but are ignored
* Access control always grants access
* Exporting/Importing data via document and system views not supported 
* Workspace management methods not supported
