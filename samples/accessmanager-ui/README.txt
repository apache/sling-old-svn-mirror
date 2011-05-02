Sling access manager UI sample
---------------------

WHY THIS SAMPLE?
----------------
This demonstrates some sample UI for viewing/updating the access control
lists of JCR nodes.

PRE-REQUISITES
--------------
This sample uses the i18n support provided by the org.apache.sling.i18n bundle,
so that bundle must be installed into your sling instance first. 

If using the sling launchpad, you will need to remove the following scripts from your
jackrabbit repository since this sample bundle provides a different implementation of
those scripts.
    /apps/sling/servlet/default/ace.html.esp
    /apps/sling/servlet/default/acl.html.esp

HOW TO INSTALL
--------------
Build this bundle and install it in Sling.

For example, if Sling is running on port 8888 (which happens if you start 
the launchpad/builder module with "mvn launchpad:run"), this will build and 
install it:

    mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8888/system/console
    
To verify that the bundle is correctly installed:

1) http://localhost:8888/libs/sling/accessmanager/page.html.esp must return the page.html.esp 
   script.

2) The console at http://localhost:8888/system/console/bundles must 
    list the bundle named "Apache Sling Access Manager UI sample" as active.    

HOW TO TEST
-----------
Login as the admin user.

Open http://localhost:8888/[node_path_here].acl.html to view the access control list for
a JCR node where [node_path_here] is replaced with the path of the node. 

Use the links on the page to add/update/remove access control entries to the list
to provision privileges for users or groups.

