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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.script.ScriptException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.cyberneko.html.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** Generates HTML code for JST templates */
class HtmlCodeGenerator {
    private final List<String> libraryScripts = new LinkedList<String>();
    private final HtmlContentRenderer htmlRenderer;
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
        htmlRenderer = new HtmlContentRenderer();
    }

    /** Generate HTML code for the given request and script path */
    void generateHtml(SlingHttpServletRequest request, String scriptPath,
            InputStream scriptStream, PrintWriter output)
    throws RepositoryException, JSONException, ScriptException, IOException {

        // access our data (need a Node)
        final Resource r = request.getResource();
        final Node n = r.adaptTo(Node.class);
        final String pageTitle = getTitle(r, n);

        // Parse script using the NekoHTML permissive HTML parser
        final DOMParser parser = new DOMParser();
        try {
            parser.setFeature("http://xml.org/sax/features/namespaces", false);
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");
            parser.parse(new InputSource(scriptStream));
        } catch(Exception e) {
            final ScriptException se = new ScriptException("Error parsing script " + scriptPath);
            se.initCause(e);
            throw se;
        }
        final Document template = parser.getDocument();

        // compute default rendering
        final StringWriter defaultRendering = new StringWriter();
        if(n!=null) {
            final PrintWriter pw = new PrintWriter(defaultRendering);
            htmlRenderer.render(pw, r, n, pageTitle);
            pw.flush();
        }

        // compute currentNode values in JSON format
        final StringWriter jsonData = new StringWriter();
        if(n != null) {
            final PrintWriter pw = new PrintWriter(jsonData);
            final JsonItemWriter j = new JsonItemWriter(null);
            final int maxRecursionLevels = 1;
            pw.print("var currentNode=");
            j.dump(n, pw, maxRecursionLevels);
            pw.print(";");
            pw.flush();
        }

        // run XSLT transform on script, passing parameter
        // for our computed values
        final String xslt = "/xslt/script-transform.xsl";
        InputStream xslTransform = getClass().getResourceAsStream(xslt);
        if(xslTransform == null) {
            throw new ScriptException("XSLT transform " + xslt + " not found");
        }
        try {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer t = tf.newTransformer(new StreamSource(xslTransform));
            t.setParameter("pageTitle", pageTitle);
            t.setParameter("slingScriptPath", fullPath(request,SLING_JS_PATH));
            t.setParameter("jstScriptPath", fullPath(request, scriptPath + ".jst.js"));
            t.setParameter("defaultRendering", defaultRendering.toString());
            t.setParameter("jsonData", jsonData.toString());
            final Result result = new StreamResult(output);
            final DOMSource source = new DOMSource(template);
            t.transform(source, result);

        } catch (Exception e) {
            final ScriptException se = new ScriptException("Error in XSLT transform for " + scriptPath);
            se.initCause(e);
            throw se;

        } finally {
            xslTransform.close();
        }
    }

    /** Return the full path to supplied resource */
    static String fullPath (SlingHttpServletRequest request, String path) {
        return SlingRequestPaths.getContextPath(request) + SlingRequestPaths.getServletPath(request) + path;
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
