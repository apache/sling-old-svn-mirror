Apache Sling Web Application
============================

This project builds a standard Servlet 2.3 Web Application which may be
deployed into any Servlet 2.3 or higher compliant servlet container.

Configuration is taken from the sling.properties file included in the
web application's WEB-INF folder and the servlet's init-params. One of the
settings must be the "sling.home" property to designate the location of
the files created by the sling application. This property is currently
set as servlet init-param with the name "sling.home".


Testing the project
-------------------

This project may be tested using the Jetty plugin for Maven which runs the
Jetty server and installs the Sling Web Application into the container.

Before starting the Jetty server, the project must be packaged because the
bundle install/copy plugin will copy bundles to be installed initially into
the WAR target folder whereas the WAR plugin copies the web app source files
from src/main/webapp folder only at package time. But both kinds of files
must be present to run this project for testing purposes.

By default Jetty is started listening on port 8080. This may be changed by
setting the "http.port" property when calling Maven, e.g.:

     $ mvn -Dhttp.port=32768 package jetty:run
