## About Sling Mocks

Mock implementation of selected Sling APIs.


### Maven Dependency

```xml
<dependency>
  <groupId>org.apache.sling</groupId>
  <artifactId>org.apache.sling.testing.sling-mock</artifactId>
  <version>1.0.0-SNAPHOT</version>
</dependency>
```

### Documentation

* [Sling Mocks Usage][usage-mocks]
* [Content Loader Usage][usage-content-loader]
* [API Documentation][apidocs]
* [Changelog][changelog]


### Implemented mock features

The mock implementation supports:

* `ResourceResolver` implementation for reading and writing resource data using the Sling Resource API
    * Backed by a [mocked][jcr-mock] or real Jackrabbit JCR implementation
    * Uses the productive [Sling JCR resource provider implementation][jcr-resource] internally to do the Resource-JCR mapping
    * Alternatively the non-JCR mock implementation provided by the 
   [Sling resourceresolver-mock implementation][resourceresolver-mock] can be used
* `AdpaterManager` implementation for registering adapter factories and resolving adaptions
    * The implementation is thread-safe so it can be used in parallel running unit tests
* `SlingScriptHelper` implementation providing access to mocked request/response objects and supports getting
   OSGi services from the [mocked OSGi][osgi-mock] environment.
* Implementations of the servlet-related Sling API classes like `SlingHttpServletRequest` and `SlingHttpServletRequest`
    * It is possible to set request data to simulate a certian Sling HTTP request
* Additional services like `MockModelAdapterFactory` and  `MimeTypeService` 

[osgi-mock]: http://sling.apache.org/testing/osgi-mock/
[jcr-mock]: http://sling.apache.org/testing/jcr-mock/
[jcr-resource]: http://svn.apache.org/repos/asf/sling/trunk/bundles/jcr/resource
[resourceresolver-mock]: http://svn.eu.apache.org/repos/asf/sling/trunk/testing/resourceresolver-moc

The following features are *not supported*:

* It is not possible (nor intended) to really execute sling components/scripts and render their results.
    * The goal is to test supporting classes in Sling context, not the sling components/scripts themselves

See [Sling Mocks Usage][usage-mocks].


### Additional features

Additional features provided:

* `ContentLoader` supports importing JSON data and binary data into the mock resource hierarchy to easily 
  prepare a test fixture consisting of a hierarchy of resources and properties.
    * The same JSON format can be used that is provided by the Sling GET servlet for output

See [Content Loader Usage][usage-content-loader].

[usage-mocks]: usage-mocks.html
[usage-content-loader]: usage-content-loader.html
[apidocs]: apidocs/
[changelog]: changes-report.html
