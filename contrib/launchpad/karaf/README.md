Apache Sling Launchpad Karaf
============================

A repository of features and a startup handler that allow an easy deployment
of Apache Sling on [Apache Karaf](http://karaf.apache.org). See [Provisioning](http://karaf.apache.org/manual/latest/users-guide/provisioning.html) for details.

Getting Started
---------------

1) [Start Apache Karaf](http://karaf.apache.org/manual/latest/quick-start.html).

2) Add the Apache Sling features repository and install a `sling-launchpad-*` (`oak-tar` | `oak-mongo`) feature:

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
    karaf@root()> feature:install sling-launchpad-oak-tar

3) Install Launchpad content and Explorer:

    karaf@root()> feature:install sling-launchpad-content
    karaf@root()> feature:install sling-extension-explorer

4) Browse to http://localhost:8181/⁠.


KAR - Karaf Archive
-------------------

    karaf@root()> kar:install mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/kar
