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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.ClassLoaderWriterListener;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext.JspFactoryHandler;
import org.apache.sling.scripting.jsp.jasper.runtime.AnnotationProcessor;
import org.apache.sling.scripting.jsp.jasper.runtime.JspApplicationContextImpl;
import org.apache.sling.scripting.jsp.jasper.servlet.JspServletWrapper;
import org.apache.sling.scripting.jsp.util.TagUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JSP engine (a.k.a Jasper).
 *
 */
@Component(service = {javax.script.ScriptEngineFactory.class,ResourceChangeListener.class,ClassLoaderWriterListener.class},
           property = {
                   "extensions=jsp",
                   "extensions=jspf",
                   "extensions=jspx",
                   "names=jsp",
                   "names=JSP",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
                   Constants.SERVICE_DESCRIPTION + "=JSP Script Handler",
                   ResourceChangeListener.CHANGES + "=CHANGED",
                   ResourceChangeListener.CHANGES + "=REMOVED",
                   ResourceChangeListener.PATHS + "=glob:**/*.jsp",
                   ResourceChangeListener.PATHS + "=glob:**/*.jspf",
                   ResourceChangeListener.PATHS + "=glob:**/*.jspx",
                   ResourceChangeListener.PATHS + "=glob:**/*.tld",
                   ResourceChangeListener.PATHS + "=glob:**/*.tag"
           })
