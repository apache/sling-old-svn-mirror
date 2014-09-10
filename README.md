Sling Replication
=================

This is the README for the Sling Replication module.

Overview
========

Configuration
=============

Resource providers
==================

One can configure a resource provider for a replication resource to give access to OSGI replication services.
Already configured ones are: ReplicationAgents, ReplicationPackageExporters and ReplicationPackageImporters.
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
