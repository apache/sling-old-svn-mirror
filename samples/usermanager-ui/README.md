Sling user manager UI sample
---------------------

WHY THIS SAMPLE?
----------------
This demonstrates some sample UI for user/group discovery, creation and update.

PRE-REQUISITES
--------------
This sample uses the i18n support provided by the org.apache.sling.i18n bundle,
so that bundle must be installed into your sling instance first. 

HOW TO INSTALL
--------------
Build this bundle and install it in Sling.

For example, if Sling is running on port 8080 (which happens if you start 
the launchpad/testing module with 
  "java -jar target/org.apache.sling.launchpad-<version>-standalone.jar"), 
this will build and install it:

    mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8080/system/console
    
To verify that the bundle is correctly installed:

1) http://localhost:8080/libs/sling/usermgmt/page.html.esp must return the page.html.esp 
   script.

2) The console at http://localhost:8080/system/console/bundles must 
    list the bundle named "Apache Sling User Manager UI sample" as active.    

HOW TO TEST
-----------
Login to sling as the admin user.

Open http://localhost:8080/system/userManager.html

Use the links in the left sidebar to get to the pages that enable you to 
find, update or create users/groups.

SEE ALSO
--------
This demo can also be used in combination with the accessmanager-ui demo. For more 
information see there.