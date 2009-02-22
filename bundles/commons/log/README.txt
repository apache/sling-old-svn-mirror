Apache Sling OSGi LogService Implementation


=================================================
Welcome to Sling - OSGi LogService Implementation
=================================================

The "log" project defines an OSGi Bundle (Logging) which provides three
components:

  (1) An implementation of the Log Service Specification contained in
      the OSGi Service Platform Service Compendium book.
      
  (2) The SLF4J API package (org.slfj), Apache Commons Logging API
      package (org.apache.commons.logging) and Log4J package
      (org.apache.log4j) for clients to use.
      
  (3) Backend logging implementation of the SLF4J interfaces.
  
The Logging bundle should be installed as one of the first modules in
the OSGi framework and - provided the framework supports start levels -
be set to start at start level 1. This ensures the Logging bundle is
loaded as early as possible thus providing services to the framework
and preparing logging.

See the Apache Sling web site (http://incubator.apache.org/sling) for
documentation and other information. You are welcome to join the
Sling mailing lists (http://incubator.apache.org/sling/site/project-information.html)
to discuss this component and to use the Sling issue tracker
(http://issues.apache.org/jira/browse/SLING) to report issues or request
new features.

Apache Sling is a project of the Apache Software Foundation
(http://www.apache.org).

Disclaimer
==========
Apache Sling is an effort undergoing incubation at The Apache Software Foundation (ASF),
sponsored by the Apache Jackrabbit PMC. Incubation is required of all newly accepted
projects until a further review indicates that the infrastructure, communications,
and decision making process have stabilized in a manner consistent with other
successful ASF projects. While incubation status is not necessarily a reflection of
the completeness or stability of the code, it does indicate that the project has yet
to be fully endorsed by the ASF.

License (see also LICENSE)
==========================

Collective work: Copyright 2007 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.0.7 or later. We recommend to use the latest Maven version.

If you have Maven 2 installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/incubator/sling/trunk/commons/log

See the Subversion documentation for other source control features.
