Apache Sling SLF4J Implementation


=======================================
Welcome to Sling - SLF4J Implementation
=======================================

The "log" project implements the SLF4J SPI and thus operates as the backend
for the SLF4J API. The SLF4j API bundle must be installed along with this
bundle to provide full SLF4J logging support.
  
The Logging bundle should be installed as one of the first modules in
the OSGi framework and - provided the framework supports start levels -
be set to start at start level 1. This ensures the Logging bundle is
loaded as early as possible thus providing services to the framework
and preparing logging.

See the Apache Sling web site (http://sling.apache.org) for
documentation and other information. You are welcome to join the
Sling mailing lists (http://sling.apache.org/site/project-information.html)
to discuss this component and to use the Sling issue tracker
(http://issues.apache.org/jira/browse/SLING) to report issues or request
new features.

Apache Sling is a project of the Apache Software Foundation
(http://www.apache.org).

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

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/commons/log

See the Subversion documentation for other source control features.
