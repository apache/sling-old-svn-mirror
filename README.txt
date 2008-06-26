Apache Sling
============

Bringing Back the Fun!

Apache Sling (currently in incubation) is a web framework that uses a Java
Content Repository, such as Apache Jackrabbit, to store and manage content.

Sling applications use either scripts or Java servlets, selected based on
simple name conventions, to process HTTP requests in a RESTful way.

The embedded Apache Felix OSGi framework and console provide a dynamic
runtime environment, where  code and content bundles can be loaded, unloaded
and reconfigured at runtime.

As the first web framework dedicated to JSR-170 Java Content Repositories,
Sling makes it very simple to implement simple applications, while providing
an enterprise-level framework for more complex applications.

See http://incubator.apache.org/sling for more information.

Getting started
---------------

You need a Java 5 (or higher) JDK and Maven 2 (http://maven.apache.org/,
version 2.0.7 or higher) to build Sling.

Sling depends on artifacts and plugins available only in the Incubator Maven
repository at Apache. You can make this repository available to Maven by
adding the following profile in your ~/.m2/settings.xml configuration file:

    <?xml version="1.0"?>
    <settings>
      <profiles>
        <profile>
          <id>apache-incubation</id>
          <activation>
            <activeByDefault>true</activeByDefault>
          </activation>
          <repositories>
            <repository>
              <id>apache.incubating</id>
              <name>Apache Incubating Repository</name>
              <url>http://people.apache.org/repo/m2-incubating-repository</url>
            </repository>
          </repositories>
          <pluginRepositories>
            <pluginRepository>
              <id>apache.incubating.plugins</id>
              <name>Apache Incubating Plugin Repository</name>
              <url>http://people.apache.org/repo/m2-incubating-repository</url>
            </pluginRepository>
          </pluginRepositories>
        </profile>
      </profiles>
    </settings>

Once you have everything in place, run

    MAVEN_OPTS=-Xmx256m mvn clean install

in this directory. This will build, test and install the Sling modules
in your local Maven repository.

Some modules might not be listed in the pom.xml found in this directory,
those won't be built by the above command. If you need one of these 
modules, run "mvn clean install" in the directory that contains its
pom.xml file.

To get started with Sling, start the launchpad/launchpad-webapp module,
see README.txt in that module's directory.

Disclaimer
==========

Apache Sling is an effort undergoing incubation at The Apache Software
Foundation (ASF), sponsored by the Apache Jackrabbit PMC. Incubation is
required of all newly accepted projects until a further review indicates that
the infrastructure, communications, and decision making process have
stabilized in a manner consistent with other successful ASF projects. While
incubation status is not necessarily a reflection of the completeness or
stability of the code, it does indicate that the project has yet to be fully
endorsed by the ASF.
