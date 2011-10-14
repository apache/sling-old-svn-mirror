Apache Sling Scala Scripting Engine

To enable Scala scripting for Sling, install the following bundles:

- org.apache.sling:org.apache.sling.scripting.scala.config
- org.apache.sling:org.apache.sling.scripting.scala.script
- org.apache.servicemix.bundles:org.apache.servicemix.bundles.scala-compiler
- org.apache.servicemix.bundles:org.apache.servicemix.bundles.scala-library

The former two bundles are part of Apache Sling, all bundles are available in the maven 
central repository.

There are two sample applications available: a simple hello world application 
and a forum application. See the README.txt files in the respective subdirectory
of contrib/scripting/scala/samples/. 

For more information on Scala for Sling see:

  https://cwiki.apache.org/confluence/display/SLING/Using+Scala+with+Sling
