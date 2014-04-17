Apache Sling Crankstart
=======================

This is an early prototype of a different way of starting Sling,
based on a single text file that describes the Sling configuration.

For now, the crank start launcher reads a .crank.txt file, starts 
the OSGi framework and installs and starts bundles that it gets from
a Maven repository.

Note that this module is in contrib for now: it might be completely
changed, abandoned etc. - there's no guarantes that this will ever
become a supported Sling component.