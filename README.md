# SlingQuery
SlingQuery is a Sling resource tree traversal tool inspired by the [jQuery](http://api.jquery.com/category/traversing/tree-traversal/).

## Introduction

Recommended way to find resources in the Sling repository is using tree-traversal methods, like `listChildren()` and `getParent()` rather than JCR queries. The latter are great for listing resources with given properties, but we can't leverage the repository tree structure with such queries. On the other hand, using tree-traversal method is quite verbose. Consider following code that takes an resource and returns its first ancestor, being `cq:Page`, with given `jcr:content/cq:template` attribute:

    Resource resource = ...;
    while ((resource = resource.getParent()) != null) {
        if (!resource.isResourceType("cq:Page")) {
            continue;
        }
        Resource template = resource.getChild("jcr:content/cq:template");
        if (template != null && "my/template".equals(template.adaptTo(String.class))) {
            break;
        }
    }
    if (resource != null) {
        // we've found appropriate ancestor
    }

SlingQuery is a tool that helps creating such queries in a more concise way. Above code could be written as:

    import static org.apache.sling.query.SlingQuery.$;
    // ...
    $(resource).closest("cq:Page[jcr:content/cq:template=my/template]")

Dollar sign is a static method that takes the resource array and creates SlingQuery object. The `closest()` method returns the first ancestor matching the selector string passed as the argument.

SlingQuery is inspired by the jQuery framework. jQuery is the source of method names, selector string syntax and the dollar sign method used as a collection constructor.

## Features

* useful [operations](https://github.com/Cognifide/Sling-Query/wiki/Method-list) to traverse the resource tree,
* flexible [filtering syntax](https://github.com/Cognifide/Sling-Query/wiki/Selector-syntax),
* lazy evaluation of the query result,
* `SlingQuery` object is immutable (thread-safe),
* fluent, friendly, jQuery-like API.

## Installation

Add following Maven dependency to your `pom.xml`:

	<dependency>
		<groupId>org.apache.sling</groupId>
		<artifactId>org.apache.sling.query</artifactId>
		<version>3.0.0</version>
	</dependency>

## Documentation

* [CIRCUIT 2014 presentation](http://cognifide.github.io/Sling-Query/circuit2014/)
* [Basic ideas](https://github.com/Cognifide/Sling-Query/wiki/Basic-ideas)
* [Method list](https://github.com/Cognifide/Sling-Query/wiki/Method-list)
* [Selector syntax](https://github.com/Cognifide/Sling-Query/wiki/Selector-syntax)
	* [Operator list](https://github.com/Cognifide/Sling-Query/wiki/Operator-list)
	* [Modifier list](https://github.com/Cognifide/Sling-Query/wiki/Modifier-list)
	* [Hierarchy operator list](https://github.com/Cognifide/Sling-Query/wiki/Hierarchy-operator-list)
* [Examples](https://github.com/Cognifide/Sling-Query/wiki/Examples)

## External resources

* See the [Apache Sling website](http://sling.apache.org/) for the Sling reference documentation. Apache Sling, Apache and Sling are trademarks of the [Apache Software Foundation](http://apache.org).
* Method names, selector syntax and some parts of documentation are inspired by the [jQuery](http://jquery.com/) library.
