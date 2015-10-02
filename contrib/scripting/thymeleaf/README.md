Apache Sling Scripting Thymeleaf
================================

scripting engine for [_Thymeleaf_](http://www.thymeleaf.org) templates

Features
--------

* simple non-caching `TemplateResolver` supporting `PatternSpec` configurations for all template modes (`HTML`, `XML`, `TEXT`, `JAVASCRIPT`, `CSS`) 
* `ResourceResolver` backed by Sling's `ResourceResolver`
* `MessageResolver` backed by `ResourceBundleProvider` from `org.apache.sling.i18n`
* `SlingDialect`

Installation
------------

For running Sling Scripting Thymeleaf with Sling's Launchpad some dependencies need to be resolved. This can be achieved by installing the following bundles:

    mvn:org.apache.sling/org.apache.sling.i18n/2.2.10
    mvn:org.javassist/javassist/3.18.2-GA
    mvn:commons-io/commons-io/2.4

There is a feature for Karaf:

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
    karaf@root()> feature:install sling-scripting-thymeleaf

relevant Thymeleaf issues
-------------------------

* [Create OSGi bundle](https://github.com/thymeleaf/thymeleaf/issues/32)
* [Remove initialize() steps in extension points](https://github.com/thymeleaf/thymeleaf/issues/54)
* [keep (custom) IContext accessible](https://github.com/thymeleaf/thymeleaf/issues/388)
