# Sling Content Distribution

This is the README for the Sling Content Distribution module.

## Overview

### Distribution agents

Each distribution agent is an OSGi service and is resolved using a [Sling Resource Provider](#Resource_Providers) who locate it under `libs/sling/distribution/services/agents`.

Distribution agents can be triggered by sending `HTTP POST` requests to 

`http://$host:$port/libs/sling/distribution/services/agents/$agentname` 

with HTTP parameters `action` and `path`.

### Resource providers

One can configure a resource provider for a distribution resource to give access to OSGI distribution services.
Already configured ones are: _DistributionAgents_, _DistributionPackageExporters_ and _DistributionPackageImporters_.
Here is an example of the configuration for exposing _DistributionAgent_ services as resources:

    {
         "jcr:primaryType" : "sling:OsgiConfig",
    
         "name" : "distributionAgents",
    
         "provider.roots" : [ "/libs/sling/distribution/services/agents" ],
    
         "serviceType" : "org.apache.sling.distribution.agent.DistributionAgent",
    
         "resourceProperties" : [
    
             "sling:resourceType=sling/distribution/service/agent",
    
             "sling:resourceSuperType=sling/distribution/service",
    
             "name={name}",
    
             "queue/sling:resourceType=distribution/agent/queue"
    
         ]
    
     }

When configuring such a provider you specify the service interface _DistributionAgent_, the url and some properties one 
wants that resource to have like resourceType so basically this config governs _/libs/sling/distribution/services/agents_.
Sub resources to an agent can also be added, as the _queue_ for example and add properties to it by specifying _queue/propertyName = propertyValue_.
That's the mechanism to expose services as resources.

### Distribution agents configuration

Distribution agents configurations are proper OSGi configurations (backed by nodes of type `sling:OsgiConfig` in the repository), see [CompactSimpleDistributionAgentServiceFactory-publish.json](src/main/resources/SLING-CONTENT/libs/sling/distribution/install.author/org.apache.sling.distribution.agent.impl.CompactSimpleDistributionAgentFactory-publish.json).

Distribution agents configuration include:

- Name: the name of the agent
- Package exporter: algorithm for retrieving a distribution package
- Package importer: algorithm for installing a distribution package
- Queue provider: current implementations are:
-- In memory
-- Sling Job Handling based
- Queue distribution: how items to be distributed are distributed to agent's queues
- Triggers: triggers able to handle distribution requests

Distribution agents' configurations can be retrieved via `HTTP GET`:

- `http -a admin:admin -v -f GET http://localhost:8080/libs/sling/distribution/settings/agents/publish`

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

### Forward distribution (PUSH)

User/client makes an `HTTP POST`request to `http://localhost:8080/libs/sling/distribution/services/agents/publish` with parameters `action=ADD` and `path=/content`

- `DistributionAgentServlet` servlet is triggered
- `DistributionAgentServlet` servlet adapts the resource to a `DistributionAgent` via a registered `OsgiPropertiesResourceProviderFactory` 
- `DistributionAgent` executes the distribution request (add the resource at path /content)
- `DistributionAgent` get the status of the request and update the response accordingly
- `DistributionAgentServlet` maps the agent response to an HTTP response accordingly

## HOWTOs

### Installation

- install the dependency bundles on all Sling instances
- install Sling Distribution core on all Sling instances

### Push resources

#### Push changes

- create/update some content on author (e.g. /content/sample1)
- add 'content/sample1' by sending an HTTP POST on sender instance: 

```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/publish action=ADD path=/content/sample1```

#### Push deletions

- delete 'content' by sending an HTTP POST on sender instance:
 
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/distribution/services/agents/publish action=DELETE path=/content/sample1```

# Open Tasks

- distributed configuration
- pushing to / pulling from JMS (pros: established pattern for producers/consumers problems, cons: other library / systems involved as a possible PoF)
- WebSocket support (pros: once established it's bidirectional and therefore also publish can directly push stuff to author)
- asynchronous import of packages (pros: parallel transport and import, cons: complex management of multiple queues on different publish instances)

### HTTP API

#### API Requirements
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
 
#### API endpoints 

##### Configuration API
- Create config:  - POST _/libs/sling/distribution/settings/agents_
- Read config - GET _/libs/sling/distribution/settings/agents/{config identifier}_
- Update config - PUT _/libs/sling/distribution/settings/agents/{config identifier}_
- Delete config - DELETE _/libs/sling/distribution/settings/agents/{config identifier}_ or POST with :operation=delete

##### Command API
- Distribute - POST _/libs/sling/distribution/services/agents/{agentName}_
- Import package - POST _/libs/sling/distribution/services/importers/{importerName}_
- Export package - POST _/libs/sling/distribution/services/exporters/{exporterName}_

##### Monitoring API
- Distribution history - GET _/libs/sling/distribution/services/agents/{agentName}/history_ (not implemented yet)
- Import package history - GET _/libs/sling/distribution/services/importers/{importerName}/history_ (not implemented yet)
- Export package history - GET _/libs/sling/distribution/services/exporters/{exporterName}/history_ (not implemented yet)
- Agent queue inspection  - GET _/libs/sling/distribution/services/agents/{agentName}/queue_ or _{agentName}.queue_

#### API Implementation 
TODO

##### Sample payloads
TODO