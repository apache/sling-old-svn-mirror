/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.Context;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.microsling.resource.JcrNodeResource;
import org.apache.sling.microsling.scripting.helpers.ScriptFilenameBuilder;
import org.apache.sling.microsling.scripting.helpers.ScriptHelper;
import org.apache.sling.microsling.scripting.helpers.ScriptSearchPathsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find scripts in the repository, based on the current Resource type. The
 * script filename is built using the current HTTP request method name, followed
 * by the extension of the current request and the desired script extension. For
 * example, a "js" script for a GET request on a Resource of type some/type with
 * request extension "html" should be stored as
 *
 * <pre>
 *      /sling/scripts/some/type/get.html.js
 * </pre>
 * 
 * in the repository. In the above example, "/sling/scripts" is a script search path,
 * which is provided by {#ScriptSearchPathsBuilder} 
 */
public class MicroslingScriptResolver implements SlingScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(MicroslingScriptResolver.class);

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    private final ScriptFilenameBuilder scriptFilenameBuilder = new ScriptFilenameBuilder();
    private final ScriptSearchPathsBuilder scriptSearchPathsBuilder = new ScriptSearchPathsBuilder();

    private final ScriptEngineManager scriptEngineManager;
    
    public MicroslingScriptResolver() throws SlingException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = getClass().getClassLoader();
        }
        
        scriptEngineManager = new ScriptEngineManager(loader);
    }

    public SlingScript findScript(ResourceResolver resourceResolver, String name)
            throws SlingException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Try to find a script Node that can process the given request, based on
     * the rules defined above.
     *
     * @return null if not found.
     */
    public SlingScript resolveScript(final SlingHttpServletRequest request)
            throws SlingException {
        try {
            return resolveScriptInternal(request);
        } catch (RepositoryException re) {
            throw new SlingException("Cannot resolve script for request", re);
        }
    }

    public SlingScript resolveScriptInternal(
            final SlingHttpServletRequest request) throws RepositoryException, SlingException {

        final Resource r = request.getResource();
        final Session s = (Session)request.getAttribute(Session.class.getName());
        MicroslingScript result = null;
        
        // SLING-133: do not resolve scripts for Properties, we want to use our default
        // renderers for them (TODO: having that test here is really a temp fix)
        if(r.adaptTo(Property.class) != null) {
            return null;
        }

        final String scriptFilename = scriptFilenameBuilder.buildScriptFilename(
            request.getMethod(),
            request.getRequestPathInfo().getExtension(),
            "*");

        // this is the location of the trailing asterisk
        final int scriptExtensionOffset = scriptFilename.length() - 1;

        final List<String> possiblePaths = scriptSearchPathsBuilder.getScriptSearchPaths(
                request.getResource(), request.getRequestPathInfo().getSelectors());
        for(String currentPath : possiblePaths) {

            if(result != null) {
                break;
            }

            if (log.isDebugEnabled()) {
                log.debug("Looking for script with filename=" + scriptFilename
                    + " under " + currentPath);
            }
            
            // do not throw exceptions if path is invalid, that might happen
            // depending on the resource type / search path values
            boolean pathExists = false;
            try {
                pathExists = s.itemExists(currentPath);
            } catch(Exception e) {
                if(log.isDebugEnabled()) {
                    log.debug("itemExists(" + currentPath + ") call fails, exception ignored: " + e);
                }
            }

            if (pathExists) {
                // get the item and ensure it is a node
                final Item i = s.getItem(currentPath);
                if (i.isNode()) {
                    Node parent = (Node) i;
                    NodeIterator scriptNodeIterator = parent.getNodes(scriptFilename);
                    while (scriptNodeIterator.hasNext()) {
                        Node scriptNode = scriptNodeIterator.nextNode();

                        // SLING-72: Require the node to be an nt:file
                        if (scriptNode.isNodeType("nt:file")) {

                            String scriptName = scriptNode.getName();
                            String scriptExt = scriptName.substring(scriptExtensionOffset);
                            ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension(scriptExt);

                            if (scriptEngine != null) {
                                MicroslingScript script = new MicroslingScript();
                                script.setScriptResource(new JcrNodeResource(scriptNode));
                                script.setScriptEngine(scriptEngine);
                                result = script;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (result != null) {
            log.info("Found nt:file script node {} for Resource={}",
                result.getScriptResource().getURI(), r);
        } else {
            log.debug(
                "nt:file script node not found under path={} for Resource={}",
                possiblePaths.get(possiblePaths.size() - 1)
            );
        }

        return result;
    }

    private static class MicroslingScript implements SlingScript {

        private Resource scriptResource;

        private ScriptEngine scriptEngine;

        public Resource getScriptResource() {
            return scriptResource;
        }

        void setScriptResource(Resource scriptResource) {
            this.scriptResource = scriptResource;
        }

        public ScriptEngine getScriptEngine() {
            return scriptEngine;
        }

        void setScriptEngine(ScriptEngine scriptEngine) {
            this.scriptEngine = scriptEngine;
        }

        public Reader getScriptReader() throws IOException {

            InputStream stream = getScriptResource().adaptTo(InputStream.class);
            if (stream == null) {
                throw new IOException(
                    "Cannot get a stream to the script resource "
                        + getScriptResource());
            }

            // Now know how to get the input stream, we still have to decide
            // on the encoding of the stream's data. Primarily we assume it is
            // UTF-8, which is a default in many places in JCR. Secondarily
            // we try to get a jcr:encoding property besides the data property
            // to provide a possible encoding
            ResourceMetadata meta = getScriptResource().getResourceMetadata();
            String encoding = (String) meta.get(ResourceMetadata.CHARACTER_ENCODING);
            if (encoding == null) {
                encoding = "UTF-8";
            }

            // access the value as a stream and return a buffered reader
            // converting the stream data using UTF-8 encoding, which is
            // the default encoding used
            return new BufferedReader(new InputStreamReader(stream, encoding));
        }
        
        public void eval(SlingBindings props) throws IOException,
                ServletException {
            try {
                
                SlingHttpServletRequest req = (SlingHttpServletRequest) props.get(SlingBindings.REQUEST);
                SlingHttpServletResponse res = (SlingHttpServletResponse) props.get(SlingBindings.RESPONSE);
                
                // the script helper
                ScriptHelper helper = new ScriptHelper(req, res, this);

                // prepare the properties for the script
                Bindings bindings = new SimpleBindings(); // getScriptEngine().createBindings();
                
                bindings.put(SlingBindings.SLING, helper);
                bindings.put(SlingBindings.RESOURCE, helper.getRequest().getResource());
                bindings.put(SlingBindings.REQUEST, helper.getRequest());
                bindings.put(SlingBindings.RESPONSE, helper.getResponse());
                bindings.put(SlingBindings.OUT, helper.getResponse().getWriter());
                bindings.put(SlingBindings.LOG, LoggerFactory.getLogger(getScriptResource().getURI()));

                res.setContentType(req.getResponseContentType()
                    + "; charset=UTF-8");

                ScriptContext context = new SimpleScriptContext();
                context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                context.setWriter(helper.getResponse().getWriter());
//                context.setReader();
//                context.setErrorWriter(arg0);
                
                // evaluate the script now using the ScriptEngine
                getScriptEngine().eval(getScriptReader(), context);

                // ensure data is flushed to the underlying writer in case
                // anything has been written
                helper.getResponse().getWriter().flush();
            } catch (IOException ioe) {
                throw ioe;
//            } catch (ServletException se) {
//                throw se;
            } catch (Exception e) {
                throw new SlingException("Cannot get MicroslingScript: "
                    + e.getMessage(), e);
            }
        }
    }

}
