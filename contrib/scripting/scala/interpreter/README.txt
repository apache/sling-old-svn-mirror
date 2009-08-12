Apache Sling Scala Interpreter

Scala interpreter with OSGi support. This module is used by the Scala
ScriptEngineFactory to implement support scripts stored in OSGi bundles
and JCR repositories. Due to issues with the Scala Compiler, this
module must be separate from the Scala ScriptEngineFactory module
but will be included in the latter.

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

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/scripting/scala/interpreter/

See the Subversion documentation for other source control features.

