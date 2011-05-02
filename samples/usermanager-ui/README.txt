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

For example, if Sling is running on port 8888 (which happens if you start 
the launchpad/builder module with "mvn launchpad:run"), this will build and 
install it:

    mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8888/system/console
    
To verify that the bundle is correctly installed:

1) http://localhost:8888/libs/sling/usermgmt/page.html.esp must return the page.html.esp 
   script.

2) The console at http://localhost:8888/system/console/bundles must 
    list the bundle named "Apache Sling User Manager UI sample" as active.    

HOW TO TEST
-----------
Login to sling as the admin user.

Open http://localhost:8888/system/userManager.html

Use the links in the left sidebar to get to the pages that enable you to 
find, update or create users/groups.

