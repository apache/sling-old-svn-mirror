Apache Sling Launchpad Content

The Launchpad Content bundle provides some initial and sample content
as well as links to the Online resources at http://sling.apache.org


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

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/launchpad/content

See the Subversion documentation for other source control features.

Working with the Front-end
==========================

This package uses SCSS / SASS to build the styles for the content. It is not necessary
to build the SCSS for every build unless you wish to edit the styles. To do so, you
will need NPM and Gulp. Please use the following steps to install (if needed) and 
build the front-end.

    1. Install NPM (https://www.npmjs.com/get-npm)
    2. Install Gulp (https://github.com/gulpjs/gulp/blob/master/docs/getting-started.md)
    3. Install dependencies in src/main/resources/content/etc/clientlibs/launchpad/src
        $ npm install
    4. Run Gulp
        $ gulp

The gulp process will compile the grid system, fonts, and index SCSS files into 
a minified index.css in the dist folder.