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

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JSP engine (a.k.a Jasper).
 *
 * @scr.component scr="no" label="%jsphandler.name"
 *                description="%jsphandler.description"
 * @scr.property name="service.description" value="JSP Script Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation" *
 * @scr.property name="jasper.checkInterval" value="300" type="Integer"
 * @scr.property name="jasper.classdebuginfo" value="true" type="Boolean"
 * @scr.property name="jasper.development" value="true" type="Boolean"
 * @scr.property name="jasper.enablePooling" value="true" type="Boolean"
 * @scr.property name="jasper.ieClassId"
 *               value="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
 * @scr.property name="jasper.genStringAsCharArray" value="false" type="Boolean"
 * @scr.property name="jasper.keepgenerated" value="true" type="Boolean"
 * @scr.property name="jasper.mappedfile" value="true" type="Boolean"
 * @scr.property name="jasper.modificationTestInterval" value="4" type="Integer"
 * @scr.property name="jasper.reloading" value="false" type="Boolean"
 * @scr.property name="jasper.scratchdir" value="/classes"
 * @scr.property name="jasper.trimSpaces" value="false" type="Boolean"
 * @scr.property name="jasper.displaySourceFragments" value="true"
 *               type="Boolean"
 * @scr.service
 */
public class JspScriptEngineFactory extends AbstractScriptEngineFactory {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspScriptEngineFactory.class);

    ComponentContext componentContext;

    /** @scr.reference */
    private SlingRepository repository;

    /** @scr.reference */
    private ServletContext slingServletContext;

    /**
     * @scr.reference name="RepositoryClassLoaderProvider"
     *                interface="org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider"
     */
    private ClassLoader jspClassLoader;

    private SlingIOProvider ioProvider;

    private SlingTldLocationsCache tldLocationsCache;

    private JspRuntimeContext jspRuntimeContext;

    private Options options;

    private JspServletContext jspServletContext;

    private ServletConfig servletConfig;

    public static final String SCRIPT_TYPE = "jsp";

    public JspScriptEngineFactory() {
        setExtensions(SCRIPT_TYPE);
    }

    public ScriptEngine getScriptEngine() {
        return new JspScriptEngine();
    }

    public String getLanguageName() {
        return "Java Server Pages";
    }

    public String getLanguageVersion() {
        return "2.1";
    }

    private void callJsp(SlingScriptHelper scriptHelper) throws SlingException,
            IOException {

        ioProvider.setRequestResourceResolver(scriptHelper.getRequest().getResourceResolver());
        try {
            JspServletWrapperAdapter jsp = getJspWrapperAdapter(scriptHelper);
            jsp.service(scriptHelper);
        } finally {
            ioProvider.resetRequestResourceResolver();
        }
    }

    private JspServletWrapperAdapter getJspWrapperAdapter(
            SlingScriptHelper scriptHelper) throws SlingException {

        JspRuntimeContext rctxt = jspRuntimeContext;

        SlingScript script = scriptHelper.getScript();
        String scriptName = script.getScriptResource().getPath();
        JspServletWrapperAdapter wrapper = (JspServletWrapperAdapter) rctxt.getWrapper(scriptName);
        if (wrapper != null) {
            return wrapper;
        }

        synchronized (this) {
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

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;

        // set the current class loader as the thread context loader for
        // the setup of the JspRuntimeContext
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(jspClassLoader);

        try {
            ioProvider = new SlingIOProvider(repository);

            tldLocationsCache = new SlingTldLocationsCache(slingServletContext,
                componentContext.getBundleContext());

            // return options which use the jspClassLoader
            options = new JspServletOptions(slingServletContext, ioProvider,
                componentContext, jspClassLoader, tldLocationsCache);

            // Initialize the JSP Runtime Context
            jspRuntimeContext = new JspRuntimeContext(slingServletContext,
                options);

            // by default access the repository
            jspRuntimeContext.setIOProvider(ioProvider);

            jspServletContext = new JspServletContext(ioProvider,
                slingServletContext, tldLocationsCache);

            servletConfig = new JspServletConfig(jspServletContext,
                componentContext.getProperties());

        } finally {
            // make sure the context loader is reset after setting up the
            // JSP runtime context
            Thread.currentThread().setContextClassLoader(old);
        }

        if (log.isDebugEnabled()) {
            log.debug("Scratch dir for the JSP engine is: {}",
                options.getScratchDir().toString());
            log.debug("IMPORTANT: Do not modify the generated servlets");
        }

    }

    protected void deactivate(ComponentContext oldComponentContext) {
        if (log.isDebugEnabled()) {
            log.debug("JspScriptEngine.deactivate()");
        }

        if (jspRuntimeContext != null) {
            jspRuntimeContext.destroy();
            jspRuntimeContext = null;
        }

        if (tldLocationsCache != null) {
            tldLocationsCache.shutdown(componentContext.getBundleContext());
            tldLocationsCache = null;
        }

        ioProvider = null;
        componentContext = null;
    }

    protected void bindRepositoryClassLoaderProvider(
            RepositoryClassLoaderProvider repositoryClassLoaderProvider) {
        try {
            jspClassLoader = repositoryClassLoaderProvider.getClassLoader("admin");
        } catch (RepositoryException re) {
            log.error("Cannot get JSP class loader", re);
        }
    }

    protected void unbindRepositoryClassLoaderProvider(
            RepositoryClassLoaderProvider repositoryClassLoaderProvider) {
        if (jspClassLoader != null) {
            repositoryClassLoaderProvider.ungetClassLoader(jspClassLoader);
            jspClassLoader = null;
        }
    }

    // ---------- Internal -----------------------------------------------------

    private class JspScriptEngine extends AbstractSlingScriptEngine {

        JspScriptEngine() {
            super(JspScriptEngineFactory.this);
        }

        public void eval(SlingScript script, Map<String, Object> props)
                throws SlingException, IOException {
            eval(script, props);
        }

        public Object eval(Reader script, ScriptContext context)
                throws ScriptException {
            Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
            SlingScriptHelper scriptHelper = (SlingScriptHelper) props.get(SLING);
            if (scriptHelper != null) {
                try {
                    callJsp(scriptHelper);
                    // } catch (IOException ioe) {
                    // } catch (ServletException se) {
                } catch (Exception e) {
                    throw new ScriptException(e);
                }
            }
            return null;
        }
    }

}
