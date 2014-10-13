## Usage

### Getting OSGi mock objects

The factory class `MockOsgi` allows to instantiate the different mock implementations.

Example:

```java
// get bundle context
BundleContext bundleContext = MockOsgi.newBundleContext();

// get component context
Dictionary<String,Object> properties = new Hashtable<>();
properties.put("prop1", "value1");
BundleContext bundleContext = MockOsgi.newComponentContext(properties);
```

It is possible to simulate registering of OSGi services (backed by a simple hash map internally):

```java
// register service
bundleContext.registerService(MyClass.class, myService, properties);

// get service instance
ServiceReference ref = bundleContext.getServiceReference(MyClass.class.getName());
MyClass service = bundleContext.getService(ref);
```

### Activation and Dependency Injection

It is possible to simulate OSGi service activation, deactivation and dependency injection and the mock implementation
tries to to its best to execute all as expected for an OSGi environment.

Example:

```java
// get bundle context
BundleContext bundleContext = MockOsgi.newBundleContext();

// create service instance manually
MyService service = new MyService();

// inject dependencies
MockOsgi.injectServices(service, bundleContext);

// activate service
MockOsgi.activate(service, props);

// operate with service...

// deactivate service
MockOsgi.deactivate(service);
```

Please note: The injectServices, activate and deactivate Methods can only work properly when the SCR XML metadata files
are preset in the classpath at `/OSGI-INF`. They are generated automatically by the Maven SCR plugin, but might be
missing if your clean and build the project within your IDE (e.g. Eclipse). In this case you have to compile the
project again with maven and can run the tests - or use a Maven IDE Integration like m2eclipse.
