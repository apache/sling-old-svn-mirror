Apache Sling Karaf
==================

* **Apache Sling Karaf Features**: a features repository for easy deployment of [Apache Sling](https://sling.apache.org) on [Apache Karaf](https://karaf.apache.org) (see [Provisioning](https://karaf.apache.org/manual/latest/#_provisioning) for details)
* **Apache Sling Karaf Distribution**: a [distribution](https://karaf.apache.org/manual/latest/#_custom_distributions) of [Apache Sling](https://sling.apache.org) based on [Apache Karaf](https://karaf.apache.org) (Sling's features and artifacts in a single archive)


Getting Started
---------------

For Karaf < 4.1.0 you need to configure _Felix File Install_ in `etc/config.properties` to include `*.config` files (not necessary when using _Sling's Karaf Distribution_):

  `felix.fileinstall.filter = .*\\.(cfg|config)`

1) [Start Apache Karaf](https://karaf.apache.org/manual/latest/#_quick_start) or _Sling's Karaf Distribution_.

2) Add the Apache Sling features repository (not necessary when using _Sling's Karaf Distribution_):

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.karaf-features/0.1.1-SNAPSHOT/xml/features

3) Install a `sling-launchpad-*` (`oak-tar` | `oak-mongo`) feature:

    karaf@root()> feature:install sling-launchpad-oak-tar

4) Install Launchpad content and Explorer:

    karaf@root()> feature:install sling-launchpad-content
    karaf@root()> feature:install sling-extension-explorer

5) Browse to [http://localhost:8181/](http://localhost:8181/⁠).
