Apache Sling Launchpad Karaf

A repository of features and a startup handler that allow an easy deployment
of Apache Sling on Apache Karaf [1]. See [2] for details.

[1] http://karaf.apache.org
[2] http://karaf.apache.org/manual/latest/users-guide/provisioning.html

Getting Started
===============

1) Start Apache Karaf. See details in:

	http://karaf.apache.org/manual/latest/quick-start.html

2) Add the Apache Sling features repository and install:

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
    karaf@root()> feature:install sling-launchpad-jackrabbit

3) Install Launchpad content and Explorer:

    karaf@root()> feature:install sling-launchpad-content
    karaf@root()> feature:install sling-extension-explorer

4) Browse to:

	http://localhost:8181/
