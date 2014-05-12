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


LAUNCH SLING
------------
The Sling Container can be launched by running the following command in the 
launchpad/builder/target directory:
  java -jar org.apache.sling.launchpad-<version>-standalone.jar
so if the current version is 7, the command should be:
  java -jar org.apache.sling.launchpad-7-standalone.jar


PREPARE SLING
-------------
Install i18n support by installing the org.apache.sling.i18n bundle:
  contrib/extensions/i18n $ mvn install -P autoInstallBundle

Remove the default implementation of the ACE and ACL scripts:
Navigate to the sling console:
  http://localhost:8080/.explorer.html

Click the 'login' button on the top-right of the screen and log in using 
  admin/admin
  
In the tree view, navigate to the /apps/sling/servlet/default/ace.html.esp node.
After clicking on the node, a 'delete this node' button appears. Click this button
to delete the node.
Also delete the /apps/sling/servlet/default/acl.html.esp in the same way.


HOW TO INSTALL
--------------
Build this bundle and install it in Sling:

  mvn install -P autoInstallBundle
    
To verify that the bundle is correctly installed:
  http://localhost:8080/libs/sling/accessmanager/page.html.esp 
must return the page.html.esp script.

This sample is best tested together with the usermanager-ui demo. Install this as well:
  samples/usermanager-ui $ mvn install -P autoInstallBundle
   

HOW TO TEST
-----------
Login as the admin user.

To test this functionality, install another sample applications, which is used as a
testing ground for the ACLs.
For example, install the slingshot sample:
  samples/slingshot $ mvn install -P autoInstallBundle 

Create a new user called 'test' using the usermanager at:
  http://localhost:8080/system/userManager.html

Using a different browser, confirm that the new 'test' user can access the slingshot
page:
  http://localhost:8080/slingshot/albums.html?sling:authRequestLogin=true

We can now restrict access to the slingshot/albums.html resource via the
access manager.

ACLs can be edited by visiting http://localhost:8080/[node_path_here].acl.html 
to view the access control list for a JCR node where [node_path_here] is 
replaced with the path of the node.

To restrict access to the userManager.html page for the 'test' user, in the original
browser navigate to:
  http://localhost:8080/slingshot/albums.acl.html
Make sure that the admin user is still logged in.

Create an entry for the 'test' user denying all privileges.

Restart the different browser/clear all its cookies that logged in the 'test' user 
previously. Then visit the following page as 'test' user again:
  http://localhost:8080/slingshot/albums.html?sling:authRequestLogin=true

To see that the page is now inaccessible to this user.

Use the links on the page to add/update/remove access control entries to the list
to provision privileges for users or groups.

