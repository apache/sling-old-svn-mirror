# Sling Replication

This is the README for the Sling Replication module.

## Overview

### Replication agents

Each replication agent is an OSGi service and is resolved using a [Sling Resource Provider](#Resource_Providers) who locate it under `libs/sling/replication/services/agents`.

Replication agents can be triggered by sending `HTTP POST` requests to 

`http://$host:$port/libs/sling/replication/services/agents/$agentname` 

with HTTP parameters `action` and `path`.

### Resource providers

One can configure a resource provider for a replication resource to give access to OSGI replication services.
Already configured ones are: _ReplicationAgents_, _ReplicationPackageExporters_ and _ReplicationPackageImporters_.
Here is an example of the configuration for exposing _ReplicationAgent_ services as resources:

    {
         "jcr:primaryType" : "sling:OsgiConfig",
    
         "name" : "replicationAgents",
    
         "provider.roots" : [ "/libs/sling/replication/services/agents" ],
    
         "serviceType" : "org.apache.sling.replication.agent.ReplicationAgent",
    
         "resourceProperties" : [
    
             "sling:resourceType=sling/replication/service/agent",
    
             "sling:resourceSuperType=sling/replication/service",
    
             "name={name}",
    
             "queue/sling:resourceType=replication/agent/queue"
    
         ]
    
     }

When configuring such a provider you specify the service interface _ReplicationAgent_, the url and some properties one 
wants that resource to have like resourceType so basically this config governs _/libs/sling/replication/services/agents_.
Sub resources to an agent can also be added, as the _queue_ for example and add properties to it by specifying _queue/propertyName = propertyValue_.
That's the mechanism to expose services as resources.

### Replication agents configuration

Replication agents configurations are proper OSGi configurations (backed by nodes of type `sling:OsgiConfig` in the repository), see [CompactSimpleReplicationAgentServiceFactory-publish.json](src/main/resources/SLING-CONTENT/libs/sling/replication/install.author/org.apache.sling.replication.agent.impl.CompactSimpleReplicationAgentFactory-publish.json).

Replication agents configuration include:

- Name: the name of the agent
- Package exporter: algorithm for retrieving a replication package
- Package importer: algorithm for installing a replication package
- Queue provider: current implementations are:
-- In memory
-- Sling Job Handling based
- Queue distribution: how items to be replicated are distributed to agent's queues
- Triggers: triggers able to handle replication requests

Replication agents' configurations can be retrieved via `HTTP GET`:

- `http -a admin:admin -v -f GET http://localhost:8080/libs/sling/replication/settings/agents/publish`

### Replication queues

#### In Memory queue

That's a draft implementation using an in memory blocking queue together with a Sling scheduled processor which periodically fetches the first item of each queue and trigger a replication of such an item.
It's not suitable for production as it's currently not persisted and therefore restarting the bundle / platform would not keep the queue together with its items.

#### Sling Job Handling based queue

That's a queue implementation based on the queues and jobs provided by Sling Event bundle.
Each item addition to a queue triggers the creation of a Sling job which will handle the processing of that item in the queue.
By default Sling queues for replication have the following options:

- ordered
- with max priority
- with infinite retries
- keeping job history

### Distribution of packages among queues

Each replication agent uses a specific queue distribution mechanism, specified via a 'queue distribution strategy', which defines how packages are routed into agent queues.
The currently available distribution strategies are

- single: the agent has one only queue and all the items are routed there
- priority path: the agent can route a configurable set of paths (note that this configuration is currently global for the system, not per agent) to a dedicated priority queue while all the others go to the default queue
- error aware: the agent has one default queue for all the items, items failing for a configurable amount of times are either dropped or moved to an error queue (depending on configuration)

 
## Usecases

### Forward replication (PUSH)

User/client makes an `HTTP POST`request to `http://localhost:8080/libs/sling/replication/services/agents/publish` with parameters `action=ADD` and `path=/content`

- `ReplicationAgentServlet` servlet is triggered
- `ReplicationAgentServlet` servlet adapts the resource to a `ReplicationAgent` via a registered `OsgiPropertiesResourceProviderFactory` 
- `ReplicationAgent` executes the replication request (add the resource at path /content)
- `ReplicationAgent` get the status of the request and update the response accordingly
- `ReplicationAgentServlet` maps the agent response to an HTTP response accordingly

## HOWTOs

### Installation

- install the dependency bundles on all Sling instances
- install Sling Replication core on all Sling instances

### Push resources

#### Push changes

- create/update some content on author (e.g. /content/sample1)
- add 'content/sample1' by sending an HTTP POST on sender instance: 

```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/replication/services/agents/publish action=ADD path=/content/sample1```

#### Push deletions

- delete 'content' by sending an HTTP POST on sender instance:
 
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/replication/services/agents/publish action=DELETE path=/content/sample1```

# Open Tasks

- distributed configuration
- pushing to / pulling from JMS (pros: established pattern for producers/consumers problems, cons: other library / systems involved as a possible PoF)
- WebSocket support (pros: once established it's bidirectional and therefore also publish can directly push stuff to author)
- asynchronous import of packages (pros: parallel transport and import, cons: complex management of multiple queues on different publish instances)

### HTTP API

#### API Requirements
We need to expose APIs for configuring, commanding and monitoring replication agents.

- Configuration API should allow:
 - CRUD operations for agent configs
- Command API (eventually issued to multiple agents at once) should allow:
 - to trigger a replication request on a specific agent
 - to explicitly create and export a package
 - to explicitly import a formerly created package
- Monitoring API should allow:
 - inspection to internal queues of replication agents
 - inspection of commands history
 
#### API endpoints 

##### Configuration API
- Create config:  - POST _/libs/sling/replication/settings/agents_
- Read config - GET _/libs/sling/replication/settings/agents/{config identifier}_
- Update config - PUT _/libs/sling/replication/settings/agents/{config identifier}_
- Delete config - DELETE _/libs/sling/replication/settings/agents/{config identifier}_ or POST with :operation=delete

##### Command API
- Replicate - POST _/libs/sling/replication/services/agents/{agentName}_
- Import package - POST _/libs/sling/replication/services/importers/{importerName}_
- Export package - POST _/libs/sling/replication/services/exporters/{exporterName}_

##### Monitoring API
- Replication history - GET _/libs/sling/replication/services/agents/{agentName}/history_
- Import package history - GET _/libs/sling/replication/services/importers/{importerName}/history_
- Export package history - GET _/libs/sling/replication/services/exporters/{exporterName}/history
- Agent queue inspection  - GET _/libs/sling/replication/services/agents/{agentName}/queue_ or _{agentName}.queue_

#### API Implementation 
Configuration API should be implemented using SlingPostServlet and a full sync should be implemented between config location and ConfigurationAdmin. 
Command API can be implemented using SlingPostServlet if we can live with the asyncronous status check. 
It is important to have in mind that the replication commands are asynchronous by default as there might be some queues that are used the different parts of a replication request. 
Hence finding out if a replication request was completely finished will require inspecting the history most of the times.
Monitoring API should be implemented with custom servlets at least for internal queues as they display live info which would be tedious to sync in the repo

##### Flatten vs granular 
The commands and monitoring APIs can be implemented using either a flat or a granular approach (or both)
###### Flatten design
It permits sending requests to multiple agents which might be desirable at least for replicate requests
It is easier to implement as it requires no hierarchy of resources
###### Granular design
It is resource oriented
One needs to implement either a ResourceProvider or a JCR syncronization for agents and queues in order to represent the hierarchy. There is no consensus on which is best.
 
##### Sample payloads
TODO