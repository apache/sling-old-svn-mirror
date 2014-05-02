Sling scripting.java module
---------------------------

This module implements a script engine for java servlets, that are compiled
on the fly by Sling.

To test it:

1. Install this bundle in Sling, for example with

  mvn -P autoInstallBundle clean install -Dsling.url=http://localhost:8080/system/console
  
If Sling is running with the launchpad/testing setup.

2. Create /apps/foo/foo.java in your repository (via WebDAV for example), with this code:

package apps.foo;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

public class foo extends SlingSafeMethodsServlet {
    
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        response.setContentType("text/plain");
        response.getWriter().write("Response from " + getClass().getName() + " at " + new java.util.Date());
    }
}   

3. Request http://localhost:8080/content/foo/*.html which should display something like

  Response from apps.foo.foo at Tue Nov 18 14:49:14 CET 2008
  
4. The servlet code should be automatically recompiled after any changes.
  