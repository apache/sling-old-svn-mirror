Sling user management UI sample
---------------------

WHY THIS SAMPLE?
----------------
This demonstrates some sample UI for user self-regirstration and profile update.

HOW TO INSTALL
--------------
Build this bundle and install it in Sling.

For example, if Sling is running on port 8080 (which happens if you start 
the launchpad/testing module with "mvn jetty:run"), this will build and 
install it:

    mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8080/system/console
    
To verify that the bundle is correctly installed:

1) http://localhost:8080/apps/espblog/html.esp must return the html.esp 
   script.

2) The console at http://localhost:8080/system/console/bundles must 
    list the bundle named "Sling - ESP blog sample" as active.    

HOW TO TEST
-----------
Start by logging in at http://localhost:8080/?sling:authRequestLogin=true,
using username=admin and password=admin (or use the "login" link on the 
/index.html page).

Once logged in, /index.html should say "you are currently logged in as
user "admin" to workspace "default".

Then, http://localhost:8080/content/espblog/*.html should display the "Sling ESP
blog sample" page, with the Home/Admin/New Post/... menu.

If you get Sling's default HTML rendition instead ("Resource dumped by 
HtmlRendererServlet") that's probably because the "path-based-rtp" bundle
is not installed. Run the above "mvn...clean install" command in the
sling/samples/path-based-rtp folder to install it, and reload the page.

Create a new post using the "New Post" menu.

The post can include an attachment, if that's an image the included
ThumbnailGeneratorService generates thumbnails in different sizes, displayed
on the post's page. This happens asynchronously, so depending on your 
machine's speed the thumbnails might only appear after a few seconds.

The ESP scripts are found under /apps/espblog in the repository.
