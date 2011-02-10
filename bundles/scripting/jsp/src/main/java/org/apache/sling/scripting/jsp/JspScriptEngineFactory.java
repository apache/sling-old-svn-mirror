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
package org.apache.sling.scripting.jsp;

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
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
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
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.runtime.AnnotationProcessor;
import org.apache.sling.scripting.jsp.jasper.runtime.JspApplicationContextImpl;
import org.apache.sling.scripting.jsp.util.TagUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JSP engine (a.k.a Jasper).
 *
 */
@Component(label="%jsphandler.name",
           description="%jsphandler.description",
           metatype=true)
@Service(value=javax.script.ScriptEngineFactory.class)
@Properties({
   @Property(name="service.description",value="JSP Script Handler"),
   @Property(name="service.vendor",value="The Apache Software Foundation"),
   @Property(name="jasper.classdebuginfo",boolValue=true),
   @Property(name="jasper.enablePooling",boolValue=true),
   @Property(name="jasper.ieClassId",value="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"),
   @Property(name="jasper.genStringAsCharArray",boolValue=false),
   @Property(name="jasper.keepgenerated",boolValue=true),
   @Property(name="jasper.mappedfile",boolValue=true),
   @Property(name="jasper.trimSpaces",boolValue=false),
   @Property(name="jasper.displaySourceFragments",boolValue=false)
})
public class JspScriptEngineFactory
    extends AbstractScriptEngineFactory
    implements EventHandler {

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(JspScriptEngineFactory.class);

    @Reference(unbind="unbindSlingServletContext")
    private ServletContext slingServletContext;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private ClassLoaderWriter classLoaderWriter;

    /** The class loader for the jsps. */
    private ClassLoader jspClassLoader;

    /** The io provider for reading and writing. */
    private SlingIOProvider ioProvider;

    private SlingTldLocationsCache tldLocationsCache;

    private JspRuntimeContext jspRuntimeContext;

    private Options options;

    private JspServletContext jspServletContext;

    private ServletConfig servletConfig;

    private ServiceRegistration eventHandlerRegistration;

    /** The handler for the jsp factories. */
    private JspRuntimeContext.JspFactoryHandler jspFactoryHandler;

    public static final String[] SCRIPT_TYPE = { "jsp", "jspf", "jspx" };

    public static final String[] NAMES = { "jsp", "JSP" };

    public JspScriptEngineFactory() {
        setExtensions(SCRIPT_TYPE);
        setNames(NAMES);
    }

    /**
     * @see javax.script.ScriptEngineFactory#getScriptEngine()
     */
    public ScriptEngine getScriptEngine() {
        return new JspScriptEngine();
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageName()
     */
    public String getLanguageName() {
        return "Java Server Pages";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageVersion()
     */
    public String getLanguageVersion() {
        return "2.1";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getParameter(String)
     */
    public Object getParameter(String name) {
        if ("THREADING".equals(name)) {
            return "STATELESS";
        }

        return super.getParameter(name);
    }

    /**
     * @param scriptHelper
     * @throws SlingServletException
     * @throws SlingIOException
     */
    private void callJsp(final Bindings bindings,
                         final SlingScriptHelper scriptHelper,
                         final ScriptContext context) {

        ResourceResolver resolver = (ResourceResolver) context.getAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                SlingScriptConstants.SLING_SCOPE);
        if ( resolver == null ) {
            resolver = scriptHelper.getScript().getScriptResource().getResourceResolver();
        }
        final SlingIOProvider io = this.ioProvider;
        io.setRequestResourceResolver(resolver);
        jspFactoryHandler.incUsage();
        try {
            final JspServletWrapperAdapter jsp = getJspWrapperAdapter(scriptHelper);
            // create a SlingBindings object
            final SlingBindings slingBindings = new SlingBindings();
            slingBindings.putAll(bindings);
            jsp.service(slingBindings);
        } finally {
            jspFactoryHandler.decUsage();
            io.resetRequestResourceResolver();
        }
    }

    private JspServletWrapperAdapter getJspWrapperAdapter(final SlingScriptHelper scriptHelper)
    throws SlingException {
        final JspRuntimeContext rctxt = jspRuntimeContext;

        final SlingScript script = scriptHelper.getScript();
        final String scriptName = script.getScriptResource().getPath();
        JspServletWrapperAdapter wrapper = (JspServletWrapperAdapter) rctxt.getWrapper(scriptName);
        if (wrapper != null) {
            return wrapper;
        }

        synchronized (rctxt) {
            wrapper = (JspServletWrapperAdapter) rctxt.getWrapper(scriptName);
            if (wrapper != null) {
                return wrapper;
            }

            try {

                wrapper = new JspServletWrapperAdapter(servletConfig, options,
                    scriptName, false, rctxt);
                rctxt.addWrapper(scriptName, wrapper);

                return wrapper;
            } catch (JasperException je) {
                if (je.getCause() != null) {
                    throw new SlingException(je.getMessage(), je.getCause());
                }
                throw new SlingException("Cannot create JSP", je);
            }
        }
    }

    // ---------- SCR integration ----------------------------------------------

    /**
     * Activate this component
     */
    protected void activate(final ComponentContext componentContext) {
        // set the current class loader as the thread context loader for
        // the setup of the JspRuntimeContext
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(jspClassLoader);

        try {
            this.jspFactoryHandler = JspRuntimeContext.initFactoryHandler();

            this.tldLocationsCache = new SlingTldLocationsCache(componentContext.getBundleContext());

            // prepare some classes
            ioProvider = new SlingIOProvider(classLoaderWriter);

            // return options which use the jspClassLoader
            options = new JspServletOptions(slingServletContext, ioProvider,
                componentContext, jspClassLoader, tldLocationsCache);

            // Initialize the JSP Runtime Context
            this.jspRuntimeContext = new JspRuntimeContext(slingServletContext,
                    options);

                // by default access the repository
            this.jspRuntimeContext.setIOProvider(ioProvider);

            jspServletContext = new JspServletContext(ioProvider,
                slingServletContext, tldLocationsCache);

            servletConfig = new JspServletConfig(jspServletContext,
                componentContext.getProperties());

        } finally {
            // make sure the context loader is reset after setting up the
            // JSP runtime context
            Thread.currentThread().setContextClassLoader(old);
        }

        // register event handler
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("event.topics","org/apache/sling/api/resource/*");
        props.put("service.description","JSP Script Modification Handler");
        props.put("service.vendor","The Apache Software Foundation");

        this.eventHandlerRegistration = componentContext.getBundleContext()
                  .registerService(EventHandler.class.getName(), this, props);

        logger.debug("IMPORTANT: Do not modify the generated servlets");
    }

    /**
     * Activate this component
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("JspScriptEngine.deactivate()");

        if ( this.tldLocationsCache != null ) {
            this.tldLocationsCache.deactivate(componentContext.getBundleContext());
            this.tldLocationsCache = null;
        }
        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
        if (jspRuntimeContext != null) {
            try {
                jspRuntimeContext.destroy();
            } catch (NullPointerException npe) {
                // SLING-530, might be thrown on system shutdown in a servlet
                // container when using the Equinox servlet container bridge
                logger.debug("deactivate: ServletContext might already be unavailable", npe);
            }
            jspRuntimeContext = null;
        }

        ioProvider = null;
        this.jspFactoryHandler.destroy();
        this.jspFactoryHandler = null;
    }

    /**
     * Unbinds the Sling ServletContext and removes any known servlet context
     * attributes preventing the bundles's class loader from being collected.
     *
     * @param slingServletContext The <code>ServletContext</code> to be unbound
     */
    protected void unbindSlingServletContext(
            final ServletContext slingServletContext) {

        // remove JspApplicationContextImpl from the servlet context,
        // otherwise a ClassCastException may be caused after this component
        // is recreated because the class loader of the
        // JspApplicationContextImpl class object is different from the one
        // stored in the servlet context same for the AnnotationProcessor
        // (which generally does not exist here)
        try {
            if (slingServletContext != null) {
                slingServletContext.removeAttribute(JspApplicationContextImpl.class.getName());
                slingServletContext.removeAttribute(AnnotationProcessor.class.getName());
            }
        } catch (NullPointerException npe) {
            // SLING-530, might be thrown on system shutdown in a servlet
            // container when using the Equinox servlet container bridge
            logger.debug(
                "unbindSlingServletContext: ServletContext might already be unavailable",
                npe);
        }

        if (this.slingServletContext == slingServletContext) {
            this.slingServletContext = null;
        }
    }

    /**
     * Bind the class load provider.
     *
     * @param repositoryClassLoaderProvider the new provider
     */
    protected void bindDynamicClassLoaderManager(final DynamicClassLoaderManager rclp) {
        if ( this.jspClassLoader != null ) {
            this.ungetClassLoader();
        }
        this.getClassLoader(rclp);
    }

    /**
     * Unbind the class loader provider.
     * @param repositoryClassLoaderProvider the old provider
     */
    protected void unbindDynamicClassLoaderManager(final DynamicClassLoaderManager rclp) {
        if ( this.dynamicClassLoaderManager == rclp ) {
            this.ungetClassLoader();
        }
    }

    /**
     * Get the class loader
     */
    private void getClassLoader(final DynamicClassLoaderManager rclp) {
        this.dynamicClassLoaderManager = rclp;
        this.jspClassLoader = rclp.getDynamicClassLoader();
    }

    /**
     * Unget the class loader
     */
    private void ungetClassLoader() {
        this.jspClassLoader = null;
        this.dynamicClassLoaderManager = null;
    }

    // ---------- Internal -----------------------------------------------------

    private class JspScriptEngine extends AbstractSlingScriptEngine {

        JspScriptEngine() {
            super(JspScriptEngineFactory.this);
        }

        public Object eval(Reader script, ScriptContext context)
                throws ScriptException {
            Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
            SlingScriptHelper scriptHelper = (SlingScriptHelper) props.get(SLING);
            if (scriptHelper != null) {

                // set the current class loader as the thread context loader for
                // the compilation and execution of the JSP script
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(jspClassLoader);

                try {
                    callJsp(props, scriptHelper, context);
                } catch (SlingServletException e) {
                    // ServletExceptions use getRootCause() instead of getCause(),
                    // so we have to extract the actual root cause and pass it as
                    // cause in our new ScriptException
                    if (e.getCause() != null) {
                        // SlingServletException always wraps ServletExceptions
                        Throwable rootCause = TagUtil.getRootCause((ServletException) e.getCause());
                        // the ScriptException unfortunately does not accept a Throwable as cause,
                        // but only a Exception, so we have to wrap it with a dummy Exception in Throwable cases
                        if (rootCause instanceof Exception) {
                            throw new BetterScriptException(rootCause.toString(), (Exception) rootCause);
                        }
                        throw new BetterScriptException(rootCause.toString(),
                                new Exception("Wrapping Throwable: " + rootCause.toString(), rootCause));
                    }

                    // fallback to standard behaviour
                    throw new BetterScriptException(e.getMessage(), e);

                } catch (Exception e) {

                    throw new BetterScriptException(e.getMessage(), e);

                } finally {

                    // make sure the context loader is reset after setting up the
                    // JSP runtime context
                    Thread.currentThread().setContextClassLoader(old);

                }
            }
            return null;
        }
    }

    /**
     * Fixes {@link ScriptException} that overwrites the
     * {@link ScriptException#getMessage()} method to display its own
     * <code>message</code> instead of the <code>detailMessage</code>
     * defined in {@link Throwable}. Unfortunately using the constructor
     * {@link ScriptException#ScriptException(Exception)} does not set the
     * <code>message</code> member of {@link ScriptException}, which leads to
     * a message of <code>"null"</code>, effectively supressing the detailed
     * information of the cause. This class provides a way to do that explicitly
     * with a new constructor accepting both a message and a causing exception.
     *
     */
    private static class BetterScriptException extends ScriptException {

        private static final long serialVersionUID = -6490165487977283019L;

        public BetterScriptException(String message, Exception cause) {
            super(message);
            this.initCause(cause);
        }

    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        if ( SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic()) ) {
            this.jspRuntimeContext.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH));
        } else if ( SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
            this.jspRuntimeContext.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH));
        } else {
            this.jspRuntimeContext.handleModification((String)event.getProperty(SlingConstants.PROPERTY_PATH));
        }
    }
}
