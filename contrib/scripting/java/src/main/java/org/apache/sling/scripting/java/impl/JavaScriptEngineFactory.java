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
package org.apache.sling.scripting.java.impl;

import static org.apache.sling.api.scripting.SlingBindings.SLING;

import java.io.IOException;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * The Java engine
 *
 */
@Component(metatype=true, label="%javahandler.name", description="%javahandler.description")
@Service(value=javax.script.ScriptEngineFactory.class)
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="Java Servlet Script Handler"),
    @Property(name=JavaScriptEngineFactory.PROPERTY_COMPILER_SOURCE_V_M, value=JavaScriptEngineFactory.DEFAULT_VM_VERSION),
    @Property(name=JavaScriptEngineFactory.PROPERTY_COMPILER_TARGET_V_M, value=JavaScriptEngineFactory.DEFAULT_VM_VERSION),
    @Property(name=JavaScriptEngineFactory.PROPERTY_CLASSDEBUGINFO, boolValue=true),
    @Property(name=JavaScriptEngineFactory.PROPERTY_ENCODING, value="UTF-8")
})
public class JavaScriptEngineFactory
    extends AbstractScriptEngineFactory
    implements EventHandler {

    public static final String PROPERTY_COMPILER_SOURCE_V_M = "java.compilerSourceVM";

    public static final String PROPERTY_COMPILER_TARGET_V_M = "java.compilerTargetVM";

    public static final String PROPERTY_CLASSDEBUGINFO = "java.classdebuginfo";

    public static final String PROPERTY_ENCODING = "java.javaEncoding";

    /** Default source and target VM version (value is "1.6"). */
    public static final String DEFAULT_VM_VERSION = "1.6";

    @Reference
    private JavaCompiler javaCompiler;

    @Reference
    private ServletContext slingServletContext;

    private SlingIOProvider ioProvider;

    private JavaServletContext javaServletContext;

    private ServletConfig servletConfig;

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
     * @see javax.script.ScriptEngineFactory#getParameter(String)
     */
    @Override
    public Object getParameter(String name) {
        if ("THREADING".equals(name)) {
            return "STATELESS";
        }

        return super.getParameter(name);
    }

    /**
     * Activate this engine
     *
     * @param componentContext
     */
    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext componentContext) {
        this.ioProvider = new SlingIOProvider(this.javaCompiler,
                                              CompilerOptions.createOptions(componentContext.getProperties()));
        this.javaServletContext = new JavaServletContext(ioProvider,
            slingServletContext);

        this.servletConfig = new JavaServletConfig(javaServletContext,
            componentContext.getProperties());

        // register event handler
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("event.topics","org/apache/sling/api/resource/*");
        props.put("service.description","Java Servlet Script Modification Handler");
        props.put("service.vendor","The Apache Software Foundation");

        this.eventHandlerRegistration = componentContext.getBundleContext()
                  .registerService(EventHandler.class.getName(), this, props);
    }

    /**
     * Deactivate this engine
     * @param componentContext
     */
    protected void deactivate(final ComponentContext componentContext) {
        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
        if ( this.ioProvider != null ) {
            this.ioProvider.destroy();
            this.ioProvider = null;
        }
        javaServletContext = null;
        servletConfig = null;
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
        // create a SlingBindings object
        final SlingBindings slingBindings = new SlingBindings();
        slingBindings.putAll(bindings);

        ResourceResolver resolver = (ResourceResolver) context.getAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                SlingScriptConstants.SLING_SCOPE);
        if ( resolver == null ) {
            resolver = scriptHelper.getScript().getScriptResource().getResourceResolver();
        }
        ioProvider.setRequestResourceResolver(resolver);

        final SlingHttpServletRequest request = slingBindings.getRequest();
        final Object oldValue = request.getAttribute(SlingBindings.class.getName());
        try {
            final ServletWrapper servlet = getWrapperAdapter(scriptHelper);

            request.setAttribute(SlingBindings.class.getName(), slingBindings);
            servlet.service(request, slingBindings.getResponse());
        } catch (SlingException se) {
            // rethrow as is
            throw se;
        } catch (IOException ioe) {
            throw new SlingIOException(ioe);
        } catch (ServletException se) {
            throw new SlingServletException(se);
        } catch (Exception ex) {
            throw new SlingException(null, ex);
        } finally {
            request.setAttribute(SlingBindings.class.getName(), oldValue);
            ioProvider.resetRequestResourceResolver();
        }
    }

    private ServletWrapper getWrapperAdapter(final SlingScriptHelper scriptHelper)
    throws SlingException {

        SlingScript script = scriptHelper.getScript();
        final String scriptName = script.getScriptResource().getPath();
        ServletWrapper wrapper = this.ioProvider.getServletCache().getWrapper(scriptName);
        if (wrapper != null) {
            return wrapper;
        }

        wrapper = new ServletWrapper(servletConfig,
                                     ioProvider,
                                     scriptName,
                                     scriptHelper);
        wrapper = this.ioProvider.getServletCache().addWrapper(scriptName, wrapper);

        return wrapper;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        if ( SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic()) ) {
            this.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH), false);
        } else if ( SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
            this.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH), true);
        }
    }

    private void handleModification(final String scriptName, final boolean remove) {
        this.ioProvider.getServletCache().removeWrapper(scriptName);
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
