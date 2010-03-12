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
package org.apache.sling.scripting.java;

import static org.apache.sling.api.scripting.SlingBindings.SLING;

import java.io.Reader;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Java engine
 *
 * @scr.component label="%javahandler.name" description="%javahandler.description"
 * @scr.property name="service.description" value="Java Servlet Script Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="javax.script.ScriptEngineFactory"
 *
 * @scr.property name="java.javaEncoding" value="UTF-8"
 * @scr.property name="java.compilerSourceVM" value="1.5"
 * @scr.property name="java.compilerTargetVM" value="1.5"
 * @scr.property name="java.classdebuginfo" value="true" type="Boolean"
 */
public class JavaScriptEngineFactory
    extends AbstractScriptEngineFactory
    implements EventHandler {

    /** default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @scr.reference
     */
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    /**
     * The class loader
     */
    private ClassLoader javaClassLoader;

    /** @scr.reference */
    private ServletContext slingServletContext;

    /** @scr.reference */
    private ClassLoaderWriter classLoaderWriter;

    private SlingIOProvider ioProvider;

    private JavaServletContext javaServletContext;

    private ServletConfig servletConfig;

    private ServletCache servletCache;

    /** Compiler options. */
    private Options compilerOptions;

    private ServiceRegistration eventHandlerRegistration;

    public static final String SCRIPT_TYPE = "java";

    /**
     * Constructor
     */
    public JavaScriptEngineFactory() {
        setExtensions(SCRIPT_TYPE);
    }

    /**
     * @see javax.script.ScriptEngineFactory#getScriptEngine()
     */
    public ScriptEngine getScriptEngine() {
        return new JavaScriptEngine(this);
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageName()
     */
    public String getLanguageName() {
        return "Java Servlet Compiler";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageVersion()
     */
    public String getLanguageVersion() {
        return "1.5";
    }

    /**
     * Activate this engine
     * @param componentContext
     */
    protected void activate(final ComponentContext componentContext) {
        this.ioProvider = new SlingIOProvider(this.classLoaderWriter);
        this.servletCache = new ServletCache();

        this.javaServletContext = new JavaServletContext(ioProvider,
            slingServletContext);

        this.servletConfig = new JavaServletConfig(javaServletContext,
            componentContext.getProperties());
        this.compilerOptions = new Options(componentContext,
                                           this.javaClassLoader);
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("event.topics","org/apache/sling/api/resource/*");
        props.put("service.description","Java Servlet Script Modification Handler");
        props.put("service.vendor","The Apache Software Foundation");

        this.eventHandlerRegistration = componentContext.getBundleContext()
                  .registerService(EventHandler.class.getName(), this, props);
        if (logger.isDebugEnabled()) {
            logger.debug("JavaServletScriptEngine.activate()");
        }
    }

    /**
     * Deactivate this engine
     * @param componentContext
     */
    protected void deactivate(final ComponentContext componentContext) {
        if (logger.isDebugEnabled()) {
            logger.debug("JavaServletScriptEngine.deactivate()");
        }

        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
        if ( this.servletCache != null ) {
            this.servletCache.destroy();
            this.servletCache = null;
        }
        ioProvider = null;
        javaServletContext = null;
        servletConfig = null;
        compilerOptions = null;
    }

    /**
     * Call the servlet.
     * @param binding The bindings for the script invocation
     * @param scriptHelper The script helper.
     * @param context The script context.
     * @throws SlingServletException
     * @throws SlingIOException
     */
    private void callServlet(final Bindings bindings,
                             final SlingScriptHelper scriptHelper,
                             final ScriptContext context) {
        ResourceResolver resolver = (ResourceResolver) context.getAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                SlingScriptConstants.SLING_SCOPE);
        if ( resolver == null ) {
            resolver = scriptHelper.getScript().getScriptResource().getResourceResolver();
        }
        ioProvider.setRequestResourceResolver(resolver);
        try {
            final ServletWrapper servlet = getWrapperAdapter(scriptHelper);
            // create a SlingBindings object
            final SlingBindings slingBindings = new SlingBindings();
            slingBindings.putAll(bindings);
            servlet.service(slingBindings);
        } finally {
            ioProvider.resetRequestResourceResolver();
        }
    }

    private ServletWrapper getWrapperAdapter(final SlingScriptHelper scriptHelper)
    throws SlingException {

        SlingScript script = scriptHelper.getScript();
        final String scriptName = script.getScriptResource().getPath();
        ServletWrapper wrapper = this.servletCache.getWrapper(scriptName);
        if (wrapper != null) {
            return wrapper;
        }

        synchronized (this) {
            wrapper = this.servletCache.getWrapper(scriptName);
            if (wrapper != null) {
                return wrapper;
            }

            wrapper = new ServletWrapper(servletConfig,
                    this.compilerOptions, ioProvider, scriptName, this.servletCache);
            this.servletCache.addWrapper(scriptName, wrapper);

            return wrapper;
        }
    }

    /**
     * Bind the class load provider.
     * @param repositoryClassLoaderProvider the new provider
     */
    protected void bindDynamicClassLoaderManager(DynamicClassLoaderManager rclp) {
        if ( this.javaClassLoader != null ) {
            this.ungetClassLoader();
        }
        this.getClassLoader(rclp);
    }

    /**
     * Unbind the class loader provider.
     * @param repositoryClassLoaderProvider the old provider
     */
    protected void unbindDynamicClassLoaderManager(DynamicClassLoaderManager rclp) {
        if ( this.dynamicClassLoaderManager == rclp ) {
            this.ungetClassLoader();
        }
    }

    /**
     * Get the class loader
     */
    private void getClassLoader(DynamicClassLoaderManager rclp) {
        this.dynamicClassLoaderManager = rclp;
        this.javaClassLoader = rclp.getDynamicClassLoader();
    }

    /**
     * Unget the class loader
     */
    private void ungetClassLoader() {
        this.dynamicClassLoaderManager = null;
        this.javaClassLoader = null;
    }
    // ---------- Internal -----------------------------------------------------

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        if ( SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic()) ) {
            this.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH));
        } else if ( SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
            this.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH));
        }
    }

    private void handleModification(final String scriptName) {
        final ServletWrapper wrapper = this.servletCache.getWrapper(scriptName);
        if ( wrapper != null ) {
            wrapper.getCompilationContext().clearLastModificationTest();
        }
    }

    private static class JavaScriptEngine extends AbstractSlingScriptEngine {

        JavaScriptEngine(JavaScriptEngineFactory factory) {
            super(factory);
        }

        /**
         * @see javax.script.ScriptEngine#eval(java.io.Reader, javax.script.ScriptContext)
         */
        public Object eval(Reader script, ScriptContext context)
        throws ScriptException {
            final Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
            final SlingScriptHelper scriptHelper = (SlingScriptHelper) props.get(SLING);
            if (scriptHelper != null) {
                ((JavaScriptEngineFactory)this.getFactory()).callServlet(props, scriptHelper, context);
            }
            return null;
        }
    }
}
