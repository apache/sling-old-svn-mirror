/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component which exposes a ScriptEngineManager service.
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.reference name="ScriptEngineFactory"
 *                interface="javax.script.ScriptEngineFactory"
 *                cardinality="0..n" policy="dynamic"
 */
public class ScriptEngineManagerFactory implements BundleListener {

    private final Logger log = LoggerFactory.getLogger(ScriptEngineManagerFactory.class);

    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/" + ScriptEngineFactory.class.getName();

    private BundleContext bundleContext;

    /**
     * The service tracker for the event admin
     */
    private ServiceTracker eventAdminTracker;

    private ScriptEngineManager scriptEngineManager;

    private List<Bundle> engineSpiBundles = new LinkedList<Bundle>();

    private List<ScriptEngineFactory> engineSpiServices = new LinkedList<ScriptEngineFactory>();

    private ServiceRegistration scriptEngineManagerRegistration;

    @SuppressWarnings("unchecked")
    private void refreshScriptEngineManager() {

        if (scriptEngineManagerRegistration != null) {
            scriptEngineManagerRegistration.unregister();
            scriptEngineManagerRegistration = null;
        }

        // create (empty) script engine manager
        ClassLoader loader = getClass().getClassLoader();
        SlingScriptEngineManager tmp = new SlingScriptEngineManager(loader);

        // register script engines from bundles
        final SortedSet<Object> extensions = new TreeSet<Object>();
        for (Bundle bundle : engineSpiBundles) {
            extensions.addAll(registerFactories(tmp, bundle));
        }

        // register script engines from registered services
        for (ScriptEngineFactory factory : engineSpiServices) {
            extensions.addAll(registerFactory(tmp, factory));
        }

        scriptEngineManager = tmp;

        if (bundleContext != null) {
            scriptEngineManagerRegistration = bundleContext.registerService(
                ScriptEngineManager.class.getName(), scriptEngineManager,
                new Hashtable());
        }

        // Log messages to verify which ScriptEngine is actually used
        // for our registered extensions
        if (log.isInfoEnabled()) {
            for (Object o : extensions) {
                final String ext = o.toString();
                final ScriptEngine e = scriptEngineManager.getEngineByExtension(ext);
                if (e == null) {
                    log.warn("No ScriptEngine found for extension '{}' that was just registered", ext);
                } else {
                    log.info("Script extension '{}' is now handled by ScriptEngine '{}', version='{}', class='{}'", new Object[] { ext,
                            e.getFactory().getEngineName(), e.getFactory().getEngineVersion(), e.getClass().getName() });
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<?> registerFactories(SlingScriptEngineManager mgr, Bundle bundle) {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        final SortedSet<String> extensions = new TreeSet<String>();
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Class<ScriptEngineFactory> clazz = bundle.loadClass(line);
                    ScriptEngineFactory spi = clazz.newInstance();
                    registerFactory(mgr, spi);
                    extensions.addAll(spi.getExtensions());
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

        return extensions;
    }

    private Collection<?> registerFactory(SlingScriptEngineManager mgr, ScriptEngineFactory factory) {
        log.info("Adding ScriptEngine {}, {} for language {}, {}", new Object[] { factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion() });

        mgr.registerScriptEngineFactory(factory);

        return factory.getExtensions();
    }

    // ---------- BundleListener interface -------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null) {

            engineSpiBundles.add(event.getBundle());
            refreshScriptEngineManager();

        } else if (event.getType() == BundleEvent.STOPPED && engineSpiBundles.remove(event.getBundle())) {

            refreshScriptEngineManager();

        }
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();

        // setup tracker first as this is used in the bind/unbind methods
        this.eventAdminTracker = new ServiceTracker(this.bundleContext, EventAdmin.class.getName(), null);
        this.eventAdminTracker.open();



        this.bundleContext.addBundleListener(this);

        Bundle[] bundles = this.bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE && bundle.getEntry(ENGINE_FACTORY_SERVICE) != null) {
                engineSpiBundles.add(bundle);
            }
        }

        try {
            org.apache.sling.scripting.core.impl.ScriptEngineConsolePlugin.initPlugin(context.getBundleContext(), this);
        } catch (Throwable t) {
            // so what ?
        }

        refreshScriptEngineManager();
    }

    protected void deactivate(ComponentContext context) {
        try {
            org.apache.sling.scripting.core.impl.ScriptEngineConsolePlugin.destroyPlugin();
        } catch (Throwable t) {
            // so what ?
        }

        context.getBundleContext().removeBundleListener(this);

        if (scriptEngineManagerRegistration != null) {
            scriptEngineManagerRegistration.unregister();
            scriptEngineManagerRegistration = null;
        }
        engineSpiBundles.clear();
        engineSpiServices.clear();
        scriptEngineManager = null;
        if (this.eventAdminTracker != null) {
            this.eventAdminTracker.close();
            this.eventAdminTracker = null;
        }
        this.bundleContext = null;
    }



    protected void bindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.add(scriptEngineFactory);
        refreshScriptEngineManager();
        // send event
        postEvent(SlingScriptConstants.TOPIC_SCRIPT_ENGINE_FACTORY_ADDED, scriptEngineFactory);
    }

    protected void unbindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.remove(scriptEngineFactory);
        refreshScriptEngineManager();
        // send event
        postEvent(SlingScriptConstants.TOPIC_SCRIPT_ENGINE_FACTORY_REMOVED, scriptEngineFactory);
    }

    /**
     * Get the event admin.
     *
     * @return The event admin or <code>null</code>
     */
    private EventAdmin getEventAdmin() {
        return (EventAdmin) (this.eventAdminTracker != null ? this.eventAdminTracker.getService() : null);
    }

    @SuppressWarnings("unchecked")
    private String[] toArray(List list) {
        return (String[]) list.toArray(new String[list.size()]);
    }

    private void postEvent(final String topic, final ScriptEngineFactory scriptEngineFactory) {
        final EventAdmin localEA = this.getEventAdmin();
        if (localEA != null) {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_NAME, scriptEngineFactory.getEngineName());
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_VERSION, scriptEngineFactory.getEngineVersion());
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_EXTENSIONS, toArray(scriptEngineFactory.getExtensions()));
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_NAME, scriptEngineFactory.getLanguageName());
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_VERSION, scriptEngineFactory.getLanguageVersion());
            props.put(SlingScriptConstants.PROPERTY_SCRIPT_ENGINE_FACTORY_MIME_TYPES, toArray(scriptEngineFactory.getMimeTypes()));
            localEA.postEvent(new Event(topic, props));
        }
    }


    ScriptEngineManager getScriptEngineManager() {
        return scriptEngineManager;
    }


}
