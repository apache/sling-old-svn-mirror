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

import static org.apache.sling.api.scripting.SlingBindings.FLUSH;
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.core.impl.helper.ProtectedBindings;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultSlingScript implements SlingScript, Servlet, ServletConfig {

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSlingScript.class);

    /** Thread local containing the resource resolver. */
    private static ThreadLocal<ResourceResolver> requestResourceResolver = new ThreadLocal<ResourceResolver>();

    /** The set of protected keys. */
    private static final Set<String> PROTECTED_KEYS =
        new HashSet<String>(Arrays.asList(REQUEST, RESPONSE, READER, SLING, RESOURCE, OUT, LOG));

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

    /** The cache for services. */
    private final ServiceCache cache;

    /**
     * Constructor
     * @param bundleContext The bundle context
     * @param scriptResource The script resource
     * @param scriptEngine The script engine
     * @param bindingsValuesProviders additional bindings values providers
     * @param cache serviceCache
     */
    DefaultSlingScript(final BundleContext bundleContext,
            final Resource scriptResource,
            final ScriptEngine scriptEngine,
            final Collection<BindingsValuesProvider> bindingsValuesProviders,
            final ServiceCache cache) {
        this.scriptResource = scriptResource;
        this.scriptEngine = scriptEngine;
        this.bundleContext = bundleContext;
        this.bindingsValuesProviders = bindingsValuesProviders;
        this.cache = cache;
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

    /**
     * @see org.apache.sling.api.scripting.SlingScript#getScriptResource()
     */
    public Resource getScriptResource() {
        final ResourceResolver resolver = requestResourceResolver.get();
        if ( resolver == null ) {
            // if we don't have a request resolver we directly return the script resource
            return scriptResource;
        }
        return new LazyScriptResource(this.scriptName,
                this.scriptResource.getResourceType(), resolver);
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScript#eval(org.apache.sling.api.scripting.SlingBindings)
     * @throws ScriptEvaluationException
     */
    public Object eval(SlingBindings props) {
        return this.call(props, null);
    }

    // ---------- Servlet interface --------------------------------------------
    private static final Integer[] SCOPES = { SlingScriptConstants.SLING_SCOPE, Integer.valueOf(100), Integer.valueOf(200) };

    /**
     * @see org.apache.sling.api.scripting.SlingScript#call(org.apache.sling.api.scripting.SlingBindings, java.lang.String, java.lang.Object[])
     * @throws ScriptEvaluationException
     */
    public Object call(SlingBindings props, String method, Object... args) {
        Bindings bindings = null;
        Reader reader = null;
        boolean disposeScriptHelper = !props.containsKey(SLING);
        ResourceResolver oldResolver = null;
        try {
            bindings = verifySlingBindings(props);

            // use final variable for inner class!
            final Bindings b = bindings;
            // create script context
            final ScriptContext ctx = new ScriptContext() {

                private Bindings globalScope;
                private Bindings engineScope = b;
                private Writer writer = (Writer) b.get(OUT);
                private Writer errorWriter = new LogWriter((Logger) b.get(LOG));
                private Reader reader = (Reader)b.get(READER);
                private Bindings slingScope = new SimpleBindings();


                /**
                 * @see javax.script.ScriptContext#setBindings(javax.script.Bindings, int)
                 */
                public void setBindings(final Bindings bindings, final int scope) {
                    switch (scope) {
                        case SlingScriptConstants.SLING_SCOPE : this.slingScope = bindings;
                                                                break;
                        case 100: if (bindings == null) throw new NullPointerException("Bindings for ENGINE scope is null");
                                  this.engineScope = bindings;
                                  break;
                        case 200: this.globalScope = bindings;
                                  break;
                        default: throw new IllegalArgumentException("Invaild scope");
                    }
                }

                /**
                 * @see javax.script.ScriptContext#getBindings(int)
                 */
                public Bindings getBindings(final int scope) {
                    switch (scope) {
                        case SlingScriptConstants.SLING_SCOPE : return slingScope;
                        case 100: return this.engineScope;
                        case 200: return this.globalScope;
                    }
                    throw new IllegalArgumentException("Invaild scope");
                }

                /**
                 * @see javax.script.ScriptContext#setAttribute(java.lang.String, java.lang.Object, int)
                 */
                public void setAttribute(final String name, final Object value, final int scope) {
                    if (name == null) throw new IllegalArgumentException("Name is null");
                    final Bindings bindings = getBindings(scope);
                    if (bindings != null) {
                        bindings.put(name, value);
                    }
                }

                /**
                 * @see javax.script.ScriptContext#getAttribute(java.lang.String, int)
                 */
                public Object getAttribute(final String name, final int scope) {
                    if (name == null) throw new IllegalArgumentException("Name is null");
                    final Bindings bindings = getBindings(scope);
                    if (bindings != null) {
                        return bindings.get(name);
                    }
                    return null;
                }

                /**
                 * @see javax.script.ScriptContext#removeAttribute(java.lang.String, int)
                 */
                public Object removeAttribute(final String name, final int scope) {
                    if (name == null) throw new IllegalArgumentException("Name is null");
                    final Bindings bindings = getBindings(scope);
                    if (bindings != null) {
                        return bindings.remove(name);
                    }
                    return null;
                }

                /**
                 * @see javax.script.ScriptContext#getAttribute(java.lang.String)
                 */
                public Object getAttribute(String name) {
                    if (name == null) throw new IllegalArgumentException("Name is null");
                    for (final int scope : SCOPES) {
                        final Bindings bindings = getBindings(scope);
                        if ( bindings != null ) {
                            final Object o = bindings.get(name);
                            if ( o != null ) {
                                return o;
                            }
                        }
                    }
                    return null;
                }

                /**
                 * @see javax.script.ScriptContext#getAttributesScope(java.lang.String)
                 */
                public int getAttributesScope(String name) {
                    if (name == null) throw new IllegalArgumentException("Name is null");
                    for (final int scope : SCOPES) {
                       if ((getBindings(scope) != null) && (getBindings(scope).containsKey(name))) {
                           return scope;
                       }
                    }
                    return -1;
                }

                /**
                 * @see javax.script.ScriptContext#getScopes()
                 */
                public List<Integer> getScopes() {
                    return Arrays.asList(SCOPES);
                }

                /**
                 * @see javax.script.ScriptContext#getWriter()
                 */
                public Writer getWriter() {
                    return this.writer;
                }

                /**
                 * @see javax.script.ScriptContext#getErrorWriter()
                 */
                public Writer getErrorWriter() {
                    return this.errorWriter;
                }

                /**
                 * @see javax.script.ScriptContext#setWriter(java.io.Writer)
                 */
                public void setWriter(Writer writer) {
                    this.writer = writer;
                }

                /**
                 * @see javax.script.ScriptContext#setErrorWriter(java.io.Writer)
                 */
                public void setErrorWriter(Writer writer) {
                    this.errorWriter = writer;
                }

                /**
                 * @see javax.script.ScriptContext#getReader()
                 */
                public Reader getReader() {
                    return this.reader;
                }

                /**
                 * @see javax.script.ScriptContext#setReader(java.io.Reader)
                 */
                public void setReader(Reader reader) {
                    this.reader = reader;
                }
            };

            // set the current resource resolver if a request is available from the bindings
            if ( props.getRequest() != null ) {
                oldResolver = requestResourceResolver.get();
                requestResourceResolver.set(props.getRequest().getResourceResolver());
            }

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
            Object flushObject = bindings.get(FLUSH);
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
            if ( props.getRequest() != null ) {
                requestResourceResolver.set(oldResolver);
            }

            // close the script reader (SLING-380)
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }

            // dispose of the SlingScriptHelper
            if ( bindings != null && disposeScriptHelper ) {
                final InternalScriptHelper helper = (InternalScriptHelper) bindings.get(SLING);
                if ( helper != null ) {
                    helper.cleanup();
                }
            }

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

            // try to set content type (unless included)
            if (request.getAttribute(SlingConstants.ATTR_INCLUDE_SERVLET_PATH) == null) {
                final String contentType = request.getResponseContentType();
                if (contentType != null) {
                    res.setContentType(contentType);

                    // only set the character encoding for text/ content types
                    // see SLING-679
                    if (contentType.startsWith("text/")) {
                        res.setCharacterEncoding("UTF-8");
                    }
                } else {
                    LOGGER.debug("service: No response content type defined for request {}.", request.getRequestURI());
                }
            } else {
                LOGGER.debug("service: Included request, not setting content type and encoding");
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
        final StringBuilder buffer = new StringBuilder(method);
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
            @Override
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

    private Bindings verifySlingBindings(final SlingBindings slingBindings) throws IOException {

    	final Bindings bindings = new SimpleBindings();

        final SlingHttpServletRequest request = slingBindings.getRequest();

        // check sling object
        Object slingObject = slingBindings.get(SLING);
        if (slingObject == null) {

            if ( request != null ) {
                slingObject = new InternalScriptHelper(this.bundleContext, this, request, slingBindings.getResponse(), this.cache);
            } else {
                slingObject = new InternalScriptHelper(this.bundleContext, this, this.cache);
            }
        } else if (!(slingObject instanceof SlingScriptHelper) ) {
            throw fail(SLING, "Wrong type");
        }
        final SlingScriptHelper sling = (SlingScriptHelper)slingObject;
        bindings.put(SLING, sling);

        if (request != null) {
        	final SlingHttpServletResponse response = slingBindings.getResponse();
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
            Set<String> protectedKeys = new HashSet<String>();
            protectedKeys.addAll(PROTECTED_KEYS);

            ProtectedBindings protectedBindings = new ProtectedBindings(bindings, protectedKeys);
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

    /**
     * This is a lazy implementation of the script resource which
     * just returns the path, resource type and resource resolver directly.
     */
    private static final class LazyScriptResource extends ResourceWrapper {

        private final String path;

        private final String resourceType;

        private final ResourceResolver resolver;

        private Resource delegatee;

        public LazyScriptResource(final String path, final String resourceType, final ResourceResolver resolver) {
            super(null);
            this.path = path;
            this.resourceType = resourceType;
            this.resolver = resolver;
        }

        @Override
        public Resource getResource() {
            if (this.delegatee == null) {
                this.delegatee = this.resolver.getResource(this.path);
                if (this.delegatee == null) {
                    this.delegatee = new SyntheticResource(resolver, this.path,
                        this.resourceType);
                }
            }
            return this.delegatee;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getPath()
         */
        @Override
        public String getPath() {
            return this.path;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceType()
         */
        @Override
        public String getResourceType() {
            return this.resourceType;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceResolver()
         */
        @Override
        public ResourceResolver getResourceResolver() {
            return this.resolver;
        }
    }
}