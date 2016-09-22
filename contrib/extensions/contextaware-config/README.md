Apache Sling Context-Aware Configuration
========================================

Apache Sling Context-Aware Configuration is work-in-progress.

These bundles provide a service API that can be used to get context-aware configurations. Context-aware configurations are configurations that are related to a content resource or a resource tree, e.g. a web site or a tenant site.

1. Java API
===========

To get and use configurations, the Java API must be used. Any using code must not make any assumptions on how the context-aware configurations are searched or stored!

1.1 Context-Aware Resources
===========================

The base concept are context-aware resources: for a given content resource, a named configuration resource can be get.
The service for getting the configuration resources is called the ConfigurationResourceResolver. This service has two methods:
- getting a named configuration resource
- getting all child resources of a named configuration resource.

For example to get a configuration resource for a content resource at /content/mysite/page1, you would write:

    Resource pageResource = resourceResolver.getResource("/content/mysite/page1");

    Resource configResource = configurationResourceResolver.getResource(pageResource, "configs", "site-configuration");

Or if you have several configuration resources of the same type and you need all of them:

    Collection<Resource> configResources = configurationResourceResolver.getResourceCollection(pageResource, "configs", socialmedia");

1.2 Context-Aware Configurations
================================

While context-aware resources give you pure resources and your application code can decide what to do with it,
the most common use case is some configuration. A configuration is usually described by an annotation class
(like Declarative Services does for component configurations). These are typed configuration objects
and the context-aware configuration support automatically converts resources into the wanted configuration type.

Context-aware configurations are built on top of context-aware resources. The same concept is used: configurations are
named and the service to get them is the ConfigurationResolver. It has a single method to get a ConfigurationBuilder
and this builder can then be used to get configurations:

    Resource pageResource = resourceResolver.getResource("/content/mysite/page1");

    ConfigurationBuilder builder = configurationResolver.get(pageResource);

    SiteConfiguration siteConfig = builder.as(SiteConfiguration.class);
    Collection<SocialMediaConfig> configs = builder.name("socialmedia").asCollection(SocialMediaConfig.class);

2. Default Implementation
=========================

The above API is completely agnostic to the way context-aware resources / configurations are searched and stored in the resource tree.
The following is how the default implementation in Apache Sling works:

2.1 Context Resolving
=====================

The first step is to find out to which context (e.g. site or tenant) a resource belongs. The mechanism starts at the given content resource
and looks for a property named "sling:config-ref". It traverses up the resource hierarchy until either root is reached or such a property is found.
If no property is found, there are the following fallbacks which will be searched in the given order : "/conf/global", "/apps/conf", and "/libs/conf".
These fallbacks are also used if a configuration resource is requested which does not exist in the given context.

For example with this content structure

    /content
      /mysite
        @sling:config-ref = /conf/tenants/piedpiper
          /page1
          /page2
          /sub
            @sling:config-ref = /conf/tenants/piedpiper/sub
              /pageA
              /pageB
                
The context for "/content/mysite/page1" is "/conf/tenants/piedpiper" while for "/content/mysite/sub/pageA" it is "/conf/tenants/piedpiper/sub"

For "/content/mysite/page1" the implementation searches at these paths for a configuration resource named "socialmedia/facebook":

    /conf/tenants/piedpiper/sling:configs/socialmedia/facebook
    /conf/global/sling:configs/socialmedia/facebook
    /apps/conf/sling:configs/socialmedia/facebook
    /libs/conf/sling:configs/socialmedia/facebook

The first resource found at these locations is used, if none is found, no context-aware resource will be returned.

For "/content/mysite/sub/pageA" the implementation searches at these paths for a configuration resource named "socialmedia/facebook"

    /conf/tenants/piedpiper/sub/sling:configs/socialmedia/facebook
    /conf/global/sling:configs/socialmedia/facebook
    /apps/conf/sling:configs/socialmedia/facebook
    /libs/conf/sling:configs/socialmedia/facebook

2.2 Content Model
=================

Configurations are stored under /conf

    /conf
        /global
            /sling:configs
                /socialmedia
                    /youtube
                      @enabled = false
                      @url = https://youtube.com
        /tenants
            /piedpiper
                /sling:configs
                    /socialmedia
                        /facebook 
                          @enabled = true
                          @url = https://facebook.com
