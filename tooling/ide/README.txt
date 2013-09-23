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
- The Jackrabbit FileVault project, located at
  https://svn.apache.org/repos/asf/jackrabbit/commons/filevault/trunk

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
Development Environment and Maven features installed.

To start with, import all the projects in Eclipse as Maven projects. Eclipse
might prompt you to install an additional m2eclipse configurator for PDE
projects, as it's needed for bridging between Maven and PDE projects.

After the projects are imported, you need to set your target environment to
ensure that all dependencies are met and that you are working against the
project's declared baseline. To do that, open the following file in Eclipse

    target-definition/org.apache.sling.ide.target-definition.target

In the target editor which appears, click 'Set as Target Platform'. Once
the target platform is set up, you can create a new launch configuration.

  NOTE: if you don't see a target editor, but an XML editor, try right-clicking
  on the file and choosing File -> Open With -> Target Editor. If you don't
  see that option, you don't have PDE installed.

The final step is to create an Eclipse launch configuration which includes
all the Sling bundles. From the toolbar, access Run -> Run configurations...
and create a new Eclipse Application. By default, this configuration will
contain all the plugins from the target platform and the plugin projects that
you have in your workspace. If this setup is enough for you, you can save the
the launch configuration and run it. If you're looking to trim down the plugin
list for faster startup you can switch to the 'Plug-ins' tab and make the 
following changes:

    1. Select 'Launch with plug-ins selected below only'
    2. In the filter text box, type 'org.apache.sling.ide'
    3. Select org plugins which appear in the Workspace section.
    4. Clear the selection
    5. Click 'Add Required Plug-ins'
    6. Type 'application' in the filter text box
    7. Select the org.eclipse.ui.ide.application plug-in
    8. Click 'Validate Plug-ins'

Eclipse should report no errors, and you should be able to save the launch 
configuration and run it.