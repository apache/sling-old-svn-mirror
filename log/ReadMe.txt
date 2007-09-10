OSGi LogService Implementation
==============================

The "log" project defines an OSGi Bundle (Logging) which provides three
components:

  (1) An implementation of the Log Service Specification contained in
      the OSGi Service Platform Service Compendium book.
      
  (2) The SLF4J API package (org.slfj) and Apache Commons Logging API
      package (org.apache.commons.logging) for clients to use.
      
  (3) Backend logging to Log4J with support to configure Log4J.
  
The Logging bundle should be installed as one of the first modules in
the OSGi framework and - provided the framework supports start levels -
be set to start at start level 1. This ensures the Logging bundle is
loaded as early as possible thus providing services to the framework
and preparing logging.
