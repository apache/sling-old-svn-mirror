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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.servlets.get.helpers.HtmlRendererServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Experimental JST script engine: converts a JST template (using the
 *  same templating syntax as ESP) to client-side javascript code
 *  that renders the page.
 *
 *  THIS IS STILL VERY ROUGH (2008/03/11), CONSIDER EXPERIMENTAL!!
 */
public class JstScriptEngine extends AbstractSlingScriptEngine {

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

    JstScriptEngine(ScriptEngineFactory scriptEngineFactory) {
        super(scriptEngineFactory);

        libraryScripts.add(SLING_JS_PATH);
        htmlRenderer = new HtmlRendererServlet();
    }

    public Object eval(Reader script, ScriptContext context) throws ScriptException {

        // This engine does not really run the script, we simply dump it
        // to the client inside a skeleton HTML document, and let the
        // client run the script
        Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
        final SlingScriptHelper helper = (SlingScriptHelper) props.get(SlingBindings.SLING);

        try {
            final PrintWriter w = helper.getResponse().getWriter();

            // access our data (need a Node)
            final Resource r = helper.getRequest().getResource();
            final Node n = r.adaptTo(Node.class);

            // output HEAD with javascript initializations
            // TODO we should instead parse (at least minimally) the template file, and inject our
            // stuff in the right places
            w.println("<html><head><title id=\"JstPageTitle\">");
            w.println(getTitle(r, n));
            w.println("</title>");
            
            // TODO get additional head stuff from the script?
            // something like
            //  <!-- jst:head
            //      <link rel="stylesheet" href="/apps/foo/foo.css"/>
            //  -->

            // library scripts
            final SlingHttpServletRequest request = helper.getRequest();
            for(String lib : libraryScripts) {
                final String fullScriptPath =
                    SlingRequestPaths.getContextPath(request)
                    + SlingRequestPaths.getServletPath(request)
                    + lib
                ;
                w.println("<script type=\"text/javascript\" src=\"" + fullScriptPath + "\"></script>");
            }

            // Node data in JSON format
            final JsonItemWriter j = new JsonItemWriter(null);
            final int maxRecursionLevels = 1;
            w.println("<script language='javascript'>");
            w.print("var currentNode=");
            if(n!=null) {
                j.dump(n, w, maxRecursionLevels);
            } else {
                w.print("{}");
            }
            w.println(";");
            w.println("</script>");

            // default rendering, turned off automatically from the javascript that
            // follows, if javascript is enabled
            w.println("</head><body>");
            w.println("<div id=\"JstDefaultRendering\">");
            if(n!=null) {
                htmlRenderer.render(w, r, n);
            }
            w.println("</div>");
            w.println("<script language=\"javascript\">");
            w.println("document.getElementById(\"JstDefaultRendering\").setAttribute(\"style\",\"display:none\");");
            w.println("</script>");
            
            // reference to script provided by the JstCodeGeneratorServlet
            final String scriptUrl = helper.getScript().getScriptResource().getPath() + ".jst.js";
            w.println("<script type=\"text/javascript\" src=\"" + scriptUrl + "\"></script>");

            // all done
            w.println("</body></html>");

        } catch (IOException ioe) {
            throw new ScriptException(ioe);

        } catch(RepositoryException re) {
            throw new ScriptException(re);

        } catch(JSONException je) {
            throw new ScriptException(je);
        }

        return null;
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
