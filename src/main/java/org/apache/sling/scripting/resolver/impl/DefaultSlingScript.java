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
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.resolver.ScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultSlingScript implements SlingScript {

    private Resource scriptResource;

    private ScriptEngine scriptEngine;

    DefaultSlingScript(Resource scriptResource, ScriptEngine scriptEngine) {
        this.scriptResource = scriptResource;
        this.scriptEngine = scriptEngine;
    }

    public Resource getScriptResource() {
        return scriptResource;
    }

    public void eval(SlingBindings props) throws IOException, ServletException {

        Bindings bindings = verifySlingBindings(props);

        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        ctx.setReader(((SlingHttpServletRequest) bindings.get(REQUEST)).getReader());
        ctx.setWriter((Writer) bindings.get(OUT));
        ctx.setErrorWriter(new LogWriter((Logger) bindings.get(LOG)));

        Reader reader = getScriptReader();

        try {
            // evaluate the script
            scriptEngine.eval(reader, ctx);
            
            // optionall flush the output channel
            Object flushObject = bindings.get(SlingBindings.FLUSH);
            if (flushObject instanceof Boolean && ((Boolean) flushObject).booleanValue()) {
                ctx.getWriter().flush();
            }
            
            // allways flush the error channel
            ctx.getErrorWriter().flush();
            
        } catch (ScriptException se) {
            throw new ServletException(se.getMessage(), se);
        }
    }

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

    private Bindings verifySlingBindings(SlingBindings slingBindings)
            throws IOException, ServletException {
        Object requestObject = slingBindings.get(REQUEST);
        if (!(requestObject instanceof SlingHttpServletRequest)) {
            throw fail(REQUEST, "Missing or wrong type");
        }

        Object responseObject = slingBindings.get(RESPONSE);
        if (!(responseObject instanceof SlingHttpServletResponse)) {
            throw fail(RESPONSE, "Missing or wrong type");
        }

        Object resourceObject = slingBindings.get(RESOURCE);
        if (resourceObject != null && !(resourceObject instanceof Resource)) {
            throw fail(RESOURCE, "Wrong type");
        }

        Object writerObject = slingBindings.get(OUT);
        if (writerObject != null && !(writerObject instanceof PrintWriter)) {
            throw fail(OUT, "Wrong type");
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
                throw fail(REQUEST,
                    "Not the same as request field of SlingScriptHelper");
            }

            if (sling.getResponse() != responseObject) {
                throw fail(RESPONSE,
                    "Not the same as response field of SlingScriptHelper");
            }

            if (resourceObject != null
                && sling.getRequest().getResource() != resourceObject) {
                throw fail(RESOURCE,
                    "Not the same as resource of the SlingScriptHelper request");
            }

            if (writerObject != null
                && sling.getResponse().getWriter() != writerObject) {
                throw fail(OUT,
                    "Not the same as writer of the SlingScriptHelper response");
            }

        } else {

            throw fail(SLING, "Wrong type");

        }

        Object logObject = slingBindings.get(LOG);
        if (logObject == null) {
            logObject = LoggerFactory.getLogger(getLoggerName());
        } else if (!(logObject instanceof Logger)) {
            throw fail(LOG, "Wrong type");
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

    private ServletException fail(String variableName, String message) {
        return new ServletException(variableName + ": " + message);
    }

    private String getLoggerName() {
        String name = getScriptResource().getURI();
        name = name.replace('.', '$');
        name = name.replace('/', '.');
        return name;
    }
}