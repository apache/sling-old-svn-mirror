# Apache Sling Samples Webloader User Interface

This module is part of the [Apache Sling](https://sling.apache.org) project.

## Sling Webloader sample - user interface

This bundle provides a sample web user interface for the Webloader Service,
which id provided by the sibling "webloader.service" bundle.

The demo bundles can be deployed through Maven using the following commands:

Deploy the Webloader Service bundle running the following command in 
the samples/webloader/service directory:
  mvn install -P autoInstallBundle
then deploy the Webloader UI bundle by running the following command in 
the samples/webloader/ui directory:
  mvn install -P autoInstallBundle

When deployed, the webloader can be accessed by navigating to:
  http://localhost:8080/bin/sling/webloader.html 
This should display the "Sling Webloader" page, that gives access to 
the Webloader service.
