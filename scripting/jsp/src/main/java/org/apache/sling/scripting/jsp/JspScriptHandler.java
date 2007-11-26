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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.apache.jasper.JasperException;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JSP engine (a.k.a Jasper).
 *
 * @scr.component immediate="false" label="%jsphandler.name"
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
 * @scr.service
 */
public class JspScriptHandler implements SlingScriptEngine  {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspScriptHandler.class);

    private ComponentContext componentContext;

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * @scr.reference
     */
    private ServletContext slingServletContext;

    /**
     * @scr.reference name="RepositoryClassLoaderProvider"
     *      interface="org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider"
     */
    private ClassLoader jspClassLoader;

    private RepositoryOutputProvider outputProvider;

    private TldLocationsCacheSupport tldLocationsCache;

    private JspComponentContext jspComponentContext;

    public static final String SCRIPT_TYPE = "jsp";

    public String getType() {
        return SCRIPT_TYPE;
    }

    public String[] getExtensions() {
        // probably also jspx, jspf ?
        return new String[]{ SCRIPT_TYPE };
    }

    public String getEngineName() {
        return "JSP ScriptEngine (Jasper, Eclipse)";
    }

    public String getEngineVersion() {
        return "0.9";
    }

    public void eval(SlingScript script, Map<String, Object> props)
            throws SlingException, IOException {
        SlingScriptHelper ssh = (SlingScriptHelper) props.get(SLING);
        if (ssh != null) {
            JspServletWrapperAdapter jsp = getJspWrapperAdapter(ssh.getRequest(), "TODO");
            jsp.service(ssh);
        }
    }

    private JspServletWrapperAdapter getJspWrapperAdapter(
            SlingHttpServletRequest component,
            String scriptName) {

        JspComponentContext jcc = getJspRuntimeContext();
        JspRuntimeContext rctxt = jcc.getRctxt();

        JspServletWrapperAdapter wrapper = (JspServletWrapperAdapter) rctxt.getWrapper(scriptName);
        if (wrapper != null) {
            return wrapper;
        }

        synchronized (this) {
            wrapper = (JspServletWrapperAdapter) rctxt.getWrapper(scriptName);
            if (wrapper != null) {
                return wrapper;
            }

            // Check if the requested JSP page exists, to avoid creating
            // unnecessary directories and files.
            try {
                if (jcc.getServletContext().getResource(scriptName) == null) {
                    log.info("getJspWrapperAdapter: Script {} does not exist",
                        scriptName);
                    return null;
                }
            } catch (MalformedURLException mue) {
                log.error("getJspWrapperAdapter: Cannot check script {}",
                    scriptName, mue);
            }

            try {
                wrapper = new JspServletWrapperAdapter(jcc.getServletConfig(),
                    jcc.getOptions(), scriptName, false, rctxt);
                rctxt.addWrapper(scriptName, wrapper);
                return wrapper;
            } catch (JasperException je) {
                log.error(
                    "getJspWrapperAdapter: Error creating adapter for script {}",
                    scriptName, je);
                return null;
            }
        }
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.tldLocationsCache = new TldLocationsCacheSupport(
            componentContext.getBundleContext());
        this.outputProvider = new RepositoryOutputProvider(repository);
    }

    protected void deactivate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("JspScriptHandler.deactivate()");
        }

        if (jspComponentContext != null) {
            jspComponentContext.getRctxt().setOutputProvider(null);
            jspComponentContext.getRctxt().destroy();
            jspComponentContext = null;
        }

        if (tldLocationsCache != null) {
            tldLocationsCache.shutdown(componentContext.getBundleContext());
            tldLocationsCache = null;
        }

        outputProvider.dispose();
        this.componentContext = null;
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

    private JspComponentContext getJspRuntimeContext() {
        if (jspComponentContext == null) {
            jspComponentContext = new JspComponentContext(slingServletContext);
        }
        return jspComponentContext;
    }

    private class JspComponentContext {

        private final ServletContext servletContext;

        private final ServletConfig servletConfig;

        private final Options options;

        private final JspRuntimeContext rctxt;

        JspComponentContext(ServletContext slingServletContext) {
            this.servletContext = new JspServletContext(slingServletContext,
                tldLocationsCache, outputProvider);
            this.servletConfig = new JspServletConfig(servletContext);

            // return options which use the jspClassLoader
            TldLocationsCache tlc = tldLocationsCache.getTldLocationsCache(servletContext);
            this.options = new JspServletOptions(servletConfig, outputProvider,
                jspClassLoader, tlc);

            // set the current class loader as the thread context loader for
            // the setup of the JspRuntimeContext
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(jspClassLoader);

            try {
                // Initialize the JSP Runtime Context
                this.rctxt = new JspRuntimeContext(servletContext, options);

            } finally {
                // make sure the context loader is reset after setting up the
                // JSP runtime context
                Thread.currentThread().setContextClassLoader(old);
            }

            // by default access the repository
            rctxt.setOutputProvider(outputProvider);

            if (log.isDebugEnabled()) {
                log.debug("Scratch dir for the JSP engine is: {}",
                    options.getScratchDir().toString());
                log.debug("IMPORTANT: Do not modify the generated servlets");
            }
        }

        public ServletConfig getServletConfig() {
            return servletConfig;
        }

        public ServletContext getServletContext() {
            return servletContext;
        }

        public Options getOptions() {
            return options;
        }

        public JspRuntimeContext getRctxt() {
            return rctxt;
        }
    }

    private class JspServletConfig implements ServletConfig {
        private final ServletContext servletContext;

        private String servletName;
        private Map<String, String> properties;

        JspServletConfig(ServletContext servletContext) {
            this.servletContext = servletContext;

            Dictionary<?, ?> props = componentContext.getProperties();

            // set the servlet name
            servletName = (String) props.get(Constants.SERVICE_DESCRIPTION);
            if (servletName == null) {
                servletName = "JSP Script Handler";
            }

            // copy the "jasper." properties
            properties = new HashMap<String, String>();
            for (Enumeration<?> ke = props.keys(); ke.hasMoreElements();) {
                String key = (String) ke.nextElement();
                if (key.startsWith("jasper.")) {
                    properties.put(key.substring("jasper.".length()),
                        String.valueOf(props.get(key)));
                }
            }

        }

        public String getInitParameter(String name) {
            return properties.get(name);
        }

        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(properties.keySet());
        }

        public ServletContext getServletContext() {
            return servletContext;
        }

        public String getServletName() {
            return servletName;
        }
    }
}
