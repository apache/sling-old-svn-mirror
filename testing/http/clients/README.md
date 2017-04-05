# Sling Http Clients

`SlingClient` is a specialized
[`HttpClient`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/HttpClient.html)
that provides additional functionalities specific to Sling. It is designed to be easy to use out of the box, but also fully customizable.
This library comes with a bunch of other specialized clients (built on top of `SlingClient`) that are ready to use.

## <a name="architecture"></a> Architecture

`SlingClient`  implements the `HttpClient` interface, but [deletegates](https://en.wikipedia.org/wiki/Delegation_pattern)
this functionality to a `private final CloseableHttpClient http` field.
The config is stored in a `private final SlingClientConfig config` field which is immutable and may be shared across multiple clients
(more about it in the [How to configure a SlingClient](#config) section).
These two objects define the state of the client and are built to make the client thread safe.

`SlingClient` is designed in two layers:
* The base `class AbstractSlingClient implements HttpClient` provides an overlay of basic http methods such as `doGet()`,
  `doPost()` & similar. These are meant to be full replacements of the ones in `HttpClient` for Sling specific needs,
  and they add specific customizations. One particularity is that they all return `SlingHttpResponse`, an augmented `HttpResponse`.

  Still, all the methods from `HttpClient` are exposed (through inheritance and delegation) in case one needs the raw functionality.
  Some useful methods to manipulate Sling paths and URLs have also been added (`getUrl()`, `getPath()`).

  This class encapsulates the mechanisms for extensibility (immutable config field, delegate client field, package private constructor,
  `adaptTo()`), but it is defined as abstract and should never be used directly.

* The main `class SlingClient extends AbstractSlingClient` is the one that adds Sling specific methods (`createNode()`,
  `deletePath()` etc.). It has no fields, but makes use of everything that was defined in the super class.
  Another main functionality defined in `SlingClient` are the mechanisms to instantiate a SlingClient (and any other sub-class):

  * constructor: `public SlingClient(URI url, String user, String password) throws ClientException`

  * builder: `public final static class Builder extends InternalBuilder<SlingClient>` (more in [How to write a `Builder`](#builder))

Any client you write should extend `SlingClient` (more in [How to extend `SlingClient`](#extend))

## <a name="instantiate"></a> How to instantiate `SlingClient`
There are several ways to obtain a SlingClient (and sub-client) instance, depending on the resources available:

* constructor `SlingClient(URI url, String user, String password)` - handy for obtaining a simple client from the url:
  ```java
  SlingClient c = new SlingClient(URI.create("localhost:8080"), "admin", "admin");
  ```

* builder `class Builder<T extends Builder> extends HttpClientBuilder` - this allows for more complex clients to be created, e.g.
  with different authentication mechanism, or additional interceptors:
  ```java
  SlingClient c = SlingClient.Builder.create("localhost:8080", "admin", "admin").build();
  ```
  This gives the possibility to customize the HttpClient (e.g. add interceptors, change auth method) before constructing it.

* `public <T extends AbstractSlingClient> T adaptTo(Class<T> clientClass)` is the convenient method to obtain another specialized
client form an existing one. The advantage is that the two will share the same configuration and http handler, so they will behave
like two different "facets" of the same client (think about the analogy of a Web browser with multiple tabs).

Although the constructor and the builder are very handy, the preferred way of working with clients is to obtain it using one of the
Junit Rules provided (e.g. `ExistingQuickstart`) and then call `adaptTo()`.

## <a name="config"></a> How to configure `SlingClient`
All the configs specific to `SlingClient` are stored in `private final SlingClientConfig config` which contains fields such as
`url`, `cookieStore` and authentication parameters. These fields can be set only indirectly, through constructor or Builder, and only
before constructing the client. They cannot be changed, so if you need to change something, you must instantiate another client.

`SlingClient` was designed to be immutable, so thread safe. You don't have to worry about synchronizing it when running tests in parallel.
Also, the immutable config is the base for the `adaptTo()` mechanism, since the two clients can share the same config.

## <a name="extend"></a> How to extend `SlingClient`
The `SlingClient` was designed with extensibility in mind. That's why it provides only basic functionality, leaving other specialized
clients to implement the rest. To create a new client class (let's call it `MyClient`), you need to:
* extend SlingClient: `class MyClient extends SlingClient`
* implement the two constructors:
  * the one for simple uses:
  ```java
    public MyClient(URI serverUrl, String userName, String password) throws ClientException {
      super(serverUrl, userName, password);
    }
    ```
  * the one used by `adaptTo()` (so don't forget it!):
  ```java
    public MyClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
      super(http, config);
    }
    ```
  * optionally create your `Builder`, but only if needed (more in [How to write a `Builder`](#builder))

A good example of how `SlingClient` can be extended is `OsgiConsoleClient`. Note you can further extend the sub-clients in the same way.

## <a name="builder"></a> How to write a `Builder`
If you need to make your client customizable you will have to write your own Builder (you were no thinking to break the immutability
by adding a setter, right?). Below is an example of how to create the Builder mechanism that you can take and adapt for your needs.
In this case, we try to expose only one field `foo`, but it can be extended to any number of fields. Although it seems complicated,
if you follow exactly the example, you cannot fail. Trying to simplify it will burn you (sooner or later), you have been warned!

A short description of the Builder architecture would be: the `InternalBuilder` contains all the logic while staying extensible, while
`Builder` takes all the credit by exposing the `build()` method. Yet, the `Builder` cannot be extended because all the sub-classes would
return a `SlingClient` when calling `build()` (and not a subclass instance).

```java
@Immutable
public class MyClient extends SlingClient {

    private final String foo;

    public MyClient(URI serverUrl, String user, String password) throws ClientException {
        super(serverUrl, user, password);
    }

    /**
     * Constructor used by Builders and adaptTo(). <b>Should never be called directly from the code.</b>
     *
     * @see AbstractSlingClient#AbstractSlingClient(CloseableHttpClient, SlingClientConfig)
     */
    public MyClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    public static abstract class InternalBuilder<T extends MyClient> extends SlingClient.InternalBuilder<T> {
        protected String foo;

        protected InternalBuilder(URI url, String user, String password) {
            super(url, user, password);
        }

        public InternalBuilder<T> withFoo(String foo) {
          this.foo = foo;
        }
    }

    public final static class Builder extends InternalBuilder<MyClient> {

        private Builder(URI url, String user, String password) {
            super(url, user, password);
        }

        @Override
        public MyClient build() throws ClientException {
            MyClient client = new MyClient(buildHttpClient(), buildSlingClientConfig());
            client.foo = this.foo;
            return client;
        }

        public static Builder create(URI url, String user, String password) {
            return new Builder(url, user, password);
        }
    }
}
```

## FAQ
##### How can I change the server url of an existing client?
You don't. As described in [How to configure a `SlingClient`](#config), you have to instantiate another client to change the config.

##### How can I create a client for a server url with context path?
The server `url` (passed in the constructor or builder) must contain all the elements, including protocol, hostname, port and eventually
the context path, e.g.: `http://localhost:8080/mycontextpath/`.
The url may (or may not) contain the trailing slash. Yet, the client will always store it with a trailing slash:
```java
SlingClient client = new SlingClient("http://localhost:4502/mycontextpath", "user", "pass");
System.out.println(client.getUrl());
// prints http://localhost:4502/mycontextpath/
```

##### How can I customize the underlying `HttpClient`?
The `SlingClient.Builder` directly exposes the most useful methods from `HttpClientBuilder`, but not all of them.
First, check if you can find it there. If you haven't found your specific method, then the `Builder` exposes an `HttpClientBuilder` through
`public HttpClientBuilder httpClientBuilder()` which you can use to config it. Note that in this case you cannot chain the methods
to build the client, so you will need to keep a reference to the `SlingClient.Builder`:
```java
SlingClient.Builder builder = SlingClient.Builder.create("http://localhost:8080", "user", "pass");
HttpClientBuilder httpBuilder = builder.httpClientBuilder();
httpBuilder.setProxy(myProxy);
builder.setUser("another");
SlingClient client = builder.build();
```

##### Why is the `Builder` pattern so complicated? Do I really need two classes?
Don't try to get creative here. Respect the examples provided and don't take shortcuts, otherwise you will hurt yourself.

We have tried different ways of designing the Builder. This is the best compromise between extensibility and simplicity. The
`HttpClientBuilder` does not offer any extensibility support, so `SlingClient.Builder` does not extend it, it just uses it internally.
Always remember that you don't need to create your Builder, unless you want to add custom fields to the client.

##### Why I cannot use the entity's content InputStream?
`SlingClient#doRequest()`, `SlingClient#doGet()`, `SlingClient#doPost()` & co. are all consuming the entity and caching it as
String. This is by design, since there's a big risk to forget closing the connections and to run out of sockets quickly.
If you need the response content as InputStream (e.g. for downloading a binary), you can use `doStreamGet()` or similar. These
methods were written specially to not consume the entity so it's the caller's responsibility to close it when finished. Remember to use
them with caution and only when needed.

##### Can my client use another authentication method?
The username and password required by the constructor and builder are there for convenience (since more than 90% of cases will use
basic auth). But you can easily overwrite the `CredentialsProvider` in Builder so those will be ignored. Or do anything you want with
that `HttpClientBuilder`...

##### How can I obtain the context path?
`client.getUrl().getPath()`

##### How can I obtain the "relative" url (excluding hostname and port, but including context path)?
`client.getUrl(path).getPath()`

##### How can I remove the context path from a path?
`client.getPath(path)`

##### What if I pass an url or a path with or without context path to `getUrl()` or `getPath()`?
We have tried to make these methods as robust as possible. Their job is very clear:
* `getUrl(String path)` to transform a Sling path into a full url
* `getPath(String url)` to transform a full url into a Sling path

Any input that does not respect the contract might not work. Check `AbstractSlingClientGetPathTest` and `AbstractSlingClientGetUrlTest`
for an extensive list of cases that we have considered when writing these methods.

