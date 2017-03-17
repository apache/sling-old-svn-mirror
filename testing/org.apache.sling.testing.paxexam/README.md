Apache Sling Testing Pax Exam
=============================

**Test support for use with Pax Exam.**

* _Sling's Karaf Features_ as `Option`s for Pax Exam (without Karaf)
* `TestSupport` with common helper methods and `Option`s

**Provided features:**

* run integration tests in a Sling instance in the same module (with the build artifact under test)
* use different versions in build (e.g. *minimal*) and tests (e.g. *latest*)
* overriding of versions

Getting Started
---------------

Add required dependencies:

    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.sling</groupId>
      <artifactId>org.apache.sling.testing.paxexam</artifactId>
      <version>${org.apache.sling.testing.paxexam.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.ops4j.pax.exam</groupId>
      <artifactId>pax-exam</artifactId>
      <version>${org.ops4j.pax.exam.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.ops4j.pax.exam</groupId>
      <artifactId>pax-exam-cm</artifactId>
      <version>${org.ops4j.pax.exam.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.ops4j.pax.exam</groupId>
      <artifactId>pax-exam-container-forked</artifactId>
      <version>${org.ops4j.pax.exam.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.ops4j.pax.exam</groupId>
      <artifactId>pax-exam-junit4</artifactId>
      <version>${org.ops4j.pax.exam.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.ops4j.pax.exam</groupId>
      <artifactId>pax-exam-link-mvn</artifactId>
      <version>${org.ops4j.pax.exam.version}</version>
      <scope>test</scope>
    </dependency>

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

Add `depends-maven-plugin` when using `TestSupport#baseConfiguration()` or `SlingVersionResolver#setVersionFromProject(…)` (see below):

      <plugin>
        <groupId>org.apache.servicemix.tooling</groupId>
        <artifactId>depends-maven-plugin</artifactId>
        <version>1.3.1</version>
        <executions>
          <execution>
            <goals>
              <goal>generate-depends-file</goal>
            </goals>
          </execution>
        </executions>
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

**Overriding (or adding) versions:**

    SlingOptions.versionResolver.setVersion(SLING_GROUP_ID, "org.apache.sling.jcr.oak.server", "1.1.0");

using a version from project (`pom.xml`):

    SlingOptions.versionResolver.setVersionFromProject(SLING_GROUP_ID, "org.apache.sling.jcr.oak.server");

**NOTE:** When using `slingLaunchpadOakTar()` or `slingLaunchpadOakMongo()` without _working directory_, _HTTP port_ and _Mongo URI_ make sure to clean up file system and database after each test and do not run tests in parallel to prevent interferences between tests.
