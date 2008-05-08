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
package org.apache.sling.scripting.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  AdapterFactory that adapts Resources to the DefaultSlingScript servlet,
 *  which executes the Resources as scripts.
 *
 * @scr.component metatype="no"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Default SlingScriptResolver"
 *
 * @scr.property name="adaptables"
 *               value="org.apache.sling.api.resource.Resource";
 * @scr.property name="adapters"
 *               values.0="org.apache.sling.api.scripting.SlingScript"
 *               values.1="javax.servlet.Servlet"
 *
 * @scr.service interface="org.apache.sling.api.adapter.AdapterFactory"
 *
 * @scr.reference name="ScriptEngineFactory"
 *                interface="javax.script.ScriptEngineFactory"
 *                cardinality="0..n" policy="dynamic"
 */
public class SlingScriptAdapterFactory implements AdapterFactory,
        BundleListener {

    private static final Logger log = LoggerFactory.getLogger(SlingScriptAdapterFactory.class);

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/"
        + ScriptEngineFactory.class.getName();

    private ScriptEngineManager scriptEngineManager;

    private List<Bundle> engineSpiBundles = new LinkedList<Bundle>();

    private List<ScriptEngineFactory> engineSpiServices = new LinkedList<ScriptEngineFactory>();

    private BundleContext bundleContext;

    //---------- AdapterFactory -----------------------------------------------

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {

        Resource resource = (Resource) adaptable;
        String path = resource.getPath();
        String ext = path.substring(path.lastIndexOf('.') + 1);

        ScriptEngine engine = getScriptEngineManager().getEngineByExtension(
            ext);
        if (engine != null) {
            // unchecked cast
            return (AdapterType) new DefaultSlingScript(this.bundleContext, resource, engine);
        }

        return null;
    }

    private ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {

            // create (empty) script engine manager
            ClassLoader loader = getClass().getClassLoader();
            ScriptEngineManager tmp = new ScriptEngineManager(loader);

            // register script engines from bundles
            for (Bundle bundle : engineSpiBundles) {
                registerFactories(tmp, bundle);
            }

            // register script engines from registered services
            for (ScriptEngineFactory factory : engineSpiServices) {
                registerFactory(tmp, factory);
            }

            scriptEngineManager = tmp;
        }
        return scriptEngineManager;
    }

    private void registerFactories(ScriptEngineManager mgr, Bundle bundle) {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                ins));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<ScriptEngineFactory> clazz = bundle.loadClass(line);
                    ScriptEngineFactory spi = clazz.newInstance();
                    registerFactory(mgr, spi);
                } catch (Throwable t) {
                    log.error("Cannot register ScriptEngineFactory " + line, t);
                }
            }
        } catch (IOException ioe) {
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    private void registerFactory(ScriptEngineManager mgr,
            ScriptEngineFactory factory) {
        log.info("Adding ScriptEngine {}, {} for language {}, {}",
            new Object[] { factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion() });

        for (Object ext : factory.getExtensions()) {
            mgr.registerEngineExtension((String) ext, factory);
        }

        for (Object mime : factory.getMimeTypes()) {
            mgr.registerEngineMimeType((String) mime, factory);
        }

        for (Object name : factory.getNames()) {
            mgr.registerEngineName((String) name, factory);
        }
    }

    // ---------- BundleListener interface -------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED
            && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null) {

            engineSpiBundles.add(event.getBundle());
            scriptEngineManager = null;

        } else if (event.getType() == BundleEvent.STOPPED
            && engineSpiBundles.remove(event.getBundle())) {

            scriptEngineManager = null;

        }
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
        this.bundleContext.addBundleListener(this);

        Bundle[] bundles = this.bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE
                && bundle.getEntry(ENGINE_FACTORY_SERVICE) != null) {
                engineSpiBundles.add(bundle);
            }
        }
    }

    protected void deactivate(ComponentContext context) {
        context.getBundleContext().removeBundleListener(this);

        engineSpiBundles.clear();
        engineSpiServices.clear();
        scriptEngineManager = null;
        this.bundleContext = null;
    }

    protected void bindScriptEngineFactory(
            ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.add(scriptEngineFactory);
        scriptEngineManager = null;
    }

    protected void unbindScriptEngineFactory(
            ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.remove(scriptEngineFactory);
        scriptEngineManager = null;
    }

}
