Apache Sling IDE Tooling

The IDE Tooling project produces a p2 update site which is installable into
a Eclipse runtime. The update site is located in the p2update/target/repository
directory.

Getting Started
===============

This component uses a Maven 3 (http://maven.apache.org/) build environment. 
It requires a Java 6 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.4 or later. We recommend to use the latest Maven version.

For the moment, this project depends on unreleased Maven artifacts which
are not part the Apache Snapshots Maven repository. As a prerequisite, you need
to install  the following projects' artifacts in the local Maven repository:

- The Sling Tooling Support Install bundle, located at
  http://svn.apache.org/viewvc/sling/trunk/tooling/support/install

For each of these locations, install the Maven artifacts in your local
Maven repository using the following command:

    mvn install

After installing them, you can can compile and package the p2 update site
using the following command:

    mvn package

See the Maven 3 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/tooling/ide

See the Subversion documentation for other source control features.


How to run the Sling IDE tools in a test Eclipse instance
---------------------------------------------------------

This howto assumes that you are running Eclipse Kepler with the Plug-In 
Development Environment and Maven features installed. You should have
previously built the projects using

    mvn package

to ensure that Maven artifacts which are not available on p2 update sites are
included in the workspace.

To start with, import all the projects in Eclipse as Maven projects. Eclipse
might prompt you to install an additional m2eclipse configurator for PDE
projects, as it's needed for bridging between Maven and PDE projects.

After the projects are imported, you need to set your target environment to
ensure that all dependencies are met and that you are working against the
project's declared baseline. To do that, open the following file in Eclipse

    target-definition/org.apache.sling.ide.target-definition-dev.target

In the target editor which appears, click 'Set as Target Platform'. Once
the target platform is set up, you can create a new launch configuration.

  NOTE: if you don't see a target editor, but an XML editor, try right-clicking
  on the file and choosing File -> Open With -> Target Editor. If you don't
  see that option, you don't have PDE installed.

Now you can use the 'Sling IDE Tooling' launch configuration which is present 
in the org.apache.sling.ide.target-definition project to launch a local instance
of Eclipse with Sling IDE Tooling plug-ins picked up from the local workspace.

How to generate a signed release
--------------------------------

The build can be configured to sign the generated jars with a code signing
certificates. This prevents unsigned content errors from appearing when
installing the plugins and reassures the user that the content comes from
a trusted source.

Please note that this is different from GPG signatures.

The following steps are needed to sign the generated jars.

1. Obtain a code signing certificate. At the moment the ASF does not provide
such a service, so you will have to obtain one yourself. One free possibility
is Certum [1]. Expect at least two weeks of processing time, so plan this
ahead of time.

2. Import the certificate chain into a local keystore. The best approach is to
install the certificate into a browser and ensure that the whole certificate
chain is present. For Certum that would by the Certum CA, the Certum Level 3
CA and the code signing certificate.  Backup the certificates from Fireox
and then import them into the keystore, with a command similar to

	keytool -importkeystore -destkeystore keystore_certum.jks -srckeystore \
		backup.p12 -srcstoretype pkcs12 

3. Insert properties controlling jarsigner behaviour in your settings.xml

	<settings>
	    <profiles>
	        <profile>
	            <id>sign</id>        
	
	            <properties>
	                <jarsigner.alias>certum-codesigning</jarsigner.alias>
	                <jarsigner.storepass>changeit</jarsigner.storepass>
	                <jarsigner.tsa>http://time.certum.pl/</jarsigner.tsa>
	                <!-- needed since we mix packages between projects -->
	                <skipTests>true</skipTests>
	                <jarsigner.keystore>/home/users/keystore_certum.jks</jarsigner.keystore>
	            </properties>
	        </profile>
	    </profiles>
	</settings>

At this point you can launch a build using

	mvn clean package -Psign

All jars will be signed, and should install without any warnings.
[1]: https://www.certum.eu/certum/cert,offer_en_open_source_cs.xml 