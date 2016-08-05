Apache Sling Context Aware Configuration
========================================

Apache Sling Context Aware Configuration is work-in-progress.

These bundles provide a service API that can be used to get context aware configurations. Context aware configurations are configurations that are related to a content resource or a resource tree, e.g. a web site or a tenant site.

1. Java API
===========

To get and use configurations, the Java API must be used. Any using code must not make any assumptions on how the context aware configurations are searched or stored!

1.1 Context Aware Resources
===========================

The base concept are context aware resources: for a given content resource, a named configuration resource can be get.
The service for getting the configuration resources is called the ConfigurationResourceResolver. This service has two methods:
- getting a named configuration resource
- getting all child resources of a named configuration resource.

For example to get a configuration resource for a content resource at /content/mysite/page1, you would write:

Resource pageResource = resourceResolver.getResource("/content/mysite/page1");

Resource configResource = configurationResourceResolver.getResource(pageResource, "site-configuration");

Or if you have several configuration resources of the same type and you need all of them:

Collection<Resource> configResources = configurationResourceResolver.getResourceCollection(pageResource, "socialmedia");

1.2 Context Aware Configurations
================================

While context aware resources give you pure resources and your application code can decide what to do with it,
the most common use case is some configuration. A configuration is usually described by a DTO like class, interface
or annotation (like Declarative Services does for component configurations). These are typed configuration objects
and the context aware configuration support automatically converts resources into the wanted configuration type.

Context aware configurations are built on top of context aware resources. The same concept is used: configurations are
named and the service to get them is the ConfigurationResolver. It has a single method to get a ConfigurationBuilder
and this builder can then be used to get configurations:

Resource pageResource = resourceResolver.getResource("/content/mysite/page1");

ConfigurationBuilder builder = configurationResolver.get(pageResource);

SiteConfiguration siteConfig = builder.as(SiteConfiguration.class);
Collection<SocialMediaConfig> configs = builder.name("socialmedia").asCollection(SocialMediaConfig.class);

2. Default Implementation
=========================

2.1 Content Model
=================

The default configuration stores the configs under /config:

    /config/
          global/
                 sling:configs/
                          socialmedia/
                                      youtube
                                             @enabled = false  <-  "enabled" is a property of the youtube resource, denoted by the @ prefix
                                             @url = https://youtube.com <-  "url" is a property of the youtube resource, denoted by the @ prefix
          tenants/   
                  piedpiper/
                            sling:configs/
                                     socialmedia/
                                                 facebook 
                                                      @enabled = true  <-  "enabled" is a property of the facebook resource, denoted by the @ prefix
                                                      @url = https://facebook.com <-  "url" is a property of the facebook resource, denoted by the @ prefix


2.2 Context Rules
=================

Context is defined by the content resource. If a context aware resource is searched for a content resource, the resource hierarchy is traversed from the content resource up to the root. Once a "sling:config" property with an absolute resource path is found, the context aware resource is searched at that path.
If no such property is found on the content resource or any of its parents, the global configuration at /config/global is used.

The Resource /content/hooli defines a confuration context of /config/tentants/piedpiper by setting the property sling:conf to reference that configuration. Configuration defined in that location will overlay the configuration tree in /config/global

    /content/hooli/@sling:conf = "/config/tentants/piedpiper"

If now a named configuration "socialmedia/facebook" is searched, the resolver will first look for a resource at /config/tenants/piedpiper/sling:configs/socialmedia/facebook,
followed by /config/global/sling:configs/socialmedia/facebook, /apps/sling:configs/socialmedia/facebook, and /libs/sling:configs/socialmedia/facebook. The first resource found at these locations is used.

