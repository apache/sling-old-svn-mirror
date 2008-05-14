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
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.javascript.io.EspReader;
import org.apache.sling.servlets.get.helpers.HtmlRendererServlet;

/** Experimental JST script engine: converts a JST template (using the
 *  same templating syntax as ESP) to client-side javascript code
 *  that renders the page.
 *
 *  THIS IS STILL VERY ROUGH (2008/03/11), CONSIDER EXPERIMENTAL!!
 */
public class JstScriptEngine extends AbstractSlingScriptEngine {

    private final List<String> libraryScripts = new LinkedList<String>();
    private final HtmlRendererServlet htmlRenderer;
    private final ScriptFilteredCopy copier = new ScriptFilteredCopy();
    
    // TODO should be configurable or synced with the actual location
    public final static String SLING_JS_PATH = "/system/sling.js";

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
            final Reader er = getReader(helper.getScript().getScriptResource());

            // access our data (need a Node)
            final Resource r = helper.getRequest().getResource();

            // to render we must have either a Node or a SyntheticResourceData
            final Node n = r.adaptTo(Node.class);

            // output HEAD with javascript initializations
            // TODO we should instead parse (at least minimally) the template file, and inject our
            // stuff in the right places
            w.println("<html><head><title id=\"JstPageTitle\">");
            w.println("JST rendering of " + r.getPath());
            w.println("</title>");

            // library scripts
            final SlingHttpServletRequest request = helper.getRequest();
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
            w.println("function jstOnLoad() { if(typeof onLoad == \"function\") { onLoad(); } }");
            w.println("</script>");

            // data in JSON format
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
            w.println("</head><body onLoad=\"jstOnLoad()\">");

            // output our parsed script, first in body
            w.println("<div id=\"JstRenderingScript\">\n<script language='javascript'>");
            copier.copy(er,w);
            w.println("</script>\n</div>");

            // default rendering, turned off automatically from the javascript that
            // follows, if javascript is enabled
            w.println("<div id=\"JstDefaultRendering\">");
            if(n!=null) {
                htmlRenderer.render(w, r, n);
            }
            w.println("</div>");
            w.println("<script language=\"javascript\">");
            w.println("document.getElementById(\"JstDefaultRendering\").setAttribute(\"style\",\"display:none\");");
            w.println("</script>");

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

    private Reader getReader(Resource resource) throws IOException {
        InputStream ins = resource.adaptTo(InputStream.class);
        if (ins != null) {
            String enc = (String) resource.getResourceMetadata().get(ResourceMetadata.CHARACTER_ENCODING);
            if (enc == null) {
                enc = "UTF-8";
            }

            Reader r = new InputStreamReader(ins, enc);
            EspReader er = new EspReader(r);
            er.setOutInitStatement("out=document;\n");

            return er;
        }

        return null;
    }
}
