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
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.microsling.resource.JcrNodeResource;
import org.apache.sling.microsling.scripting.helpers.ScriptFilenameBuilder;
import org.apache.sling.microsling.scripting.helpers.ScriptHelper;
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
 * in the repository.
 */
public class MicroslingScriptResolver implements SlingScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(MicroslingScriptResolver.class);

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    private final ScriptFilenameBuilder scriptFilenameBuilder = new ScriptFilenameBuilder();

    private Map<String, SlingScriptEngine> scriptEngines;

    private static final String[] DEFAULT_SCRIPT_ENGINES = new String[] {
          "org.apache.sling.scripting.javascript.RhinoJavasSriptEngine",
          "org.apache.sling.scripting.velocity.VelocityTemplatesScriptEngine",
          "org.apache.sling.scripting.freemarker.FreemarkerScriptEngine",
          "org.apache.sling.scripting.ruby.ErbScriptEngine"
       };

    public MicroslingScriptResolver() throws SlingException {
        scriptEngines = new HashMap<String, SlingScriptEngine>();
        for(String engineName : DEFAULT_SCRIPT_ENGINES) {
            try {
                final Class engineClass = this.getClass().getClassLoader().loadClass(engineName);
                final SlingScriptEngine engine = (SlingScriptEngine)engineClass.newInstance();
                addScriptEngine(engine);
            } catch (Exception ignore) {
                log.warn("Unable to instantiate script engine " + engineName, ignore);
            }
        }
    }

    /**
     * @param req
     * @param scriptExtension
     * @return <code>true</code> if a MicroslingScript and a ScriptEngine to
     *         evaluate it could be found. Otherwise <code>false</code> is
     *         returned.
     * @throws ServletException
     * @throws IOException
     */
    public static void evaluateScript(final SlingScript script, final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException,
            IOException {
        try {
            // the script helper
            ScriptHelper helper = new ScriptHelper(req, resp);

            // prepare the properties for the script
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(SlingScriptEngine.SLING, helper);
            props.put(SlingScriptEngine.RESOURCE, helper.getRequest().getResource());
            props.put(SlingScriptEngine.REQUEST, helper.getRequest());
            props.put(SlingScriptEngine.RESPONSE, helper.getResponse());
            props.put(SlingScriptEngine.OUT, helper.getResponse().getWriter());
            props.put(SlingScriptEngine.LOG, LoggerFactory.getLogger(script.getScriptResource().getURI()));

            resp.setContentType(req.getResponseContentType()
                + "; charset=UTF-8");

            // evaluate the script now using the ScriptEngine
            script.getScriptEngine().eval(script, props);

            // ensure data is flushed to the underlying writer in case
            // anything has been written
            helper.getResponse().getWriter().flush();
        } catch (IOException ioe) {
            throw ioe;
        } catch (ServletException se) {
            throw se;
        } catch (Exception e) {
            throw new SlingException("Cannot get MicroslingScript: "
                + e.getMessage(), e);
        }
    }

    public SlingScript findScript(String path) throws SlingException {
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

        // ensure repository access
        if (!(r.getRawData() instanceof Item)) {
            return null;
        }

        final Session s = ((Item) r.getRawData()).getSession();
        MicroslingScript result = null;

        if (r == null) {
            return null;
        }

        String scriptFilename = scriptFilenameBuilder.buildScriptFilename(
            request.getMethod(),
            request.getRequestPathInfo().getSelectorString(),
            request.getResponseContentType(), "*");
        String scriptPath = scriptFilenameBuilder.buildScriptPath(r);

        // SLING-72: if the scriptfilename contains a relative path, move that
        // to the scriptPath and make the scriptFilename a direct child pattern
        int lastSlash = scriptFilename.lastIndexOf('/');
        if (lastSlash >= 0) {
            scriptPath += "/" + scriptFilename.substring(0, lastSlash);
            scriptFilename = scriptFilename.substring(lastSlash + 1);
        }

        // this is the location of the trailing asterisk
        final int scriptExtensionOffset = scriptFilename.length() - 1;

        if (log.isDebugEnabled()) {
            log.debug("Looking for script with filename=" + scriptFilename
                + " under " + scriptPath);
        }

        if (s.itemExists(scriptPath)) {

            // get the item and ensure it is a node
            final Item i = s.getItem(scriptPath);
            if (i.isNode()) {
                Node parent = (Node) i;
                NodeIterator scriptNodeIterator = parent.getNodes(scriptFilename);
                while (scriptNodeIterator.hasNext()) {
                    Node scriptNode = scriptNodeIterator.nextNode();

                    // SLING-72: Require the node to be an nt:file
                    if (scriptNode.isNodeType("nt:file")) {

                        String scriptName = scriptNode.getName();
                        String scriptExt = scriptName.substring(scriptExtensionOffset);
                        SlingScriptEngine scriptEngine = scriptEngines.get(scriptExt);

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

        if (result != null) {
            log.info("Found nt:file script node {} for Resource={}",
                result.getScriptResource().getURI(), r);
        } else {
            log.debug(
                "nt:file script node not found at path={} for Resource={}",
                scriptPath, r);
        }

        return result;
    }

    private void addScriptEngine(SlingScriptEngine scriptEngine) {
        String[] extensions = scriptEngine.getExtensions();
        for (String extension : extensions) {
            scriptEngines.put(extension, scriptEngine);
        }
    }

    private static class MicroslingScript implements SlingScript {

        private Resource scriptResource;

        private SlingScriptEngine scriptEngine;

        public Resource getScriptResource() {
            return scriptResource;
        }

        void setScriptResource(Resource scriptResource) {
            this.scriptResource = scriptResource;
        }

        public SlingScriptEngine getScriptEngine() {
            return scriptEngine;
        }

        void setScriptEngine(SlingScriptEngine scriptEngine) {
            this.scriptEngine = scriptEngine;
        }

        public Reader getScriptReader() throws IOException {

            Property property;
            Value value;

            try {

                if (getScriptResource().getRawData() instanceof Node) {
                    // SLING-72: Cannot use primary items due to WebDAV creating
                    // nt:unstructured as jcr:content node. So we just assume
                    // nt:file and try to use the well-known data path
                    Node node = (Node) getScriptResource().getRawData();
                    property = node.getProperty("jcr:content/jcr:data");
                } else {
                    throw new IOException("Scriptresource " + getScriptResource() + " must is not JCR Node based");
                }

                value = null;
                if (property.getDefinition().isMultiple()) {
                    // for a multi-valued property, we take the first non-null
                    // value (null values are possible in multi-valued
                    // properties)
                    // TODO: verify this claim ...
                    Value[] values = property.getValues();
                    for (Value candidateValue : values) {
                        if (candidateValue != null) {
                            value = candidateValue;
                            break;
                        }
                    }

                    // incase we could not find a non-null value, we bail out
                    if (value == null) {
                        throw new IOException("Cannot access "
                            + getScriptResource().getURI());
                    }
                } else {
                    // for single-valued properties, we just take this value
                    value = property.getValue();
                }
            } catch (RepositoryException re) {
                throw (IOException) new IOException("Cannot get script "
                    + getScriptResource().getURI()).initCause(re);
            }

            // Now know how to get the input stream, we still have to decide
            // on the encoding of the stream's data. Primarily we assume it is
            // UTF-8, which is a default in many places in JCR. Secondarily
            // we try to get a jcr:encoding property besides the data property
            // to provide a possible encoding
            String encoding = "UTF-8";
            try {
                Node parent = property.getParent();
                if (parent.hasNode(JCR_ENCODING)) {
                    encoding = parent.getProperty(JCR_ENCODING).getString();
                }
            } catch (RepositoryException re) {
                // don't care if we fail for any reason here, just assume
                // default
            }

            // access the value as a stream and return a buffered reader
            // converting the stream data using UTF-8 encoding, which is
            // the default encoding used
            try {
                InputStream input = value.getStream();
                return new BufferedReader(
                    new InputStreamReader(input, encoding));
            } catch (RepositoryException re) {
                throw (IOException) new IOException("Cannot get script "
                    + getScriptResource().getURI()).initCause(re);
            }
        }
    }

}
