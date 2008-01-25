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
package org.apache.sling.scripting.resolver.impl;

import static java.lang.Boolean.TRUE;
import static org.apache.sling.api.scripting.SlingBindings.FLUSH;
import static org.apache.sling.api.scripting.SlingBindings.LOG;
import static org.apache.sling.api.scripting.SlingBindings.OUT;
import static org.apache.sling.api.scripting.SlingBindings.REQUEST;
import static org.apache.sling.api.scripting.SlingBindings.RESOURCE;
import static org.apache.sling.api.scripting.SlingBindings.RESPONSE;
import static org.apache.sling.api.scripting.SlingBindings.SLING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.resolver.ScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultSlingScript implements SlingScript, Servlet, ServletConfig {

    private Resource scriptResource;

    private ScriptEngine scriptEngine;

    private ServletContext servletContext;

    private Dictionary<String, String> initParameters;

    DefaultSlingScript(Resource scriptResource, ScriptEngine scriptEngine) {
        this.scriptResource = scriptResource;
        this.scriptEngine = scriptEngine;
    }

    // ---------- SlingScript interface ----------------------------------------

    public Resource getScriptResource() {
        return scriptResource;
    }

    /**
     * @throws ScriptEvaluationException
     */
    public void eval(SlingBindings props) {

        String scriptName = getScriptResource().getPath();

        try {
            Bindings bindings = verifySlingBindings(scriptName, props);
            
            ScriptContext ctx = new SimpleScriptContext();
            ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            ctx.setReader(((SlingHttpServletRequest) bindings.get(REQUEST)).getReader());
            ctx.setWriter((Writer) bindings.get(OUT));
            ctx.setErrorWriter(new LogWriter((Logger) bindings.get(LOG)));

            Reader reader = getScriptReader();

            // evaluate the script
            scriptEngine.eval(reader, ctx);

            // optionall flush the output channel
            Object flushObject = bindings.get(SlingBindings.FLUSH);
            if (flushObject instanceof Boolean
                && ((Boolean) flushObject).booleanValue()) {
                ctx.getWriter().flush();
            }

            // allways flush the error channel
            ctx.getErrorWriter().flush();

        } catch (IOException ioe) {
            throw new ScriptEvaluationException(scriptName, ioe.getMessage(),
                ioe);
            
        } catch (ScriptException se) {
            Throwable cause = (se.getCause() == null) ? se : se.getCause();
            throw new ScriptEvaluationException(scriptName, se.getMessage(),
                cause);
        }
    }

    // ---------- Servlet interface --------------------------------------------

    public void init(ServletConfig servletConfig) {
        if (servletConfig != null) {
            Dictionary<String, String> params = new Hashtable<String, String>();
            for (Enumeration<?> ne = servletConfig.getInitParameterNames(); ne.hasMoreElements();) {
                String name = String.valueOf(ne.nextElement());
                String value = servletConfig.getInitParameter(name);
                params.put(name, value);
            }
            
            servletContext = servletConfig.getServletContext();
        }
    }

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        SlingHttpServletRequest request = (SlingHttpServletRequest) req;

        try {
            // prepare the properties for the script
            SlingBindings props = new SlingBindings();
            props.put(REQUEST, req);
            props.put(RESPONSE, res);
            props.put(FLUSH, TRUE);

            res.setCharacterEncoding("UTF-8");
            res.setContentType(request.getResponseContentType());

            // evaluate the script now using the ScriptEngine
            eval(props);

        } catch (ScriptEvaluationException see) {
            throw see;
        } catch (Exception e) {
            throw new SlingException("Cannot get DefaultSlingScript: "
                + e.getMessage(), e);
        }
    }

    public ServletConfig getServletConfig() {
        return this;
    }

    public String getServletInfo() {
        return "Script " + getScriptResource().getPath();
    }

    public void destroy() {
        initParameters = null;
        servletContext = null;
    }

    // ---------- ServletConfig ------------------------------------------------

    public String getInitParameter(String name) {
        Dictionary<String, String> params = initParameters;
        return (params != null) ? params.get(name) : null;
    }

    public Enumeration<String> getInitParameterNames() {
        Dictionary<String, String> params = initParameters;
        return (params != null) ? params.keys() : null;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getServletName() {
        return getScriptResource().getPath();
    }

    // ---------- internal -----------------------------------------------------

    private Reader getScriptReader() throws IOException {

        InputStream input = getScriptResource().adaptTo(InputStream.class);
        if (input == null) {
            throw new IOException("Cannot get a stream to the script resource "
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
        return new BufferedReader(new InputStreamReader(input, encoding));
    }

    private Bindings verifySlingBindings(String scriptName,
            SlingBindings slingBindings) throws IOException {
        
        Object requestObject = slingBindings.get(REQUEST);
        if (!(requestObject instanceof SlingHttpServletRequest)) {
            throw fail(scriptName, REQUEST, "Missing or wrong type");
        }

        Object responseObject = slingBindings.get(RESPONSE);
        if (!(responseObject instanceof SlingHttpServletResponse)) {
            throw fail(scriptName, RESPONSE, "Missing or wrong type");
        }

        Object resourceObject = slingBindings.get(RESOURCE);
        if (resourceObject != null && !(resourceObject instanceof Resource)) {
            throw fail(scriptName, RESOURCE, "Wrong type");
        }

        Object writerObject = slingBindings.get(OUT);
        if (writerObject != null && !(writerObject instanceof PrintWriter)) {
            throw fail(scriptName, OUT, "Wrong type");
        }

        Bindings bindings = new SimpleBindings();
        SlingScriptHelper sling;

        Object slingObject = slingBindings.get(SLING);
        if (slingObject == null) {

            sling = new ScriptHelper(this,
                (SlingHttpServletRequest) requestObject,
                (SlingHttpServletResponse) responseObject);

        } else if (slingObject instanceof SlingScriptHelper) {

            sling = (SlingScriptHelper) slingObject;

            if (sling.getRequest() != requestObject) {
                throw fail(scriptName, REQUEST,
                    "Not the same as request field of SlingScriptHelper");
            }

            if (sling.getResponse() != responseObject) {
                throw fail(scriptName, RESPONSE,
                    "Not the same as response field of SlingScriptHelper");
            }

            if (resourceObject != null
                && sling.getRequest().getResource() != resourceObject) {
                throw fail(scriptName, RESOURCE,
                    "Not the same as resource of the SlingScriptHelper request");
            }

            if (writerObject != null
                && sling.getResponse().getWriter() != writerObject) {
                throw fail(scriptName, OUT,
                    "Not the same as writer of the SlingScriptHelper response");
            }

        } else {

            throw fail(scriptName, SLING, "Wrong type");

        }

        Object logObject = slingBindings.get(LOG);
        if (logObject == null) {
            logObject = LoggerFactory.getLogger(getLoggerName());
        } else if (!(logObject instanceof Logger)) {
            throw fail(scriptName, LOG, "Wrong type");
        }

        // set base variables
        bindings.put(SLING, sling);
        bindings.put(REQUEST, sling.getRequest());
        bindings.put(RESPONSE, sling.getResponse());
        bindings.put(RESOURCE, sling.getRequest().getResource());
        bindings.put(OUT, sling.getResponse().getWriter());
        bindings.put(LOG, logObject);

        // copy non-base variables
        for (Map.Entry<String, Object> entry : slingBindings.entrySet()) {
            if (!bindings.containsKey(entry.getKey())) {
                bindings.put(entry.getKey(), entry.getValue());
            }
        }

        return bindings;
    }

    private ScriptEvaluationException fail(String scriptName,
            String variableName, String message) {
        return new ScriptEvaluationException(scriptName, variableName + ": "
            + message);
    }

    private String getLoggerName() {
        String name = getScriptResource().getPath();
        name = name.replace('.', '$');
        name = name.replace('/', '.');
        return name;
    }
}