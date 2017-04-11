<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->
# Apache Sling ESX Scripting Engine

A Node JS (like) module loader for Apache Sling.

## Description
This module implements a Nashorn Apache Sling Script Engine for the "esx" extension.

It requires a function named `render` in the `esx` script that processes the request.

To activate this script engine you must first **enable Nashorn support** in the 
`sling.properties` file of your Sling instance:

```
jre-1.8=jdk.nashorn.api.scripting;version\="0.0.0.1_008_JavaSE"
```
**attention**
> currently this implementation only works with java version "1.8.0_92" and higher

Once the bundle is active, you can try the engine with this minimal (and not very interesting) example:

First create a node with some content:

    curl -u admin:admin \
      -F"sling:resourceType=foo" \
	  -Ftitle="Hello ESX" \
	  -Ftext="Here's some example text" \
	  http://localhost:8080/apps/foo
	  
Then create an ESX script to render it:

    $ cat << EOF > /tmp/foo.esx
    var foo = {
      render: function () {
        var output  = "<h1>" + currentNode.properties.title + "</h1>";             
        output += currentNode.properties.text;
        return output;     
      }
    }  
    module.exports = foo;
    EOF
	
    $ curl -u admin:admin -T /tmp/foo.esx http://localhost:8080/apps/foo/foo.esx
   
    $ curl http://localhost:8080/apps/foo.html
    <h1>Hello ESX</h1>Here's some example text
  	  

An ESX file is a regular java script file. 

The NodeJS module resolution (https://nodejs.org/api/modules.html) is implemented to give access to the
rich collection of Node modules.

There's currently no priority handling of global modules.

The engine searches for scripts in the following order, if the regular module resolution does not find a module:
        - /apps/esx/node_modules
        - /apps/esx/esx_modules
        - /libs/esx/node_modules
        - /libs/esx/esx_modules

Additionally, ESX will try to resolve the folder *esx_modules* prior to *node_modules*.

### Special Loaders
Require Extensions are deprecated (see https://nodejs.org/api/globals.html#globals_require_extensions), therefore we have not implemented/used the extension loaders api and .bin extension cannot be used.

We have borrowed the requirejs loader plugin syntax instead (see http://requirejs.org/docs/api.html#text). Additionally to the standard JS loader following two loaders are existing:

- text (e.g. ```require("text!./templates/header.html"))```)
  - will return a javascript native string containing the content of the file
- resource  (e.g. ```require("resource!./content/blogposts)```)
  following will be exposed:
  - properties (resource valuemap)
  - path (jcr path)  
  - simpleResource (has getChildren method with resolved simpleresoruce in an array)
  - array with list of children (simpleResource)

- json loader  (e.g. ```require("./dict/en.json```)
  - the json as a whole will be exported as a javascript Object

## Installing Demo Application
Currently the demo application is bundles with the engine bundle. To install the engine with the demo application, follow this steps:
- switch to directory src/main/resources/libs/esx/demo
- run: npm install
- go back to package root directory
- run mvn clean install sling:install´

open http://localhost:8080/libs/esx/demo/content/demo.html

### Writing a module
You can actually follow the NODE JS description on https://nodejs.org/api/modules.html for more detailed explanation.

A module has access to following variables:
- __filename
- __dirname
- console (console.log is a log4j logger registered to the resolved module path and is not a 1:1 console.log implementation for now)
- properties (valuemap)
- simpleResource
- currentNode
 - currentNode.path
 - currentNode.resource
 - currentNode.properties
- sling (SlingScriptHelper)


# Example
## Caluclator Module
Path: /apps/demo/components/test/helper/calculator/index.js
```javascript
function calculate(a, b) {
  return a + b;
}
exports.math = calculate;
```

## Test components
Path: /apps/demo/components/test/test.esx
```javascript
var calculator = require("./helper/calculator");

exports.render = function () {
  return calculator.math(2,2);
}
```