## Usage

The factory class `MockSling` allows to instantiate the different mock implementations.

### Sling Resource Resolver

Example:

```java
// get a resource resolver
ResourceResolver resolver = MockSling.newResourceResolver();

// get a resource resolver backed by a specific repository type
ResourceResolver resolver = MockSling.newResourceResolver(ResourceResolverType.JCR_MOCK);
```
The following types are supported currently: [Resource Resolver Types](resource-resolver-types.html)

### Adapter Factories

You can register your own or existing adapter factories to support adaptions e.g. for classes extending `SlingAdaptable`.

Example:

```java
// register adapter factory
BundleContext bundleContext = MockOsgi.newBundleContext();
MockSling.setAdapterManagerBundleContext(bundleContext);
bundleContext.registerService(myAdapterFactory);

// test adaption
MyClass object = resource.adaptTo(MyClass.class);

// cleanup after unit test
MockSling.clearAdapterManagerBundleContext();
```

Make sure you clean up the adapter manager bundle association after running the unit test otherwise it can 
interfere with the following tests. If you use the `SlingContext` Junit rule this is done automatically for you.


### SlingScriptHelper

Example:

```java
// get script helper
SlingScriptHelper scriptHelper = MockSling.newSlingScriptHelper();

// get request
SlingHttpServletRequest request = scriptHelper.getRequest();

// get service
MyService object = scriptHelper.getService(MyService.class);
```

To support getting OSGi services you have to register them via the `BundleContext` interface of the
[JCR Mocks][jcr-mock] before. You can use an alternative factory method for the `SlingScriptHelper` providing
existing instances of request, response and bundle context. 


### SlingHttpServletRequest

Example for preparing a sling request with custom request data:

```java
// prepare sling request
ResourceResolver resourceResolver = MockSling.newResourceResolver();
MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver);

// simulate query string
request.setQueryString("param1=aaa&param2=bbb");

// alternative - set query parameters as map
request.setParameterMap(ImmutableMap.<String,Object>builder()
    .put("param1", "aaa")
    .put("param2", "bbb")
    .build());

// set current resource
request.setResource(resourceResolver.getResource("/content/sample"));

// set sling request path info properties
MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
requestPathInfo.setSelectorString("selector1.selector2");
requestPathInfo.setExtension("html");

// set method
request.setMethod(HttpConstants.METHOD_POST);

// set attributes
request.setAttribute("attr1", "value1");

// set headers
request.addHeader("header1", "value1");

// set cookies
request.addCookie(new Cookie("cookie1", "value1"));
```

### SlingHttpServletResponse

Example for preparing a sling response which can collect the data that was written to it:

```java
// prepare sling response
MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

// execute your unit test code that writes to the response...

// validate status code
assertEquals(HttpServletResponse.SC_OK, response.getStatus());

// validate content type and content length
assertEquals("text/plain;charset=UTF-8", response.getContentType());
assertEquals(CharEncoding.UTF_8, response.getCharacterEncoding());
assertEquals(55, response.getContentLength());

// validate headers
assertTrue(response.containsHeader("header1"));
assertEquals("5", response.getHeader("header2"));

// validate response body as string
assertEquals(TEST_CONTENT, response.getOutputAsString());

// validate response body as binary data
assertArrayEquals(TEST_DATA, response.getOutput());
```


[jcr-mock]: http://sling.apache.org/testing/jcr-mock/
