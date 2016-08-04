# Sling Content Distribution

This is the README for the Sling Content Distribution module.

## Overview

The Sling Content Distribution module main goal is allowing distribution of content (Sling resources) among different Sling 
instances. The term "distribution" here means the ability of picking one or more resources on a certain Sling instance in order 
to copy and persist them onto another Sling instance. The Sling Content Distribution module is able to distribute content 
by:

 - "pushing" from Sling instance A to Sling instance B
 - "pulling" from Sling instance B to Sling instance A
 - "synchronizing" Sling instances A and B via a (third) coordinating instance C

### Bundles

The Sling Content Distribution module consists of the following bundles:

 - org.apache.sling.distribution.api: this is where the APIs are defined
 - org.apache.sling.distribution.core: this is where the basic infrastructure for distributing content is implemented
 - org.apache.sling.distribution.extensions: this is where some additional (optional) extensions for distributing content are implemented
 - org.apache.sling.distribution.sample: this is a set of sample configurations and implementations for demo purpose 
 - org.apache.sling.distribution.it: this is the integration testing suite
 
## Design

The Sling Content Distribution module main design goals resume in being: _Reliable_, _extensible_ and _simple_.

Reliability means that the system should be able to keep working also in presence of failures regarding I/O, network, etc.
An example of such problems is when pushing content from instance A to instance B fails because B is unreachable: in such 
 scenarios instance A should be able to keep pushing (pulling, etc.) content to other instances seamlessly. Another example
 is when delivery of a certain content (package) fails too many times the distribution module should be able to either drop 
 it or move it into a different "bucket" of failed items.
