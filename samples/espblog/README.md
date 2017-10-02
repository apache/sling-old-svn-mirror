Sling ESP blog sample
---------------------

WHY THIS SAMPLE?
----------------
This demonstrates the use of ESP server-side javascript to implement a simple
blog, with file attachments.

A java service uses JCR observation to detect incoming files (either uploaded
via the blog forms, or copied via WebDAV), and creates thumbnails of files image
files, to demonstrate observation and OSGi SCR services.

PRECONDITIONS
-------------
As a first step, launch Sling.

The Sling Container can be launched by running the following command in the 
launchpad/builder/target directory:
  java -jar org.apache.sling.launchpad-<version>-standalone.jar
so if the current version is 7, the command should be:
  java -jar org.apache.sling.launchpad-7-standalone.jar

This launches sling on the default port: 8080.

Once Sling is running, deploy the 'path-based-rtp' bundle, which is required by
this demo:
  samples/path-based-rtp$ mvn clean install -P autoInstallBundle


INSTALL ESPBLOG
---------------
Install the espblog demo with the following command:
  mvn clean install -P autoInstallBundle
    
To verify that the bundle is correctly installed:

1) http://localhost:8080/apps/espblog/html.esp must return the html.esp script.

2) Log in by visiting http://localhost:8080/?sling:authRequestLogin=true,
   using username=admin and password=admin

3) The console at http://localhost:8080/system/console/bundles must list both 
   the following bundles as active:

     Apache Sling ESP blog sample 
         (org.apache.sling.samples.espblog)
     Apache Sling Sample Path Based Resource Type Provider
         (org.apache.sling.samples.path-based.rtp)


HOW TO TEST
-----------
The http://localhost:8080/content/espblog/*.html should display the "Sling ESP
blog sample" page, with the Home/Admin/New Post/... menu.

Create a new post using the "New Post" menu.

The post can include an attachment, if that's an image the included
ThumbnailGeneratorService generates thumbnails in different sizes, displayed
on the post's page. This happens asynchronously, so depending on your 
machine's speed the thumbnails might only appear after a few seconds.

Clicking on the Home menu item will show all the blog entries. 
The Admin menu item allows you to edit and delete entries.

Clicking on the RSS menu item shows an alternative rendering of the posts list.

The ESP scripts can be found under /apps/espblog in the repository.
