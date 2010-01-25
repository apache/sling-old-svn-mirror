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
package org.apache.sling.scripting.core.impl;

import static org.apache.sling.api.scripting.SlingBindings.LOG;
import static org.apache.sling.api.scripting.SlingBindings.OUT;
import static org.apache.sling.api.scripting.SlingBindings.READER;
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
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.core.ScriptHelper;
import org.apache.sling.scripting.core.impl.helper.ProtectedBindings;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultSlingScript implements SlingScript, Servlet, ServletConfig {

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSlingScript.class);

    /** Thread local containing the resource resolver. */
    private static ThreadLocal<ResourceResolver> requestResourceResolver = new ThreadLocal<ResourceResolver>();

    /** The resource pointing to the script. */
    private final Resource scriptResource;

    /** The name of the script (the resource path) */
    private final String scriptName;

    /** The encoding of the script. */
    private final String scriptEncoding;

    /** The script engine for this script. */
    private final ScriptEngine scriptEngine;

    /** The servlet context. */
    private ServletContext servletContext;

    /** The init parameters for this servlet. */
    private Dictionary<String, String> initParameters;

    /** The current bundle context. */
    private final BundleContext bundleContext;

    /** The ScriptBindingsValuesProviders. */
    private final Collection<BindingsValuesProvider> bindingsValuesProviders;

    /**
     * Constructor
     * @param bundleContext The bundle context
     * @param scriptResource The script resource
     * @param scriptEngine The script engine
     */
    DefaultSlingScript(BundleContext bundleContext, Resource scriptResource, ScriptEngine scriptEngine,
            Collection<BindingsValuesProvider> bindingsValuesProviders) {
        this.scriptResource = scriptResource;
        this.scriptEngine = scriptEngine;
        this.bundleContext = bundleContext;
        this.bindingsValuesProviders = bindingsValuesProviders;
        this.scriptName = this.scriptResource.getPath();
        // Now know how to get the input stream, we still have to decide
        // on the encoding of the stream's data. Primarily we assume it is
        // UTF-8, which is a default in many places in JCR. Secondarily
        // we try to get a jcr:encoding property besides the data property
        // to provide a possible encoding
        final ResourceMetadata meta = this.scriptResource.getResourceMetadata();
        String encoding = meta.getCharacterEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        this.scriptEncoding = encoding;
    }

    // ---------- SlingScript interface ----------------------------------------

    public Resource getScriptResource() {
        final ResourceResolver resolver = requestResourceResolver.get();
        if ( resolver == null ) {
            // if we don't have a request resolver we directly return the script resource
            return scriptResource;
        }
        Resource rsrc = resolver.getResource(this.scriptName);
        if ( rsrc == null ) {
            rsrc = new SyntheticResource(resolver, this.scriptName, this.scriptResource.getResourceType());
        }
        return rsrc;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScript#eval(org.apache.sling.api.scripting.SlingBindings)
     * @throws ScriptEvaluationException
     */
    public Object eval(SlingBindings props) {
        return this.call(props, null);
    }

    // ---------- Servlet interface --------------------------------------------

    /**
     * @see org.apache.sling.api.scripting.SlingScript#call(org.apache.sling.api.scripting.SlingBindings, java.lang.String, java.lang.Object[])
     * @throws ScriptEvaluationException
     */
    public Object call(SlingBindings props, String method, Object... args) {
        Bindings bindings = null;
        Reader reader = null;
        try {
            bindings = verifySlingBindings(props);

            final ScriptContext ctx = new SimpleScriptContext() {

                private final Map<String, Object> slingScope = new HashMap<String, Object>();

                @Override
                public Object getAttribute(String name, int scope) {
                    if ( scope == SlingScriptConstants.SLING_SCOPE ) {
                        return slingScope.get(name);
                    }
                    return super.getAttribute(name, scope);
                }

                @Override
                public Object removeAttribute(String name, int scope) {
                    if ( scope == SlingScriptConstants.SLING_SCOPE ) {
                        return slingScope.remove(name);
                    }
                    return super.removeAttribute(name, scope);
                }

                @Override
                public void setAttribute(String name, Object value, int scope) {
                    if ( scope == SlingScriptConstants.SLING_SCOPE ) {
                        slingScope.put(name, value);
                        return;
                    }
                    super.setAttribute(name, value, scope);
                }

            };
            ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            ctx.setReader((Reader) bindings.get(READER));
            ctx.setWriter((Writer) bindings.get(OUT));
            ctx.setErrorWriter(new LogWriter((Logger) bindings.get(LOG)));

            // set the current resource resolver
            requestResourceResolver.set(props.getRequest().getResourceResolver());

            // set the script resource resolver as an attribute
            ctx.setAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                    this.scriptResource.getResourceResolver(), SlingScriptConstants.SLING_SCOPE);

            reader = getScriptReader();
            if ( method != null && !(this.scriptEngine instanceof Invocable)) {
                reader = getWrapperReader(reader, method, args);
            }

            // evaluate the script
            final Object result = scriptEngine.eval(reader, ctx);

            // call method - if supplied and script engine supports direct invocation
            if ( method != null && (this.scriptEngine instanceof Invocable)) {
                try {
                    ((Invocable)scriptEngine).invokeFunction(method, Arrays.asList(args).toArray());
                } catch (NoSuchMethodException e) {
                    throw new ScriptEvaluationException(this.scriptName, "Method " + method + " not found in script.", e);
                }
            }
            // optionall flush the output channel
            Object flushObject = bindings.get(SlingBindings.FLUSH);
            if (flushObject instanceof Boolean && (Boolean) flushObject) {
                ctx.getWriter().flush();
            }

            // allways flush the error channel
            ctx.getErrorWriter().flush();

            return result;

        } catch (IOException ioe) {
            throw new ScriptEvaluationException(this.scriptName, ioe.getMessage(),
                ioe);

        } catch (ScriptException se) {
            Throwable cause = (se.getCause() == null) ? se : se.getCause();
            throw new ScriptEvaluationException(this.scriptName, se.getMessage(),
                cause);

        } finally {
            // dispose of the SlingScriptHelper
            if ( bindings != null ) {
                final SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SLING);
                if ( helper != null ) {
                    helper.dispose();
                }
            }

            // close the script reader (SLING-380)
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
            requestResourceResolver.remove();
        }
    }

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig servletConfig) {
        if (servletConfig != null) {
            final Dictionary<String, String> params = new Hashtable<String, String>();
            for (Enumeration<?> ne = servletConfig.getInitParameterNames(); ne.hasMoreElements();) {
                String name = String.valueOf(ne.nextElement());
                String value = servletConfig.getInitParameter(name);
                params.put(name, value);
            }
            this.initParameters = params;
            this.servletContext = servletConfig.getServletContext();
        }
    }

    /**
     * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service(ServletRequest req, ServletResponse res) {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) req;

        try {
            // prepare the properties for the script
            final SlingBindings props = new SlingBindings();
            props.setRequest((SlingHttpServletRequest) req);
            props.setResponse((SlingHttpServletResponse) res);

            // try to set content type
            final String contentType = request.getResponseContentType();
            if (contentType != null) {
                res.setContentType(contentType);

                // only set the character encoding for text/ content types
                // see SLING-679
                if (contentType.startsWith("text/")) {
                    res.setCharacterEncoding("UTF-8");
                }
            } else {
                LOGGER.debug(
                    "service:No response content type defined for request {}.",
                    request.getRequestURI());
            }

            // evaluate the script now using the ScriptEngine
            eval(props);

        } catch (ScriptEvaluationException see) {

            // log in the request progress tracker
            logScriptError(request, see);

            throw see;
        } catch (SlingException e) {
            // log in the request progress tracker
            logScriptError(request, e);

            throw e;
        } catch (Exception e) {

            // log in the request progress tracker
            logScriptError(request, e);

            throw new SlingException("Cannot get DefaultSlingScript: "
                + e.getMessage(), e);
        }
    }

    public ServletConfig getServletConfig() {
        return this;
    }

    public String getServletInfo() {
        return "Script " + scriptName;
    }

    /**
     * @see javax.servlet.Servlet#destroy()
     */
    public void destroy() {
        initParameters = null;
        servletContext = null;
    }

    // ---------- ServletConfig ------------------------------------------------

    /**
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        final Dictionary<String, String> params = initParameters;
        return (params != null) ? params.get(name) : null;
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    public Enumeration<String> getInitParameterNames() {
        final Dictionary<String, String> params = initParameters;
        return (params != null) ? params.keys() : null;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletName()
     */
    public String getServletName() {
        return this.scriptName;
    }

    // ---------- internal -----------------------------------------------------

    private Reader getScriptReader() throws IOException {
        // access the value as a stream and return a buffered reader
        // converting the stream data using UTF-8 encoding, which is
        // the default encoding used
        return new BufferedReader(new InputStreamReader(new LazyInputStream(this.scriptResource), this.scriptEncoding));
    }

    private Reader getWrapperReader(final Reader scriptReader, final String method, final Object... args) {
        final StringBuffer buffer = new StringBuffer(method);
        buffer.append('(');
        for(Object o : args) {
            buffer.append('"');
            buffer.append(o);
            buffer.append('"');
        }
        buffer.append(')');
        final String msg = buffer.toString();
        return new Reader() {

            protected boolean doAppend = false;

            protected StringReader methodReader = new StringReader(msg);
            /**
             * @see java.io.Reader#close()
             */
            public void close() throws IOException {
                scriptReader.close();
            }

            @Override
            public int read(char[] cbuf, int start, int len) throws IOException {
                if ( doAppend ) {
                    return methodReader.read(cbuf, start, len);
                }
                int readLen = scriptReader.read(cbuf, start, len);
                if ( readLen == -1 ) {
                    doAppend = true;
                    return this.read(cbuf, start, len);
                }
                return readLen;
            }

            @Override
            public int read() throws IOException {
                if ( doAppend ) {
                    return methodReader.read();
                }
                int value = scriptReader.read();
                if ( value == -1 ) {
                    doAppend = true;
                    return methodReader.read();
                }
                return value;
            }

            @Override
            public int read(char[] cbuf) throws IOException {
                return this.read(cbuf, 0, cbuf.length);
            }

            @Override
            public boolean ready() throws IOException {
                return scriptReader.ready();
            }
        };
    }

    private Bindings verifySlingBindings(SlingBindings slingBindings) throws IOException {

    	Bindings bindings = new SimpleBindings();

        final SlingHttpServletRequest request = slingBindings.getRequest();

        // check sling object
        Object slingObject = slingBindings.get(SLING);
        if (slingObject == null) {

            if ( request != null ) {
                slingObject = new ScriptHelper(this.bundleContext, this, request, slingBindings.getResponse());
            } else {
                slingObject = new ScriptHelper(this.bundleContext, this);
            }
        } else if (!(slingObject instanceof SlingScriptHelper) ) {
            throw fail(SLING, "Wrong type");
        }
        final SlingScriptHelper sling = (SlingScriptHelper)slingObject;
        bindings.put(SLING, sling);

        if (request != null) {
            //throw fail(REQUEST, "Missing or wrong type");

        	SlingHttpServletResponse response = slingBindings.getResponse();
            if (response == null) {
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

            // if there is a provided sling script helper, check arguments
            if (slingBindings.get(SLING) != null) {

                if (sling.getRequest() != request) {
                    throw fail(REQUEST,
                        "Not the same as request field of SlingScriptHelper");
                }

                if (sling.getResponse() != response) {
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
            }

            // set base variables when executing inside a request
            bindings.put(REQUEST, sling.getRequest());
            bindings.put(READER, sling.getRequest().getReader());
            bindings.put(RESPONSE, sling.getResponse());
            bindings.put(RESOURCE, sling.getRequest().getResource());
            bindings.put(OUT, sling.getResponse().getWriter());
        }

        Object logObject = slingBindings.get(LOG);
        if (logObject == null) {
            logObject = LoggerFactory.getLogger(getLoggerName());
        } else if (!(logObject instanceof Logger)) {
            throw fail(LOG, "Wrong type");
        }
        bindings.put(LOG, logObject);

        // copy non-base variables
        for (Map.Entry<String, Object> entry : slingBindings.entrySet()) {
            if (!bindings.containsKey(entry.getKey())) {
                bindings.put(entry.getKey(), entry.getValue());
            }
        }

        if (!bindingsValuesProviders.isEmpty()) {
            ProtectedBindings protectedBindings = new ProtectedBindings(bindings);
            for (BindingsValuesProvider provider : bindingsValuesProviders) {
                provider.addBindings(protectedBindings);
            }
        }

        return bindings;
    }

    private ScriptEvaluationException fail(String variableName, String message) {
        return new ScriptEvaluationException(this.scriptName, variableName + ": "
            + message);
    }

    private String getLoggerName() {
        String name = scriptName;
        name = name.substring(1);       // cut-off leading slash
        name = name.replace('.', '$');  // extension separator as part of name
        name = name.replace('/', '.');  // hierarchy defined by dot
        return name;
    }

    /**
     * Logs the error caused by executing the script in the request progress
     * tracker.
     */
    private void logScriptError(SlingHttpServletRequest request,
            Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null) {
            message = throwable.getMessage().replace('\n', '/');
        } else {
            message = throwable.toString();
        }
        request.getRequestProgressTracker().log("SCRIPT ERROR: {0}", message);
    }

    /**
     * Input stream wrapper which acquires the underlying input stream lazily.
     * This ensures that the input stream is only fetched from the repository
     * if it is really used by the script engines.
     */
    public final static class LazyInputStream extends InputStream {

        /** The script resource which is adapted to an inputm stream. */
        private final Resource resource;

        /** The input stream created on demand, null if not used */
        private InputStream delegatee;

        public LazyInputStream(final Resource resource) {
            this.resource = resource;
        }

        /**
         * Closes the input stream if acquired otherwise does nothing.
         */
        @Override
        public void close() throws IOException {
            if (delegatee != null) {
                delegatee.close();
            }
        }

        @Override
        public int available() throws IOException {
            return getStream().available();
        }

        @Override
        public int read() throws IOException {
            return getStream().read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return getStream().read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getStream().skip(n);
        }

        @Override
        public boolean markSupported() {
            try {
                return getStream().markSupported();
            } catch (IOException ioe) {
                // ignore
            }
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                getStream().mark(readlimit);
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            getStream().reset();
        }

        /** Actually retrieves the input stream from the underlying JCR Value */
        private InputStream getStream() throws IOException {
            if (delegatee == null) {
                delegatee = this.resource.adaptTo(InputStream.class);
                if (delegatee == null) {
                    throw new IOException("Cannot get a stream to the script resource "
                        + this.resource);
                }
            }
            return delegatee;
        }

    }
}