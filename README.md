Sling Classloader Leak Detector
===============================

This bundle provides support for tracing classloader leaks which occur due to
improper cleanup in bundles. Refer to [SLING-3359][1] for background details

The bundle registers a Felix Configuration Printer which dumps out a list of
suspected classloaders which are not getting garbage collected. it can be accessed
at http://localhost:8080/system/console/status-leakdetector

    Possible classloader leak detected
    Number of suspicious bundles - 1

    * org.apache.sling.sample.leakdetector.bad-bundle (0.0.1.SNAPSHOT) - Classloader Count [2]
         - Bundle Id - 204
         - Leaked classloaders
             - Identity HashCode - 4a273519, Creation time 31.01.2014 15:22:58.407

### JVM Arguments

 By default on Oracle JDK the classloaders and related classes from Permgen are
 not garbage collected by default. This bundle relies on Classloaders getting
 gced for it work. So to enable that pass on following arguments

     -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled

[1]: https://issues.apache.org/jira/browse/SLING-3359