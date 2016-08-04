Apache Sling Context Aware Configuration
========================================

Apache Sling Context Aware Configuration is work-in-progress.

These bundles provide a service API that can be used to get context aware configurations. Context aware configurations are configurations that are related to a content resource or a resource tree, e.g. a web site or a tenant site.

1. Java API
===========

To get and use configurations, the Java API must be used. Any using code must not make any assumptions on how the context aware configurations are searched or stored!

TODO - Explain API

1.1 Context Aware Resources
===========================

The base concept are context aware resources: for a given content resource, a named configuration resource can be get.

1.2 Context Aware Configurations
================================

Context aware configurations build on top of context aware resources. The process for getting a context aware configuration is to get the named context aware resource and adapt it to the application specific configuration object.

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

