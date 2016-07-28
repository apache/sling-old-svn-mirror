Apache Sling Testing Pax Exam
=============================

**Test support for use with Pax Exam.**

* _Sling's Launchpad Karaf Features_ as `Option`﻿s for Pax Exam (without Karaf)

Getting Started
---------------

Configure the build artifact (*bundle*) to use in integration testing in `pom.xml`:

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <version>2.18.1</version>
        <executions>
          <execution>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <systemProperties>
            <property>
              <name>bundle.filename</name>
              <value>${basedir}/target/${project.build.finalName}.jar</value>
            </property>
          </systemProperties>
        </configuration>
      </plugin>

Create a test class (extend `TestSupport` to use helper methods and `Option`s) and provide a *Configuration* (`Option[]`) for Pax Exam:

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(), // from TestSupport
            launchpad(),
            // build artifact
            testBundle("bundle.filename"), // from TestSupport
            // testing
            junitBundles()
        };
    }

    protected Option launchpad() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        final int httpPort = findFreePort(); // from TestSupport
        return composite(
            slingLaunchpadOakTar(workingDirectory, httpPort), // from SlingOptions
            slingExtensionModels(), // from SlingOptions (for illustration)
            slingScriptingThymeleaf() // from SlingOptions (for illustration)
        );
    }
