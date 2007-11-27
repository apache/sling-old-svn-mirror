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
package org.apache.sling.core.impl.scripting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.core.impl.scripting.helper.ScriptHelper;
import org.apache.sling.core.impl.scripting.helper.ScriptPathIterator;
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
 *
 * @scr.component metatype="no"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Default SlingScriptResolver"
 * @scr.reference name="SlingScriptEngine"
 *                interface="org.apache.sling.api.scripting.SlingScriptEngine"
 *                cardinality="0..n" policy="dynamic"
 * @scr.service
 */
public class DefaultSlingScriptResolver implements SlingScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultSlingScriptResolver.class);

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    public static final String SCRIPT_BASE_PATH = "/sling/scripts";

    private Map<String, SlingScriptEngine> scriptEngines = new HashMap<String, SlingScriptEngine>();

    /**
     * @param req
     * @param scriptExtension
     * @return <code>true</code> if a DefaultSlingScript and a ScriptEngine to
     *         evaluate it could be found. Otherwise <code>false</code> is
     *         returned.
     * @throws ServletException
     * @throws IOException
     */
    public static void evaluateScript(final SlingScript script,
            final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException,
            IOException {
        try {
            // the script helper
            ScriptHelper helper = new ScriptHelper(script, req, resp);

            // prepare the properties for the script
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(SlingScriptEngine.SLING, helper);
            props.put(SlingScriptEngine.RESOURCE,
                helper.getRequest().getResource());
            props.put(SlingScriptEngine.REQUEST, helper.getRequest());
            props.put(SlingScriptEngine.RESPONSE, helper.getResponse());
            props.put(SlingScriptEngine.OUT, helper.getResponse().getWriter());
            props.put(SlingScriptEngine.LOG,
                LoggerFactory.getLogger(script.getScriptResource().getURI()));

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
            throw new SlingException("Cannot get DefaultSlingScript: "
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

        String scriptBaseName = buildScriptFilename(request) + ".";

        SlingScript result = null;
        Iterator<String> pathIterator = new ScriptPathIterator(request);
        ResourceResolver resolver = request.getResourceResolver();
        while (result == null && pathIterator.hasNext()) {
            Resource scriptRoot = resolver.getResource(pathIterator.next());
            if (scriptRoot != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Looking for script with filename={} under {}",
                        scriptBaseName, scriptRoot.getURI());
                }

                // offset is parent path + separator + base name
                final int scriptExtensionOffset = scriptRoot.getURI().length()
                    + 1 + scriptBaseName.length();

                // get the item and ensure it is a node
                Iterator<Resource> children = resolver.listChildren(scriptRoot);
                while (result == null && children.hasNext()) {
                    Resource scriptResource = children.next();
                    String scriptName = scriptResource.getURI();
                    String scriptExt = scriptName.substring(scriptExtensionOffset);
                    SlingScriptEngine scriptEngine = scriptEngines.get(scriptExt);
                    if (scriptEngine != null) {
                        result = new DefaultSlingScript(scriptResource,
                            scriptEngine);
                    }
                }
            }
        }

        if (result != null) {
            log.info("Script {} found for Resource={}",
                result.getScriptResource().getURI(), request.getResource());
        } else {
            log.debug("No script found for Resource={}", request.getResource());
        }

        return result;
    }

    // ---------- SCR integration ----------------------------------------------

    protected void bind(SlingScriptEngine slingScriptEngine) {
        String[] extensions = slingScriptEngine.getExtensions();
        for (String extension : extensions) {
            log.debug("Adding script engine {} for extension {}.",
                slingScriptEngine, extension);
            scriptEngines.put(extension, slingScriptEngine);
        }
    }

    protected void unbind(SlingScriptEngine slingScriptEngine) {
        String[] extensions = slingScriptEngine.getExtensions();
        for (String extension : extensions) {
            log.debug("Adding script engine {} for extension {}.",
                slingScriptEngine, extension);
            scriptEngines.remove(extension);
        }
    }

    // ---------- inner class --------------------------------------------------

    private String buildScriptFilename(SlingHttpServletRequest request) {

        String methodName = request.getMethod();
        String extension = request.getRequestPathInfo().getExtension();

        if (methodName == null || methodName.length() == 0) {
            // TODO: Shouldn't this be considered an error ??
            return "NO_METHOD";

        } else if (HttpConstants.METHOD_GET.equalsIgnoreCase(methodName)
            && extension != null && extension.length() > 0) {

            // for GET, we use the request extension
            return extension;

        } else {

            // for other methods use the method name
            return methodName.toUpperCase();
        }
    }
}
