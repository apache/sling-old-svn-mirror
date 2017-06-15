JspC Maven Plugin
=================

Compiles JSP scripts into class files and generates Declarative Service Descriptors to register the compiled JSPs as services.

See [Goals](plugin-info.html) for a list of supported goals.


## Overview

The Maven JspC Plugin provides a single goal `jspc` which is by default executed in the `compile` phase of the Maven build process. This goal takes all JSP source files from a configured location (`src/main/scripts` by default) and compiles them into classes in a configurable location (`target/jspc-plugin-generated` by default). In addition, for each compiled JSP a Declarative Services descriptor is generated and written to a descriptor file (`OSGI-INF/jspServiceComponents.xml` in the output location). This descriptor will then be read by the Service Component Runtime of the deployment OSGi framework to register all contained JSP as `javax.servlet.Servlet` services.


## Usage

To use the Maven JspC Plugin define the following elements in the `<plugins>` section of the POM:


    <?xml version="1.0" encoding="ISO-8859-1"?>
    <project>
        ....
        <build>
            ....
            <plugins>
                ....
                <plugin>
                    <groupId>org.apache.sling</groupId>
                    <artifactId>jspc-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>compile-jsp</id>
                            <goals>
                                <goal>jspc</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                ....
            <plugins>
            ....
        <build>
        ....
    <project>


## Notes

The generated JSP classes as well as the Declarative Services descriptor are automatically copied to the generated bundle jar file if the Maven Bundle Plugin (from the Apache Felix) project is used to build the project package.