@Designate(ocd = JspScriptEngineFactory.Config.class)
public class JspScriptEngineFactory
    extends AbstractScriptEngineFactory
    implements ResourceChangeListener,ExternalResourceChangeListener, ClassLoaderWriterListener {

    @ObjectClassDefinition(name = "Apache Sling JSP Script Handler",
            description = "The JSP Script Handler supports development of JSP " +
                 "scripts to render response content on behalf of ScriptComponents. Internally " +
                 "Jasper 6.0.14 JSP Engine is used together with the Eclipse Java Compiler to " +
                 "compile generated Java code into Java class files. Some settings of Jasper " +
                 "may be configured as shown below. Note that JSP scripts are expected in the " +
                 "JCR repository and generated Java source and class files will be written to " +
                 "the JCR repository below the configured Compilation Location.")
    public @interface Config {

        @AttributeDefinition(name = "Target Version",
                description = "The target JVM version for the compiled classes. If " +
                              "left empty, the default version, 1.6., is used. If the value \"auto\" is used, the " +
                              "current vm version will be used.")
        String jasper_compilerTargetVM() default JspServletOptions.AUTOMATIC_VERSION;

        @AttributeDefinition(name = "Source Version",
                description = "The JVM version for the java/JSP source. If " +
                              "left empty, the default version, 1.6., is used. If the value \"auto\" is used, the " +
                              "current vm version will be used.")
        String jasper_compilerSourceVM() default JspServletOptions.AUTOMATIC_VERSION;

        @AttributeDefinition(name = "Generate Debug Info",
                description = "Should the class file be compiled with " +
                         "debugging information? true or false, default true.")
        boolean jasper_classdebuginfo() default true;

        @AttributeDefinition(name = "Tag Pooling",
                description = "Determines whether tag handler pooling is " +
                        "enabled. true or false, default true.")
        boolean jasper_enablePooling() default true;

        @AttributeDefinition(name = "Plugin Class-ID",
                description = "The class-id value to be sent to Internet " +
                      "Explorer when using <jsp:plugin> tags. Default " +
                      "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93.")
        String jasper_ieClassId() default "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

        @AttributeDefinition(name = "Char Array Strings",
                description = "Should text strings be generated as " +
                      "char arrays, to improve performance in some cases? Default false.")
        boolean jasper_genStringAsCharArray() default false;

        @AttributeDefinition(name = "Keep Generated Java",
                description = "Should we keep the generated Java source " +
                    "code for each page instead of deleting it? true or false, default true.")
        boolean jasper_keepgenerated() default true;

        @AttributeDefinition(name = "Mapped Content",
                description = "Should we generate static content with one " +
                   "print statement per input line, to ease debugging? true or false, default true.")
        boolean jasper_mappedfile() default true;

        @AttributeDefinition(name = "Trim Spaces",
                description = "Should white spaces in template text between " +
                       "actions or directives be trimmed ?, default false.")
        boolean jasper_trimSpaces() default false;

        @AttributeDefinition(name = "Display Source Fragments",
                description = "Should we include a source fragment " +
                        "in exception messages, which could be displayed to the developer")
        boolean jasper_displaySourceFragments() default false;

        @AttributeDefinition(name = "Default Session Value",
                description = "Should a session be created by default for every " +
                    "JSP page? Warning - this behavior may produce unintended results and changing " +
                    "it will not impact previously-compiled pages.")
        boolean default_is_session() default true;
    }

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServletContext slingServletContext;

    @Reference
    private ClassLoaderWriter classLoaderWriter;

    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private ClassLoader dynamicClassLoader;

    @Reference
    private JavaCompiler javaCompiler;

    /** The io provider for reading and writing. */
    private SlingIOProvider ioProvider;

    private SlingTldLocationsCache tldLocationsCache;

    private JspRuntimeContext jspRuntimeContext;

    private JspServletOptions options;

    private JspServletContext jspServletContext;

    private JspServletConfig servletConfig;

    private boolean defaultIsSession;

    /** The handler for the jsp factories. */
    private JspFactoryHandler jspFactoryHandler;

    public static final String[] SCRIPT_TYPE = { "jsp", "jspf", "jspx" };

    public static final String[] NAMES = { "jsp", "JSP" };

    public JspScriptEngineFactory() {
        setExtensions(SCRIPT_TYPE);
        setNames(NAMES);
    }

    /**
     * @see javax.script.ScriptEngineFactory#getScriptEngine()
     */
    @Override
    public ScriptEngine getScriptEngine() {
        return new JspScriptEngine();
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageName()
     */
    @Override
    public String getLanguageName() {
        return "Java Server Pages";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageVersion()
     */
    @Override
    public String getLanguageVersion() {
        return "2.1";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getParameter(String)
     */
    @Override
    public Object getParameter(final String name) {
        if ("THREADING".equals(name)) {
            return "STATELESS";
        }

        return super.getParameter(name);
    }

    /**
     * Call the error page
     * @param bindings The bindings
     * @param scriptHelper Script helper service
     * @param context The script context
     * @param scriptName The name of the script
     */
    private void callErrorPageJsp(final Bindings bindings,
                                  final SlingScriptHelper scriptHelper,
                                  final ScriptContext context,
                                  final String scriptName) {
    	final SlingBindings slingBindings = new SlingBindings();
        slingBindings.putAll(bindings);

        ResourceResolver resolver = (ResourceResolver) context.getAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                SlingScriptConstants.SLING_SCOPE);
        if ( resolver == null ) {
            resolver = scriptHelper.getScript().getScriptResource().getResourceResolver();
        }
        final SlingIOProvider io = this.ioProvider;
        final JspFactoryHandler jspfh = this.jspFactoryHandler;

        // abort if JSP Support is shut down concurrently (SLING-2704)
        if (io == null || jspfh == null) {
            logger.warn("callJsp: JSP Script Engine seems to be shut down concurrently; not calling {}",
                    scriptHelper.getScript().getScriptResource().getPath());
            return;
        }

        final ResourceResolver oldResolver = io.setRequestResourceResolver(resolver);
        jspfh.incUsage();
		try {
			final JspServletWrapper errorJsp = getJspWrapper(scriptName, slingBindings);
			errorJsp.service(slingBindings);

            // The error page could be inside an include.
	        final SlingHttpServletRequest request = slingBindings.getRequest();
            final Throwable t = (Throwable)request.getAttribute("javax.servlet.jsp.jspException");

	        final Object newException = request
                    .getAttribute("javax.servlet.error.exception");

            // t==null means the attribute was not set.
            if ((newException != null) && (newException == t)) {
                request.removeAttribute("javax.servlet.error.exception");
            }

            // now clear the error code - to prevent double handling.
            request.removeAttribute("javax.servlet.error.status_code");
            request.removeAttribute("javax.servlet.error.request_uri");
            request.removeAttribute("javax.servlet.error.status_code");
            request.removeAttribute("javax.servlet.jsp.jspException");
		} finally {
            jspfh.decUsage();
			io.resetRequestResourceResolver(oldResolver);
		}
     }

    /**
     * Call a JSP script
     * @param bindings The bindings
     * @param scriptHelper Script helper service
     * @param context The script context
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
        final JspFactoryHandler jspfh = this.jspFactoryHandler;

        // abort if JSP Support is shut down concurrently (SLING-2704)
        if (io == null || jspfh == null) {
            logger.warn("callJsp: JSP Script Engine seems to be shut down concurrently; not calling {}",
                    scriptHelper.getScript().getScriptResource().getPath());
            return;
        }

        final ResourceResolver oldResolver = io.setRequestResourceResolver(resolver);
        jspfh.incUsage();
        try {
            final SlingBindings slingBindings = new SlingBindings();
            slingBindings.putAll(bindings);

            final JspServletWrapper jsp = getJspWrapper(scriptHelper, slingBindings);
            // create a SlingBindings object
            jsp.service(slingBindings);
        } finally {
            jspfh.decUsage();
            io.resetRequestResourceResolver(oldResolver);
        }
    }

    private JspServletWrapper getJspWrapper(final String scriptName, final SlingBindings bindings)
    throws SlingException {
        JspRuntimeContext rctxt = this.getJspRuntimeContext();

    	JspServletWrapper wrapper = rctxt.getWrapper(scriptName);
        if (wrapper != null) {
            if ( wrapper.isValid() ) {
                return wrapper;
            }
            synchronized ( this ) {
                rctxt = this.getJspRuntimeContext();
                wrapper = rctxt.getWrapper(scriptName);
                if ( wrapper != null ) {
                    if ( wrapper.isValid() ) {
                        return wrapper;
                    }
                    this.renewJspRuntimeContext();
                    rctxt = this.getJspRuntimeContext();
                }
            }
        }

        wrapper = new JspServletWrapper(servletConfig, options,
                scriptName, false, rctxt, defaultIsSession);
        wrapper = rctxt.addWrapper(scriptName, wrapper);

        return wrapper;
    }

    private JspServletWrapper getJspWrapper(final SlingScriptHelper scriptHelper, final SlingBindings bindings)
    throws SlingException {
        final SlingScript script = scriptHelper.getScript();
        final String scriptName = script.getScriptResource().getPath();
        return getJspWrapper(scriptName, bindings);
    }

    // ---------- SCR integration ----------------------------------------------

    /**
     * Activate this component
     */
    @Activate
    protected void activate(final BundleContext bundleContext,
            final Config config,
            final Map<String, Object> properties) {
        this.defaultIsSession = config.default_is_session();

        // set the current class loader as the thread context loader for
        // the setup of the JspRuntimeContext
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.dynamicClassLoader);

        try {
            this.jspFactoryHandler = JspRuntimeContext.initFactoryHandler();

            this.tldLocationsCache = new SlingTldLocationsCache(bundleContext);

            // prepare some classes
            ioProvider = new SlingIOProvider(this.classLoaderWriter, this.javaCompiler);

            // return options which use the jspClassLoader
            options = new JspServletOptions(slingServletContext, ioProvider,
                    properties, tldLocationsCache);

            jspServletContext = new JspServletContext(ioProvider,
                slingServletContext, tldLocationsCache);

            servletConfig = new JspServletConfig(jspServletContext, options.getProperties());

        } finally {
            // make sure the context loader is reset after setting up the
            // JSP runtime context
            Thread.currentThread().setContextClassLoader(old);
        }

        // check for changes in jasper config
        this.checkJasperConfig();

        logger.info("Activating Apache Sling Script Engine for JSP with options {}", options.getProperties());
        logger.debug("IMPORTANT: Do not modify the generated servlet classes directly");
    }

    /**
     * Activate this component
     */
    @Deactivate
    protected void deactivate(final BundleContext bundleContext) {
        logger.info("Deactivating Apache Sling Script Engine for JSP");

        if ( this.tldLocationsCache != null ) {
            this.tldLocationsCache.deactivate(bundleContext);
            this.tldLocationsCache = null;
        }
        if (jspRuntimeContext != null) {
            this.destroyJspRuntimeContext(this.jspRuntimeContext);
            jspRuntimeContext = null;
        }

        ioProvider = null;
        this.jspFactoryHandler.destroy();
        this.jspFactoryHandler = null;
    }

    private static final String CONFIG_PATH = "/jsp.config";

    /**
     * Check if the jasper configuration changed.
     */
    private void checkJasperConfig() {
        boolean changed = false;
        InputStream is = null;
        try {
            is = this.classLoaderWriter.getInputStream(CONFIG_PATH);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ( ( length = is.read(buffer)) != -1 ) {
                baos.write(buffer, 0, length);
            }
            baos.close();
            final String oldKey = new String(baos.toByteArray(), "UTF-8");
            changed = !oldKey.equals(this.servletConfig.getConfigKey());
            if ( changed ) {
                logger.info("Removing all class files due to jsp configuration change");
            }
        } catch ( final IOException notFound ) {
            changed = true;
        } finally {
            if ( is != null ) {
                try {
                    is.close();
                } catch ( final IOException ignore) {
                    // ignore
                }
            }
        }
        if ( changed ) {
            OutputStream os = null;
            try {
                os = this.classLoaderWriter.getOutputStream(CONFIG_PATH);
                os.write(this.servletConfig.getConfigKey().getBytes("UTF-8"));
            } catch ( final IOException ignore ) {
                // ignore
            } finally {
                if ( os != null ) {
                    try {
                        os.close();
                    } catch ( final IOException ignore ) {
                        // ignore
                    }
                }
            }
            this.classLoaderWriter.delete("/org/apache/jsp");
        }
    }

    @Reference(target="(name=org.apache.sling)")
    protected void bindSlingServletContext(final ServletContext context) {
        this.slingServletContext = context;
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
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.STATIC)
    protected void bindDynamicClassLoaderManager(final DynamicClassLoaderManager rclp) {
        if ( this.dynamicClassLoader != null ) {
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
        this.dynamicClassLoader = rclp.getDynamicClassLoader();
    }

    /**
     * Unget the class loader
     */
    private void ungetClassLoader() {
        this.dynamicClassLoader = null;
        this.dynamicClassLoaderManager = null;
    }

    // ---------- Internal -----------------------------------------------------

    private class JspScriptEngine extends AbstractSlingScriptEngine {

        JspScriptEngine() {
            super(JspScriptEngineFactory.this);
        }

        @Override
        public Object eval(final Reader script, final ScriptContext context)
                throws ScriptException {
            Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
            SlingScriptHelper scriptHelper = (SlingScriptHelper) props.get(SLING);
            if (scriptHelper != null) {

                // set the current class loader as the thread context loader for
                // the compilation and execution of the JSP script
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(dynamicClassLoader);

                try {
                    callJsp(props, scriptHelper, context);
                } catch (final SlingServletException e) {
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
                } catch (final SlingPageException sje) {
                	callErrorPageJsp(props, scriptHelper, context, sje.getErrorPage());

                } catch (final Exception e) {

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

    private void destroyJspRuntimeContext(final JspRuntimeContext jrc) {
        if (jrc != null) {
            try {
                jrc.destroy();
            } catch (final NullPointerException npe) {
                // SLING-530, might be thrown on system shutdown in a servlet
                // container when using the Equinox servlet container bridge
                logger.debug("deactivate: ServletContext might already be unavailable", npe);
            }
        }
    }

    private JspRuntimeContext getJspRuntimeContext() {
        if ( this.jspRuntimeContext == null ) {
            synchronized ( this ) {
                if ( this.jspRuntimeContext == null ) {
                    // Initialize the JSP Runtime Context
                    this.jspRuntimeContext = new JspRuntimeContext(slingServletContext,
                            options, ioProvider);
                }
            }
        }
        return this.jspRuntimeContext;
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

        public BetterScriptException(final String message, final Exception cause) {
            super(message);
            this.initCause(cause);
        }

    }

    @Override
	public void onChange(final List<ResourceChange> changes) {
    	for(final ResourceChange change : changes){
            final JspRuntimeContext rctxt = this.jspRuntimeContext;
            if ( rctxt != null && rctxt.handleModification(change.getPath(), change.getType() == ChangeType.REMOVED) ) {
                renewJspRuntimeContext();
            }
    	}
    }

    /**
     * Renew the jsp runtime context.
     * A new context is created, the old context is destroyed in the background
     */
    private void renewJspRuntimeContext() {
        final JspRuntimeContext jrc;
        synchronized ( this ) {
            jrc = this.jspRuntimeContext;
            this.jspRuntimeContext = null;
        }
        final Thread t = new Thread() {
            @Override
            public void run() {
                destroyJspRuntimeContext(jrc);
            }
        };
        t.start();
    }
    
	@Override
	public void onClassLoaderClear(String context) {
        final JspRuntimeContext rctxt = this.jspRuntimeContext;
		if ( rctxt != null ) {
            renewJspRuntimeContext();
        }
	}
}
