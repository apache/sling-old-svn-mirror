/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.jst;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.servlets.get.helpers.HtmlRendererServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Generates HTML code for JST templates */ 
class HtmlCodeGenerator {
    private final List<String> libraryScripts = new LinkedList<String>();
    private final HtmlRendererServlet htmlRenderer;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // TODO should be configurable or synced with the actual location
    public final static String SLING_JS_PATH = "/system/sling.js";
    
    /** Property names to use to build the page title, in order of preference */
    public final static String [] TITLE_PROPERTY_NAMES = {
        "title",
        "name",
        "description"
    };

    public HtmlCodeGenerator() {
        libraryScripts.add(SLING_JS_PATH);
        htmlRenderer = new HtmlRendererServlet();
    }
    
    /** Generate HTML code for the given request and script path */
    void generateHtml(SlingHttpServletRequest request, String scriptPath, PrintWriter output) throws RepositoryException, JSONException {
        
        // access our data (need a Node)
        final Resource r = request.getResource();
        final Node n = r.adaptTo(Node.class);

        // output HEAD with javascript initializations
        // TODO we should instead parse (at least minimally) the template file, and inject our
        // stuff in the right places
        output.println("<html><head><title id=\"JstPageTitle\">");
        output.println(getTitle(r, n));
        output.println("</title>");
        
        // TODO get additional head stuff from the script?
        // something like
        //  <!-- jst:head
        //      <link rel="stylesheet" href="/apps/foo/foo.css"/>
        //  -->

        // library scripts
        for(String lib : libraryScripts) {
            final String fullScriptPath =
                SlingRequestPaths.getContextPath(request)
                + SlingRequestPaths.getServletPath(request)
                + lib
            ;
            output.println("<script type=\"text/javascript\" src=\"" + fullScriptPath + "\"></script>");
        }

        // Node data in JSON format
        final JsonItemWriter j = new JsonItemWriter(null);
        final int maxRecursionLevels = 1;
        output.println("<script language='javascript'>");
        output.print("var currentNode=");
        if(n!=null) {
            j.dump(n, output, maxRecursionLevels);
        } else {
            output.print("{}");
        }
        output.println(";");
        output.println("</script>");

        // default rendering, turned off automatically from the javascript that
        // follows, if javascript is enabled
        output.println("</head><body>");
        output.println("<div id=\"JstDefaultRendering\">");
        if(n!=null) {
            htmlRenderer.render(output, r, n);
        }
        output.println("</div>");
        output.println("<script language=\"javascript\">");
        output.println("var e = document.getElementById(\"JstDefaultRendering\"); e.parentNode.removeChild(e);");
        output.println("</script>");
        
        // reference to script provided by the JstCodeGeneratorServlet
        final String scriptUrl = scriptPath + ".jst.js";
        output.println("<script type=\"text/javascript\" src=\"" + scriptUrl + "\"></script>");

        // all done
        output.println("</body></html>");
        
    }
    
    /** Return the title to use for the generated page */ 
    protected String getTitle(Resource r, Node n) {
        String title = null;
        
        if(n != null) {
            for(String name : TITLE_PROPERTY_NAMES) {
                try {
                    if(n.hasProperty(name)) {
                        final String s = n.getProperty(name).getString();
                        if(s.length() > 0) {
                            title = s;
                            break;
                        }
                    } 
                } catch(RepositoryException re) {
                    log.warn("RepositoryException in getTitle()", re);
                }
            }
        }
        
        if(title == null) {
            title = r.getPath();
            if(title.startsWith("/")) {
                title = title.substring(1);
            }
        }
        
        return title;
    }
}