Extensibility means that the Sling Content Distribution module provides a set of APIs for distributing resources where each
component coming into place during the distribution lifecycle can be extended or totally replaced.
Simplicity means that this module should be able to accomplish its tasks by providing clear and easy to use APIs together 
with smart but not overly complicated or "hacky" implementations (see Bertrand's talk ["Simple software is hard"](http://events.linuxfoundation.org/events/apachecon-europe/program/schedule)).

A distribution _request_ represents the need of aggregating some resources and to copy them from / to another Sling instance.
Such requests are handled by _agents_ that are the main entry point for working with the distribution module.
Each agent distributes content from one or more sources to one or more targets, such distribution can be triggered by:

 - "pushing" the content to the (remote) target instances 
 - "pulling" content from the (remote) source instances
 - "coordinating" instances, that is they are used to synchronize multiple instances by having them as both sources and targets

An _agent_ is capable of handling a certain distribution _request_ by creating one or more _packages_ of resources out of it 
from the source(s), dispatching such _packages_ to one or more _queues_ and of processing such queued _packages_ by persisting 
them into the target instance(s).

The process of creating one or more packages is called _exporting_ as such operation may either happen locally to the agent 
(the "push" scenario) or remotely (the "pull" scenario).

The process of persisting one or more packages is called _importing_ as such operation may either happen locally (the "pull" 
scenario) or remotely (the "push" scenario).

In order to properly handle large number of _requests_ against the same _agent_ each of them is provided with _queues_ 
where the exported _packages_ are sent, the _agent_ takes then care to process such a _queue_ in order to _import_ each 
_package_. 
 

### Distribution agents configuration

Distribution agents configurations are proper OSGi configurations (backed by nodes of type `sling:OsgiConfig` in the repository).



There are specialized factories for each supported scenario:
- "forward" agents, see [ForwardDistributionAgentFactory-publish.json](sample/src/main/resources/SLING-CONTENT/libs/sling/distribution/install.author/publish/org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory-publish.json).
- "reverse" agents, see [ReverseDistributionAgentFactory-publish-reverse.json](sample/src/main/resources/SLING-CONTENT/libs/sling/distribution/install.author/publish-reverse/org.apache.sling.distribution.agent.impl.ReverseDistributionAgentFactory-publish-reverse.json).
- "sync" agents, see [SyncDistributionAgentFactory-pubsync.json](sample/src/main/resources/SLING-CONTENT/libs/sling/distribution/install.author/pubsync/org.apache.sling.distribution.agent.impl.SyncDistributionAgentFactory-pubsync.json).
- "queue" agents, see [QueueDistributionAgentFactory-reverse.json](sample/src/main/resources/SLING-CONTENT/libs/sling/distribution/install.publish/reverse/org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory-reverse.json).


For example a "forward" agent can be defined specifying
- The name of the agent (name property)
- The sub service name used to access content and build packages (serviceName property)
- The endpoints where the packages are to be imported (packageImporter.endpoints property)



The sample package contains endpoints for exposing configuration for distribution agents.
The _DistributionConfigurationResourceProviderFactory_ is used to expose agent configurations as resources.

  {
      "jcr:primaryType": "sling:OsgiConfig",
      "provider.roots": [ "/libs/sling/distribution/settings/agents" ],
      "kind" : "agent"
  }


Distribution agents' configurations can be retrieved via `HTTP GET`:

- `http -a admin:admin -v -f GET http://localhost:8080/libs/sling/distribution/settings/agents/{agentName}`



### Distribution agents services

Each distribution agent is an OSGi service and is resolved using a [Sling Resource Provider](#Resource_Providers) who locate it under `libs/sling/distribution/services/agents`.

The _DistributionConfigurationResourceProviderFactory_ allows one to configure HTTP endpoints to access distribution OSGI configurations.
The sample package contains endpoints for exposing distribution agents.
The _DistributionServiceResourceProviderFactory_ is used to expose agent services as resources.

 {
     "jcr:primaryType": "sling:OsgiConfig",
     "provider.roots": [ "/libs/sling/distribution/services/agents" ],
     "kind" : "agent"
 }


Distribution agents can be triggered by sending `HTTP POST` requests to

`http://$host:$port/libs/sling/distribution/services/agents/{agentName}`

with HTTP parameters `action` and `path`.

### Distribution queues

#### In Memory queue

That's a draft implementation using an in memory blocking queue together with a Sling scheduled processor which periodically fetches the first item of each queue and trigger a distribution of such an item.
It's not suitable for production as it's currently not persisted and therefore restarting the bundle / platform would not keep the queue together with its items.

#### Sling Job Handling based queue

That's a queue implementation based on the queues and jobs provided by Sling Event bundle.
Each item addition to a queue triggers the creation of a Sling job which will handle the processing of that item in the queue.
By default Sling queues for distribution have the following options:

- ordered
- with max priority
- with infinite retries
- keeping job history

### Distribution of packages among queues

Each distribution agent uses a specific queue distribution mechanism, specified via a 'queue distribution strategy', which defines how packages are routed into agent queues.
The currently available distribution strategies are

- single: the agent has one only queue and all the items are routed there
- priority path: the agent can route a configurable set of paths (note that this configuration is currently global for the system, not per agent) to a dedicated priority queue while all the others go to the default queue
- error aware: the agent has one default queue for all the items, items failing for a configurable amount of times are either dropped or moved to an error queue (depending on configuration)

 
## Usecases

### Forward distribution

In order to configure the "forward" distribution workflow, that transfers content from an author instance to a publish instance:
- configure a remote importer on publish
- configure a "forward" agent on author pointing to the url of the importer on publish

Send `HTTP POST`request to `http://localhost:8080/libs/sling/distribution/services/agents/publish` with parameters `action=ADD` and `path=/content`

- create/update content
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/publish action=ADD path=/content/sample1```
- delete content
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/publish action=DELETE path=/content/sample1```


### Reverse distribution

In order to configure the "reverse" distribution workflow, that transfers content from a publish instance to an author instance:
- configure a queue agent on publish to hold the packages that need to be distributed to author
- configure a remote exporter on publish that exports package from the queue agent
- configure a "reverse" agent on author pointing to the url of the exporter on publish

Send `HTTP POST`request to `http://localhost:8080/libs/sling/distribution/services/agents/publish-reverse` with parameters `action=PULL`


- create/update content

```http -a admin:admin -v -f POST http://localhost:8081/libs/sling/distribution/services/agents/reverse action=ADD path=/content/sample1```
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/publish-reverse action=PULL```

### Sync distribution


In order to configure the "sync" distribution workflow, that transfers content from two publish instances via an author instance:
- configure a remote exporter on each publish instance
- configure a remote importer on each publish instance
- configure a "sync" agent on author pointing to the urls of the exporter and importers on publish

Send `HTTP POST`request to `http://localhost:8080/libs/sling/distribution/services/agents/pubsync` with parameters `action=PULL`


- create/update content

```http -a admin:admin -v -f POST http://localhost:8081/libs/sling/distribution/services/agents/reverse-pubsync action=ADD path=/content/sample1```
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/pubsync action=PULL```



### Installation

- install the dependency bundles on all Sling instances
- install Sling Distribution api, core, samples on all Sling instances

## HTTP API

### API Requirements
We need to expose APIs for configuring, commanding and monitoring distribution agents.

- Configuration API should allow:
 - CRUD operations for agent configs
- Command API (eventually issued to multiple agents at once) should allow:
 - to trigger a distribution request on a specific agent
 - to explicitly create and export a package
 - to explicitly import a formerly created package
- Monitoring API should allow:
 - inspection to internal queues of distribution agents
 - inspection of commands history
 
### API endpoints 

#### Configuration API
- Create config:  - POST _/libs/sling/distribution/settings/agents_
- Read config - GET _/libs/sling/distribution/settings/agents/{agentName}_
- Update config - PUT _/libs/sling/distribution/settings/agents/{agentName}_
- Delete config - DELETE _/libs/sling/distribution/settings/agents/{agentName}_

#### Command API
- Distribute - POST _/libs/sling/distribution/services/agents/{agentName}_
- Import package - POST _/libs/sling/distribution/services/importers/{importerName}_
- Export package - POST _/libs/sling/distribution/services/exporters/{exporterName}_

#### Monitoring API
- Distribution history - GET _/libs/sling/distribution/services/agents/{agentName}/log_
- Agent queue inspection  - GET _/libs/sling/distribution/services/agents/{agentName}/queues_

## Java API

There is a single entry point in triggering a distribution workflow, via [Distributor](api/src/main/java/org/apache/sling/distribution/Distributor.java) API.

```Distributor.distribute(agentName, resourceResolver, distributionRequest)```

## Extensions

The _org.apache.sling.distribution.extensions_ bundle contains the following extensions:

### Apache Avro distribution serialization
A _DistributionContentSerializer_ based on [Apache Avro](http://avro.apache.org).

### Kryo distribution serialization
A _DistributionContentSerializer_ based on [Kryo](http://github.com/EsotericSoftware/kryo).

## Ideas for future developments

- distributed configuration
- pushing to / pulling from JMS (pros: established pattern for producers/consumers problems, cons: other library / systems involved as a possible PoF)
- WebSocket support (pros: once established it's bidirectional and therefore also publish can directly push stuff to author)
- asynchronous import of packages (pros: parallel transport and import, cons: complex management of multiple queues on different publish instances)
