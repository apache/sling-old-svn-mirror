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
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletConfig;
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
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Java engine
 *
 */
@Component(service={javax.script.ScriptEngineFactory.class, ResourceChangeListener.class},
           property={
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
                   Constants.SERVICE_DESCRIPTION + "=" + JavaScriptEngineFactory.DESCRIPTION,
                   ResourceChangeListener.CHANGES + "=CHANGED",
                   ResourceChangeListener.CHANGES + "=REMOVED",
                   ResourceChangeListener.PATHS + "=glob:**/*.java"
           })
@Designate(ocd = JavaScriptEngineFactory.Config.class)
public class JavaScriptEngineFactory
    extends AbstractScriptEngineFactory
    implements ResourceChangeListener, ExternalResourceChangeListener {

    public static final String DESCRIPTION = "Java Servlet Script Handler";

    @ObjectClassDefinition(name = "Apache Sling Java Script Handler",
           description = "The Java Script Handler supports development of Java Servlets to render response content. ")

    public @interface Config {

        @AttributeDefinition(name = "Generate Debug Info", description = "Should the class file be compiled with " +
                   "debugging information? true or false, default true.")
        boolean java_classdebuginfo() default true;

        @AttributeDefinition(name = "Source Encoding", description = "")
        String java_javaEncoding() default "UTF-8";

        @AttributeDefinition(name = "Source VM", description = "Java Specification to be used to read " +
                 "the source files. If left empty or the value \"auto\" is specified, the " +
                 "current vm version will be used.")
        String java_compilerSourceVM() default JavaScriptEngineFactory.VERSION_AUTO;

        @AttributeDefinition(name = "Target VM", description = "Target Java version for compilation. If left " +
                   "empty or the value \"auto\" is specified, the current vm version will be used.")
        String java_compilerTargetVM() default JavaScriptEngineFactory.VERSION_AUTO;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String VERSION_AUTO = "auto";

    @Reference
    private JavaCompiler javaCompiler;

    @Reference(target="(name=org.apache.sling)")
    private ServletContext slingServletContext;

    private SlingIOProvider ioProvider;

    private JavaServletContext javaServletContext;

    private ServletConfig servletConfig;

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
    @Override
    public ScriptEngine getScriptEngine() {
        return new JavaScriptEngine(this);
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageName()
     */
    @Override
    public String getLanguageName() {
        return "Java Servlet Compiler";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getLanguageVersion()
     */
    @Override
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
     * @param config Configuration properties
     */
    @Activate
    protected void activate(final Config config, final Map<String, Object> props) {
        final CompilerOptions opts = CompilerOptions.createOptions(config);
        this.ioProvider = new SlingIOProvider(this.javaCompiler, opts);
        this.javaServletContext = new JavaServletContext(ioProvider,
            slingServletContext);

        this.servletConfig = new JavaServletConfig(javaServletContext, props);

        logger.info("Activating Apache Sling Script Engine for Java with options {}", opts);
    }

    /**
     * Deactivate this engine
     */
    @Deactivate
    protected void deactivate() {
        if ( this.ioProvider != null ) {
            this.ioProvider.destroy();
            this.ioProvider = null;
        }
        javaServletContext = null;
        servletConfig = null;
        logger.info("Deactivating Apache Sling Script Engine for Java");
    }

    /**
     * Call the servlet.
     * @param bindings The bindings for the script invocation
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

    @Override
	public void onChange(List<ResourceChange> resourceChange) {
		for(ResourceChange change : resourceChange){
			ChangeType topic = change.getType();
			if (topic.equals(ChangeType.CHANGED)) {
				this.handleModification(change.getPath(), false);
			} else if (topic.equals(ChangeType.REMOVED)){
				this.handleModification(change.getPath(), true);
			}
		}
	}

    private void handleModification(final String scriptName, final boolean remove) {
        this.ioProvider.getServletCache().removeWrapper(scriptName, remove);
    }

    private static class JavaScriptEngine extends AbstractSlingScriptEngine {

        JavaScriptEngine(JavaScriptEngineFactory factory) {
            super(factory);
        }

        /**
         * @see javax.script.ScriptEngine#eval(java.io.Reader, javax.script.ScriptContext)
         */
        @Override
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
