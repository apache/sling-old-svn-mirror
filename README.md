Sling Replication
=================

This is the README for the Sling Replication module.

Overview
========

Replication agents
------------------

Each replication agent is an OSGi service and is resolved using a [Sling Resource Provider](#Resource_Providers) who locate it under `libs/sling/replication/services/agents`.
Replication agents can be triggered by sending `HTTP POST` requests to 

`http://$host:$port/libs/sling/replication/services/agents/$agentname` 

with HTTP parameters `action` and `path`.

Resource providers
==================

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

HTTP API
========

See the [wiki](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=38572805).

Replication agents configuration
--------------------------------

Replication agents configurations are proper OSGi configurations (backed by nodes of type `sling:OsgiConfig` in the repository), see [ReplicationAgentServiceFactory-publish.json](src/main/resources/SLING-CONTENT/libs/sling/replication/install.author/org.apache.sling.replication.agent.impl.CompactSimpleReplicationAgentFactory-publish.json).
Replication agents configuration include:
- Transport handlers: currently only basic HTTP is supported
- Package builders: currently only FileVault packages are supported
- Queue types: current implementations are:
-- In memory
-- Sling Job Handling based
- Endpoint: URI of the resource to replicate to
- Name: the name of the agent
- Authentication handlers: currently supported user/password and `nop` authentication handlers for HTTP transport
- Queue distribution: how items (packages) to be replicated are distributed to agent's queues

Replication agents' configurations can be retrieved via `HTTP GET`:

- `http -a admin:admin -v -f GET http://localhost:8080/system/replication/agent/publish/configuration`
- Replication agents' configurations can be updated via `HTTP POST`
- `http -a admin:admin -v -f POST http://localhost:8080/system/replication/agent/publish/configuration enpoint=newendpoint`
- Replication queues
- In Memory queue
- draft implementation using an in memory blocking queue together with a Sling scheduled processor which periodically fetches the first item of each queue and trigger a replication of such an item
- not suitable for production as it's currently not persisted and therefore restarting the bundle / platform would remove the queue
- Sling Job Handling based queue
- each queue addition triggers a Sling job creation
- requires the creation of a Sling JobConsumer for the replication agent when a replication agent is created / updated
- by default Sling queues for replication are
- ordered
- with max priority
- with infinite retries
- keeping job history
- Distribution of packages among queues:
- a replication agent can be configured to use a specific queue distribution mechanism
- the queue distribution strategy defines how packages are routed into agent's queues
- current distribution strategies:
- single: the agent has one only queue and all the items are routed there
- priority path: the agent can route a configurable set of paths (note that this configuration is global for the system, not per agent) to a dedicated priority queue while all the others go to the default queue
- error aware: the agent has one default queue for all the items, items failing for a configurable amount of times are either dropped or moved to an error queue 
- Direct replication use case
- User makes an `HTTP POST`request to `http://localhost:8080/system/replication/agent/publish` with parameter `X-replication-action=ACTIVATE` and `X-replication-path=/content`
- `ReplicationAgentServlet` servlet is triggered
- `ReplicationAgentServlet` servlet provides the resource to a `ReplicationAgent` by `ReplicationAgentResourceProvider`
- Replication agent resource provider gets the OSGi service with name `publish`
- `ReplicationAgent` executes the replication request (activate the node at path /content)
- `ReplicationAgent` get the status of the request and update the response accordingly
- `ReplicationAgentServlet` maps the agent response to an HTTP status accordingly

How to use it
--------------

- install the dependency bundles on all Sling instances
- install Sling Replication core on all Sling instances
- create some content on author (e.g. /content/sample1)
- activate 'content' by sending an HTTP POST on sender: 

```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/replication/services/agents/publish action=ADD path=/content```

- deactivate 'content' by sending an HTTP POST on sender:
 
```http -a admin:admin -v -f POST http://localhost:8080/libs/sling/replication/services/agents/publish action=DELETE path=/content```

Open Tasks
------------------------

- [] distributed configuration
- [] pushing to / pulling from JMS (pros: established pattern for producers/consumers problems, cons: other library / systems involved as a possible PoF)
- [] WebSocket support (pros: once established it's bidirectional and therefore also publish can directly push stuff to author)
- [] asynchronous import of packages (pros: parallel transport and import, cons: complex management of multiple queues on different publish instances)


