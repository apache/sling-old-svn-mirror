# Sling Dynamic Include

## Purpose

The purpose of the module presented here is to replace dynamic generated components (eg. current time or foreign exchange rates) with server-side include tag (eg. [SSI](http://httpd.apache.org/docs/current/howto/ssi.html) or [ESI](http://www.w3.org/TR/esi-lang)). Therefore the dispatcher is able to cache the whole page but dynamic components are generated and included with every request. Components to include are chosen in filter configuration using `resourceType` attribute.

When the filter intercepts request for a component with given `resourceType`, it'll return a server-side include tag (eg. `<!--#include virtual="/path/to/resource" -->` for Apache server). However the path is extended by new selector (`nocache` by default). This is required because filter has to know when to return actual content.

Components don't have to be modified in order to use this module (or even aware of its existence). It's servlet filter, installed as an OSGi bundle and it can be enabled, disabled or reconfigured without touching CQ installation.

## Prerequisites

* CQ / Apache Sling 2
* Maven 2.x, 3.x

## Installation

Add following dependency to your project:

    <dependency>
	    <groupId>org.apache.sling</groupId>
	    <artifactId>dynamic-include</artifactId>
	    <version>2.2.0</version>
    </dependency>

## Configuration

Filter is delivered as a standard OSGi bundle. SDI is configured via the configuration factory called *SDI Configuration*. Following properties are available:

* **Enabled** - enable SDI
* **Base path** - given SDI configuration will be enabled only for this
  path
* **Resource types** - which components should be replaced with tags
* **Include type** - type of include tag (Apache SSI, ESI or Javascript)
* **Add comment** - adds debug comment: `<!-- SDI include (path: %s, resourceType: %s) -->` to every replaced component
* **Filter selector** - selector used to get actual content
* **Component TTL** - time to live in seconds, set for rendered component (require Dispatcher 4.1.11+)
* **Required header** - SDI will be enabled only if the configured header is present in the request. By default it's `Server-Agent=Communique-Dispatcher` header, added by the AEM dispatcher. You may enter just the header name only or the name and the value split with `=`.
* **Ignore URL params** - SDI normally skips all requests containing any GET parameters. This option allows to set a list of parameters that should be ignored in the test. See the [Ignoring URL parameters](https://docs.adobe.com/docs/en/dispatcher/disp-config.html#Ignoring%20URL%20Parameters) section in the dispatcher documentation.
* **Include path rewriting** -- enable rewriting link (according to sling mappings) that is used for dynamic content including.

## Compatibility with components

Filter is incompatible with following types of component:

* components which handles POST requests or GET parameters,
* synthetic components which uses suffixes (because suffix is used to pass `requestType` of the synthetic resource).

If component do not generate HTML but eg. JS or binary data then remember to turn off *Comment* option in configuration.

## Enabling SSI in Apache & dispatcher

In order to enable SSI in Apache with dispatcher first enable `Include` mod (on Debian: `a2enmod include`). Then add `Includes` option to the `Options` directive in your virtual configuration host. After that find following lines in `dispatcher.conf` file:

        <IfModule dispatcher_module>
            SetHandler dispatcher-handler
        </IfModule>

and modify it:

        <IfModule dispatcher_module>
            SetHandler dispatcher-handler
        </IfModule>
        SetOutputFilter INCLUDES

After setting output filter open virtualhost configuration and add `Includes` option to `Options` directive:

        <Directory />
            Options FollowSymLinks Includes
            AllowOverride None
        </Directory>
        <Directory /var/www/>
            Options Indexes FollowSymLinks MultiViews Includes
            AllowOverride None
            Order allow,deny
            allow from all
        </Directory>

It's also a good idea to disable the caching for `.nocache.html` files in `dispatcher.any` config file. Just add:

        /disable-nocache
        {
            /glob "*.nocache.html*"
            /type "deny"
        }

at the end of the `/rules` section.

## Enabling TTL in dispatcher 4.1.11+
In order to enable TTL on Apache with dispatcher just add:

	/enableTTL "1"

to your dispatcher configuration.


## Enabling ESI in Varnish

Just add following lines at the beginning of the `vcl_fetch` section in `/etc/varnish/default.vcl` file:

        if(req.url ~ "\.nocache.html") {
            set beresp.ttl = 0s;
        } else if (req.url ~ "\.html") {
            set beresp.do_esi = true;
        }

It'll enable ESI includes in `.html` files and disable caching of the `.nocache.html` files.

## JavaScript Include

Dynamic Include Filter can also replace dynamic components with AJAX tags, so they are loaded by the browser. It's called JSI. In the current version jQuery framework is used. More attention is required if included component has some Javascript code. Eg. Geometrixx Carousel component won't work because it's initialization is done in page `<head>` section while the component itself is still not loaded.

## Plain and synthetic resources

There are two cases: the first involves including a component which is available at some URL, eg.

    /content/geometrixx/en/jcr:content/carousel.html

In this case, component is replaced with include tag, and `nocache` selector is added

    <!--#include virtual="/content/geometrixx/en/jcr:content/carousel.nocache.html" -->
    
If the filter gets request with selector it'll pass it (using `doChain`) further without taking any action.

![Plain include](https://raw.github.com/Cognifide/Sling-Dynamic-Include/master/src/main/doc/plain-include.png)

There are also components which are created from so-called synthetic resources. Synthetic resource have some resourceType and path, but they don't have any node is JCR repository. An example is

    /content/geometrixx/en/jcr:content/userinfo

component with `foundation/components/userinfo` resource type. These components return 404 error if you try to make a HTTP request. SDI recognizes these components and forms a different include URL for them in which resource type is added as a suffix, eg.:

    /content/geometrixx/en/jcr:content/userinfo.nocache.html/foundation/components/userinfo

If filter got such request, it'll try to emulate `<sling:include>` JSP tag and includes resource with given type and `nocache` selector:

    /content/geometrixx/en/jcr:content/userinfo.nocache.html

Selector is necessary, because otherwise filter would again replace component with a SSI tag.

# External resources

* [SDI presentation](http://www.pro-vision.de/content/medialib/pro-vision/production/adaptto/2012/adaptto2012-sling-dynamic-include-tomasz-rekaweki-pdf/_jcr_content/renditions/rendition.file/adaptto2012-sling-dynamic-include-tomasz-rekaweki.pdf) on [adaptTo() 2012](http://www.pro-vision.de/de/adaptto/adaptto-2012.html)
* [SDI blog](http://www.cognifide.com/blogs/cq/sling-dynamic-include/) post on the Cognifide website
* See the [Apache Sling website](http://sling.apache.org/) for the Sling reference documentation. Apache Sling, Apache and Sling are trademarks of the [Apache Software Foundation](http://apache.org).

# Release notes

## 2.2.0

\#17 Support for time-based (TTL) caching, Dispatcher 4.1.11+ required
