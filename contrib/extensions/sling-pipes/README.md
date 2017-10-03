# Apache Sling Pipes

This module is part of the [Apache Sling](https://sling.apache.org) project.

tool for doing extract - transform - load operations through a resource tree configuration

often one-shot data transformations need sample code to be written & executed. This tiny tool set intends to provide ability to do such transformations with proven & reusable blocks called pipes, streaming resources from one to the other.

## What is a pipe

```
        getOutputBinding       
              ^                
              |                
getInput  +---+---+   getOutput
          |       |            
     +----> Pipe  +---->       
          |       |            
          +-------+            
```
A sling pipe is essentially a sling resource stream:
* it provides an output as a sling resource iterator
* it gets its input either from a configured path, either, if its chained (see container pipes below), from another pipe's output
* each pipe can have additional dynamic inputs using other's bindings, and outputting its own bindings
 
At the moment, there are 3 types of pipes to consider:
* "reader" pipes, that will just output a set of resource depending on the input
* "writer" pipes, that write to the repository, depending on configuration and output
* "container" pipes, that contains pipes, and whose job is to chain their execution : input is the input of their first pipe,
 output is the output of the last pipe it contains.
 
A `Plumber` osgi service is provided to help getting & executing pipes.

## Registered Pipes
a pipe configuration is a jcr node, with:
* `sling:resourceType` property, which must be a pipe type registered by the plumber 
* `name` property, that will be used in bindings as an id, and will be the key for the output bindings (default value being a value map of the current output resource). Note that the node name will be used in case no name is provided.
* `path` property, if configured, will override upstream's pipe output as an input.
* `expr` property, expression through which the pipe will execute (depending on the type) 
* `additionalBinding` is a node you can add to set "global" bindings (property=value) in pipe execution
* `additionalScripts` is a multi value property to declare scripts that can be reused in expressions
* `conf` optional child node that contains addition configuration of the pipe (depending on the type)

### readers

#### Base pipe
rather dummy pipe, outputs what is in input (so what is configured in path). Handy for doing some test mostly, and giving basic functionalities to others that inherit from it
* `sling:resourceType` is `slingPipes/base`

#### SlingQuery Pipe
executes $(getInput()).children(expression)
* `sling:resourceType` is `slingPipes/slingQuery`
* `expr` mandatory property, contains slingQuery expression through which getInput()'s children will be computed to getOutput()

#### JsonPipe
feeds bindings with remote json
* `sling:resourceType` is `slingPipes/json`
* `expr` mandatory property contains url that will be called, the json be sent to the output bindings, getOutput = getInput.
An empty url or a failing url will block the pipe at that given place.

#### MultiPropertyPipe
iterates through values of input multi value property and write them to bindings 
* `sling:resourceType` is `slingPipes/multiProperty`
* `path` should be the path of a mv property

#### XPathPipe
retrieve resources resulting of an xpath query
* `sling:resourceType` is `slingPipes/xpath`
* `expr` should be a valid xpath query

### JsonPipe
feeds bindings with remote json
* `sling:resourceType` is `slingPipes/json`
* `expr` mandatory property contains url that will be called, the json be sent to the output bindings, getOutput = getInput.
An empty url or a failing url will block the pipe at that given place.

#### AuthorizablePipe
retrieve authorizable resource corresponding to the id passed in expression, or if not found (or void expression),
from the input path, output the found authorizable's resource
* `sling:resourceType` is `slingPipes/authorizable`
* `expr` should be an authorizable id, or void (but then input should be an authorizable)
* `autoCreateGroup` (boolean) if autorizable id is here, but the authorizable not present, then create group with given id (in that case, considered as a write pipe)
* `addMembers` (stringified json array) if authorizable is a group, add instanciated members to it (in that case, considered as a write pipe)
* `addToGroup` (expression) add found authorizable to instanciated group (in that case, considered as a write pipe)
* `bindMembers` (boolean) if found authorizable is a group, bind the members (in that case, considered as a write pipe)

#### ParentPipe
outputs the parent resource of input resource
* `sling:resourceType` is `slingPipes/parent`

#### FilterPipe
outputs the input resource if its matches its configuration
* `sling:resourceType` is `slingPipes/filter`
* `conf` node tree that will be tested against the current input of the pipe, each `/conf/sub@prop=value` will triggers a test
on `./sub@prop` property of the current input, testing if its value matches `value` regex. If the special `slingPipesFilter_noChildren=${true}`
property is there with the value instantiated as a true boolean, then filter will pass if corresponding node has no children.

### containers
#### Container Pipe
assemble a sequence of pipes
* `sling:resourceType` is `slingPipes/container`
* `conf` node contains child pipes' configurations, that will be configured in the order they are found (note you should use sling:OrderedFolder)

#### ReferencePipe
execute the pipe referenced in path property
* `sling:resourceType` is `slingPipes/reference`
* `path` path of the referenced pipe

### writers

#### Write Pipe
writes given properties to current input
* `sling:resourceType` is `slingPipes/slingQuery`
* `conf` node tree that will be copied to the current input of the pipe, each node's properties 
names and value will be written to the input resource. Input resource will be outputed. 

### MovePipe
JCR move of current input to target path (can be a node or a property)
* `sling:resourceType` is `slingPipes/mv`
* `expr` target path, note that parent path must exists

#### RemovePipe
removes the input resource, returns the parent, regardless of the resource being a node, or
a property
* `sling:resourceType` is `slingPipes/rm`
* `conf` node tree that will be used to filter relative properties & subtrees to the current resource to remove.
A subnode is considered to be removed if it has no property configured, nore any child.

#### PathPipe
get or create path given in expression
* `sling:resourceType` is `slingPipes/path`
* `nodeType` node type of the intermediate nodes to create
* `autosave` should save at each creation (will make things slow, but sometimes you don't have choice)

## Making configuration dynamic with pipe bindings
in order to make things interesting, most of the configurations are javascript template strings, hence valid js expressions reusing bindings (from configuration, or other pipes).

Following configurations are evaluated:
* `path`
* `expr`
* name/value of each property of some pipes (write, remove)

you can use name of previous pipes in the pipe container, or the special binding `path`, where `path.previousPipe` 
is the path of the current resource of previous pipe named `previousPipe`

global bindings can be set at pipe execution, external scripts can be added to the execution as well (see pipe
 configurations)

## How to execute a pipe
for now it's possible to execute Pipes through GET (read) or POST (read/write) commands:

### Request Path
- either you'll need to create a slingPipes/plumber resource, say `etc/pipes` and then to execute
```
curl -u admin:admin -F "path=/etc/pipes/mySamplePipe" http://localhost:8080/etc/pipes.json
```
- either you execute the request directly on the pipe Path, e.g.
```
curl -u admin:admin http://localhost:8080/etc/pipes/mySamplePipe.json
```
which will return you the path of the pipes that have been through the output of the configured pipe.

### Request Parameter `binding`

you can add as `bindings` parameter a json object of global bindings you want to add for the execution of the pipe
 
e.g. 

```
 curl -u admin:admin -F "path=/etc/pipes/test" -F "bindings={testBinding:'foo'}" http://localhost:4502/etc/pipes.json
```

will returns something like

```
["/one/output/resource", "another/one"]
```

### Request Parameter `writer`

you can add as `writer` parameter a json object as a pattern to the result you want to have. The values of the json
object are expressions and can reuse each pipe's subpipe binding. 
Note 
this works only if the pipe called is a container
pipe.

e.g.

```
curl -u admin:admin http://localhost:4502/etc/pipes/users.json?writer={"user":"${user.fullName}"}
```

will returns something similar to
 
```
[{'user':'John Smith','path':'/home/users/q/q123jk1UAZS'},{'user':'John Doe','path':'/home/users/q/q153jk1UAZS'}]
```

### Request Parameter `dryRun`
if parameter dryRun is set to true, and the executed pipe is supposed to modify content, it will log (at best it can) the change it *would* have done, without doing anything

## sample configurations 

### slingQuery | write
this pipe parse all profile nodes, and 
```
{
  "sling:resourceType":"slingPipes/container",
  "name":"Dummy User prefix Sample",
  "jcr:description":"prefix all full names of profile with "Mr" or "Ms" depending on gender",
  "conf":{
    "profile": {
        "sling:resourceType":"slingPipes/slingQuery",
        "expr":"nt:unstructured#profile",
        "path":"/home/users"
    },
    "writeFullName": {       
        "sling:resourceType":"slingPipes/write",
        "conf": {
            "fullName":"${(profile.gender === 'female' ? 'Ms ' + profile.fullName : 'Mr ' + profile.fullName)}",
            "generatedBy":"slingPipes"
        }
    }
  }
}
```

### slingQuery | multiProperty | authorizable | write
```
{
  "jcr:primaryType": "sling:Folder",
  "jcr:description": "move badge<->user relation ship from badge MV property to a user MV property"
  "name": "badges",
  "sling:resourceType": "slingPipes/container",
  "conf": {
    "jcr:primaryType": "sling:OrderedFolder",
    "badge": {
      "jcr:primaryType": "sling:Folder",
      "jcr:description": "outputs all badge component resources",
      "expr": "[sling:resourceType=myApp/components/badge]",
      "path": "/etc/badges/badges-admin/jcr:content",
      "sling:resourceType": "slingPipes/slingQuery"
      },
    "profile": {
      "jcr:primaryType": "sling:Folder",
      "jcr:description": "retrieve all user ids from a mv property",
      "path": "${path.badge}/profiles",
      "sling:resourceType": "slingPipes/multiProperty"
      },
    "user": {
      "jcr:primaryType": "sling:OrderedFolder",
      "jcr:description": "outputs user resource",
      "expr": "profile",
      "sling:resourceType": "slingPipes/authorizable"
      },
    "write": {
      "jcr:primaryType": "sling:OrderedFolder",
      "jcr:descritption": "patches the badge path to the badges property of the user profile"
      "path": "${path.user}/profile",
      "sling:resourceType": "slingPipes/write",
      "conf": {
        "jcr:primaryType": "nt:unstructured",
        "badges": "+[${path.badge}]"
        }
      }
    }
  }
```

### xpath | json | write
this use case is for completing repository profiles with external system's data (that has an json api)
```
{
  "jcr:primaryType": "nt:unstructured",
  "jcr:description": "this pipe retrieves json info from an external system and writes them to the user profile, uses moment.js, it
  distributes modified resources using publish distribution agent",
  "sling:resourceType": "slingPipes/container",
  "distribution.agent": "publish",
  "additionalScripts": "/etc/source/moment.js",
  "conf": {
    "jcr:primaryType": "sling:OrderedFolder",
    "profile": {
      "jcr:primaryType": "sling:OrderedFolder",
      "expr": "/jcr:root/home/users//element(profile,nt:unstructured)[@uid]",
      "jcr:description": "query all user profile nodes",
      "sling:resourceType": "slingPipes/xpath"
      },
    "json": {
      "jcr:primaryType": "sling:OrderedFolder",
      "expr": "${(profile.uid ? 'https://my.external.system.corp.com/profiles/' + profile.uid.substr(0,2) + '/' + profile.uid + '.json' : '')",
      "jcr:description": "retrieves json information relative to the given profile, if the uid is not found, expr is empty: the pipe will do nothing",
      "sling:resourceType": "slingPipes/json"
      },
    "write": {
      "jcr:primaryType": "sling:OrderedFolder",
      "path": "path.profile",
      "jcr:description": "write json information to the profile node",
      "sling:resourceType": "slingPipes/write",
      "conf": {
        "jcr:primaryType": "sling:OrderedFolder",
        "jcr:createdBy": "admin",
        "background": "${json.opt('background')}",
        "about": "${json.opt('about')}",
        "jcr:created": "Fri Jul 03 2015 15:32:22 GMT+0200",
        "birthday": "${(json.opt('birthday') ? moment(json.opt('birthday'), \"MMMM DD\").toDate() : '')}",
        "mobile": "${json.opt('mobile')}"
        }
      }
    }
  }
```

### xpath | parent | rm
```
{
  "jcr:primaryType": "nt:unstructured",
  "jcr:description": "this pipe removes user with bad property in their profile",
  "sling:resourceType": "slingPipes/container",
  "conf": {
    "jcr:primaryType": "sling:OrderedFolder",
    "profile": {
      "jcr:primaryType": "sling:OrderedFolder",
      "expr": "/jcr:root/home/users//element(profile,nt:unstructured)[@bad]",
      "jcr:description": "query all user profile nodes with bad properties",
      "sling:resourceType": "slingPipes/xpath"
      },
    "parent": {
      "jcr:primaryType": "sling:OrderedFolder",
      "jcr:description": "get the parent node (user node)",
      "sling:resourceType": "slingPipes/parent"
      },
    "rm": {
      "jcr:primaryType": "sling:OrderedFolder",
      "jcr:description": "remove it",
      "sling:resourceType": "slingPipes/rm",
      }
   }
}
```

some other samples are in https://github.com/npeltier/sling-pipes/tree/master/src/test/

# Compatibility
For running this tool on a sling instance you need:
- java 8 (Nashorn is used for expression)
- slingQuery (3.0.0) (used in SlingQueryPipe)
- jackrabbit api (2.7.5+) (used in AuthorizablePipe)
