Apache Sling OSGi LogService Implementation


=================================================
Welcome to Sling - OSGi LogService Implementation
=================================================

The "logservice" project implements the OSGi LogService implementation on top
of the SLF4J logging API. This bundle should be installed as one of the first
modules in the OSGi framework along with the SLF4J API and implementation and
- provided the framework supports start levels - be set to start at start
level 1. This ensures the Logging bundle is loaded as early as possible thus
providing services to the framework and preparing logging.
