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
package org.apache.sling.microsling.experimental;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.microsling.helpers.json.JsonItemWriter;
import org.apache.sling.microsling.slingservlets.renderers.DefaultHtmlRenderer;
import org.apache.sling.scripting.javascript.EspReader;

/** Experimental ECT script engine: converts an ECT template (using the 
 *  same templating syntax as ESP) to client-side javascript code
 *  that renders the page.
 */
public class EctScriptEngine implements SlingScriptEngine {

    public static final String ECT_SCRIPT_EXTENSION = "ect";
    private final List<String> libraryScripts = new LinkedList<String>();
    private final DefaultHtmlRenderer htmlRenderer;
    
    public EctScriptEngine() {
        // TODO hardcoded for now...
        libraryScripts.add("/microjax/microjax.js");
        htmlRenderer = new DefaultHtmlRenderer();
    }
    
    public void eval(SlingScript script, Map<String, Object> props)
        throws SlingException, IOException {
        
        // This engine does not really run the script, we simply dump it
        // to the client inside a skeleton HTML document, and let the
        // client run the script
        final PrintWriter w = (PrintWriter)(props.get(SlingScriptEngine.OUT));
        final EspReader er = new EspReader(script.getScriptReader());
        er.setOutInitStatement("out=document;");
        
        try {
            // access our data (need a Node)
            final Resource r = (Resource)props.get(SlingScriptEngine.RESOURCE);
            final Node n = r.adaptTo(Node.class);
            if(n == null) {
                throw new HttpStatusCodeException(
                        HttpServletResponse.SC_NOT_FOUND,"Resource does not provide a Node, cannot render");
            }
            
            // output HEAD with javascript initializations
            w.println("<html><head><title id=\"EctPageTitle\">");
            w.println("ECT rendering of " + n.getPath());
            w.println("</title>");
            
            // library scripts
            final SlingHttpServletRequest request = (SlingHttpServletRequest)props.get(SlingScriptEngine.REQUEST);
            for(String lib : libraryScripts) {
                final String fullScriptPath =             
                    SlingRequestPaths.getContextPath(request)
                    + SlingRequestPaths.getServletPath(request)
                    + lib
                ;
                w.println("<script src=\"" + fullScriptPath + "\"></script>");  
            }
            
            // onLoad method
            w.println("<script language=\"javascript\">");
            w.println("function ectOnLoad() { if(typeof onLoad == \"function\") { onLoad(); } }");
            w.println("</script>");
            
            // node data in JSON format
            final JsonItemWriter j = new JsonItemWriter(null);
            final int maxRecursionLevels = 1;
            w.println("<script language='javascript'>");
            w.print("var currentNode=");
            j.dump(n, w, maxRecursionLevels);
            w.println(";");
            w.println("</script>");
            w.println("</head><body onLoad=\"ectOnLoad()\">");
            
            // output our parsed script, first in body
            w.println("<div id=\"EctRenderingScript\">\n<script language='javascript'>");
            copy(er,w);
            w.println("</script>\n</div>");
            
            // default rendering, turned off automatically from the javascript that 
            // follows, if javascript is enabled
            w.println("<div id=\"EctDefaultRendering\">");
            htmlRenderer.render(w, r, n);
            w.println("</div>");
            w.println("<script language=\"javascript\">");
            w.println("document.getElementById(\"EctDefaultRendering\").setAttribute(\"style\",\"display:none\");");
            w.println("</script>");
            
            // all done
            w.println("</body></html>");
            
        } catch(RepositoryException re) {
            throw new SlingException("RepositoryException in EctScriptEngine.eval()",re);
            
        } catch(JSONException je) {
            throw new SlingException("JSONException in EctScriptEngine.eval()",je);
            
        }
    }

    public String getEngineName() {
        return "ECT script engine (Ecmascript Client Templates)";
    }

    public String getEngineVersion() {
        return "0.9";
    }

    public String[] getExtensions() {
        return new String [] { ECT_SCRIPT_EXTENSION };
    }
    
    private static void copy(Reader r, Writer w) throws IOException {
        final int bufsize = 16384;
        final char [] buffer = new char[bufsize];
        int n = 0;
        while( (n = r.read(buffer, 0, bufsize)) > 0) {
            w.write(buffer, 0, n);
        }
    }
}
