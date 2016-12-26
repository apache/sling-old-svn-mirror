# Sample Multi-module project for Apache Sling

This is a project template for apache Sling based applications. It is built with 'Sling Multimodule Archetype' and is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

## Modules

The main parts of the project are:

* core: Java bundle containing the core functionality such as servlets.
* ui.apps: contains the /apps (and /etc) parts of the project, ie Javascript, CSS, fonts, clientlibs and sample html files. This module has in-built HTML5Boilerplate files.

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running Apache Sling instance you can build and package the whole project and deploy into Apache Sling with  

    mvn clean install -PautoInstallBundle
    
## Testing

You may verify if the bundles installed to Apache Sling at this url:  

    http://localhost:8080/system/console/bundles

Once open, search for the bundle name. 

You may also open the below url in a browser to check if the page installed properly:
     
     http://localhost:8080/apps/test/index.html
     
The required UI files (Javascript, CSS, images) will be located at:

     http://localhost:8080/bin/browser.html/etc/clientlibs/test
     
You may use the default Sling browser (http://localhost:8080/.explorer.html) or Composum (http://localhost:8080/bin/browser.html).


## Maven settings

The project comes with some settings under /core/pom.xml. You may change the Apache Sling's configs here. The default settings for Apache Sling are:

    http://localhost:8080
