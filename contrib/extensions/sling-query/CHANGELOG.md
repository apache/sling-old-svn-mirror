## SlingQuery 1.4.2

* Added new add(...) method

## SlingQuery 1.4.1

* Fixed bug in the QUERY search strategy related

## SlingQuery 1.4.0

* `not()` function is now lazy (eg. `$(resourceResolver).not('cq:InvalidType').first()` return results immediately)
* multiple selectors can be joined with comma: `$('[jcr:title=Foo], [jcr:title=Bar])`
* JCR query becomes the default SearchStrategy

## SlingQuery 1.3.1

* fixed `javax.jcr.RepositoryException: invalid node type name` bug (reported by Dawid Jędraszek)

## SlingQuery 1.3.0

* lazy `asList()` method

## SlingQuery 1.2.0

* new strategy for the `find()`: `JCR`

## SlingQuery 1.1.0

* new selector features:
	* [hierarchy operators](https://github.com/Cognifide/Sling-Query/wiki/Hierarchy-operator-list),
	* [resource name](https://github.com/Cognifide/Sling-Query/wiki/Selector-syntax#wiki-resource-name),
	* [new attribute operators](https://github.com/Cognifide/Sling-Query/wiki/Operator%20list)
* two strategies for the `find()` method: `DFS` and `BFS`

## SlingQuery 1.0.0

* first released version
