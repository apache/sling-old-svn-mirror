Sling Fling Sample
==================

This is a sample using _Sling Models_, _Sling Query_ and _Sling Scripting Thymeleaf_.

* [Sling Models](http://sling.apache.org/documentation/bundles/models.html)
* [Sling Query](https://github.com/Cognifide/Sling-Query)
* [Sling Scripting Thymeleaf](http://sling.apache.org/documentation/bundles/scripting/scripting-thymeleaf.html) ([Thymeleaf](http://www.thymeleaf.org/), [Sling i18n](http://sling.apache.org/documentation/bundles/internationalization-support-i18n.html))

Additional features used in this sample:

* [Authentication](http://www.thymeleaf.org/)¹: see `org.apache.sling.samples.fling.internal.Activator`
* [Bundle Resources](http://sling.apache.org/documentation/bundles/bundle-resources-extensions-bundleresource.html): see `Sling-Bundle-Resources` in `pom.xml`
* [Content Loading](http://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html): see `Sling-Initial-Content` in `pom.xml`
* [Maven Sling Plugin](http://sling.apache.org/documentation/development/sling.html): see `maven-sling-plugin` in `pom.xml`

There are two profiles to install this sample with _Maven Sling Plugin_ into a running local _Sling_ instance for convenience:

When using _[Sling Launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html)_ listening on port `8080` with default admin credentials run:

    mvn clean install -P launchpad@localhost

When using _[Apache Karaf](http://karaf.apache.org)_ with _[Sling Karaf Launchpad](https://github.com/apache/sling/tree/trunk/contrib/launchpad/karaf)_ listening on port `8181` with default admin credentials run:

    mvn clean install -P karaf@localhost

This will install initial content under `/apps/fling`, `/content/fling` and `/etc/i18n/org.apache.sling.samples.fling`.

Browse to [http://localhost:8080/fling.html](http://localhost:8080/fling.html) or [http://localhost:8181/fling.html](http://localhost:8181/fling.html).

To install the required bundles/features on _Apache Karaf_ run the commands below:

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
    karaf@root()> feature:install sling-launchpad-oak-tar
    karaf@root()> feature:install sling-extension-models
    karaf@root()> feature:install sling-extension-query
    karaf@root()> feature:install sling-scripting-thymeleaf
    karaf@root()> feature:install sling-auth-form﻿²

For a more detailed user profile add some properties to an user³:

    curl -u admin -FfirstName=Foo -FlastName=Bar http://localhost:8181/system/userManager/user/admin.update.json

1. Pages under _Authentication_ (`/fling/auth`) need authentication.
2. optional feature
3. requires bundle `org.apache.sling.jcr.jackrabbit.usermanager` or feature `sling-jcr-jackrabbit-security` to be installed
