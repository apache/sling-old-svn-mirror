## Usage

### Import resource data from JSON file in classpath

With the `ContentLoader` it is possible to import structured resource and property data from a JSON file stored
in the classpath beneath the unit tests. This data can be used as text fixture for unit tests.

Example JSON data:

```json
{
  "jcr:primaryType": "app:Page",
  "jcr:content": {
    "jcr:primaryType": "app:PageContent",
    "jcr:title": "English",
    "app:template": "/apps/sample/templates/homepage",
    "sling:resourceType": "sample/components/homepage",
    "jcr:createdBy": "admin",
    "jcr:created": "Thu Aug 07 2014 16:32:59 GMT+0200",
    "par": {
      "jcr:primaryType": "nt:unstructured",
      "sling:resourceType": "foundation/components/parsys",
      "colctrl": {
        "jcr:primaryType": "nt:unstructured",
        "layout": "2;colctrl-lt0",
        "sling:resourceType": "foundation/components/parsys/colctrl"
      }
    }
  }
}
```

Example code to import the JSON data:

```java
ResourceResolver resolver = MockSling.newResourceResolver();
ContentLoader contentLoader = new ContentLoader(resolver);
contentLoader.json("/sample-data.json", "/content/sample/en");
```

This codes creates a new resource at `/content/sample/en` (and - if not existent - the parent resources) and
imports the JSON data to this node. It can be accessed using the Sling Resource or JCR API afterwards.


### Import binary data from file in classpath

With the `ContentLoader` it is possible to import a binary file stored in the classpath beneath the unit tests.
The data is stored usig a nt:file/nt:resource or nt:resource node type. 

Example code to import a binary file:

```java
ResourceResolver resolver = MockSling.newResourceResolver();
ContentLoader contentLoader = new ContentLoader(resolver);
contentLoader.binaryFile("/sample-file.gif", "/content/binary/sample-file.gif");
```

This codes creates a new resource at `/content/binary/sample-file.gif` (and - if not existent - the parent 
resources) and imports the binary data to a jcr:content subnode.
