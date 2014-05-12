Sling Slingbucks sample
-----------------------

WHY THIS SAMPLE?
----------------
This simple coffee-ordering app demonstrates how Sling promotes RESTful 
application design, and how much you can do with little code.

It was presented at ApacheCon North America 2010, slides can be found at
http://www.slideshare.net/bdelacretaz/restful-slingbdelacretaz2010

It is inspired by the excellent "How to GET a Cup of Coffee" article by 
Jim Webber, Savas Parastatidis & Ian Robinson at InfoQ: 
http://www.infoq.com/articles/webber-rest-workflow

HOW TO INSTALL
--------------
Build this bundle and install it in Sling. See
http://sling.apache.org/documentation/development/getting-and-building-sling.html
for how to do that.

For example, if Sling is running on port 8080 (which happens if you start 
the launchpad/testing module with 
"java -jar org.apache.sling.launchpad-<version>-standalone.jar"), this will build and 
install it:

    mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8080/system/console
    
The OSGi console at http://localhost:8080/system/console/bundles must then
list the "org.apache.sling.samples.slingbucks" bundle as active. 

HOW TO TEST
-----------
On a default Sling trunk instance you usually need to first disable the 
"Allow Anonymous Access" option at 
http://localhost:8080/system/console/configMgr/org.apache.sling.engine.impl.auth.SlingAuthenticator

Then, start at 

  http://localhost:8080/content/slingbucks/public/orders.html 
  
and follow the instructions.

The coffee shop employee's view of things is at 

  http://localhost:8080/content/slingbucks/private/confirmed.html

Confirmed orders appear there five seconds after they are last modified,
you need to refresh that page to see them.
   
To change the coffee options, you can play with the nodes under
/content/slingbucks/readonly/options/fields, the structure should be
self-explanatory.

The Sling explorer at

  http://localhost:8080/.explorer.html
  
can be used to modify those nodes, besides the usual POST requests that the
SlingPostServlet handles.   

      

