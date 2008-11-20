----------------------
Sling javashell sample
----------------------

-------------------------------------------------------------------
SECURITY WARNING:
javashell lets user execute arbitrary code with NO LIMITS, and
is only meant as a teaching/demo tool. Use at your own risk, or
do not use if you don't understand the issues.

Just try "System.exit(0)" if you don't know what this means ;-)
-------------------------------------------------------------------

This sample application executes java code entered in an HTML form,
by generating and compiling java servlets on the fly.

In its first version (2008/11/20), javashell contains a number of
JCR code snippets, under /content/javashell/scripts. We might want
to extend this to more Sling-specific samples, but for now I just
needed that for an interactive JCR presentation. 

To test this bundle:

1. Install the org.apache.sling.scripting.java bundle, found under
   sling/scripting/java.
   
2. Install this bundle and navigate to

     http://localhost:8888/content/javashell/scripts/first_example.html
     
   That page should include a "Result of execution" section which lists
   some properties and child nodes.
		
   If you get an AccessDeniedException you need to set "allow anonymous
   access" to false in the SlingAuthenticator config, via 
   http://localhost:8888/system/console/configMgr
   
   If you get the default HTML rendering under the "Result of execution"
   header, the scripting.java bundle is probably not active.		
		
3. The above page includes links to edit the java code, and create new
   pages with more code to execute. 
   
The servlets are generated under /apps/javashell/servlets, and are *not* 
cleaned up currently, this is something that should be improved.   		
           
