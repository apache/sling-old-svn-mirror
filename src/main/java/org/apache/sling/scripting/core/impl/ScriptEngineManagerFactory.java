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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.scripting.core.impl.helper.ProxyScriptEngineManager;
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
 */
@Component(metatype=false, immediate=true, specVersion="1.1")
@Reference(name="ScriptEngineFactory", referenceInterface=ScriptEngineFactory.class,
           policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class ScriptEngineManagerFactory implements BundleListener {

    private final Logger log = LoggerFactory.getLogger(ScriptEngineManagerFactory.class);

    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/" + ScriptEngineFactory.class.getName();

    private BundleContext bundleContext;

    /**
     * The service tracker for the event admin
     */
    private ServiceTracker eventAdminTracker;

    /**
     * The proxy to the actual ScriptEngineManager. This proxy is actually
     * registered as the ScriptEngineManager service for the lifetime of
     * this factory.
     */
    private final ProxyScriptEngineManager scriptEngineManager = new ProxyScriptEngineManager();

    private final Set<Bundle> engineSpiBundles = new HashSet<Bundle>();

    private final Map<ScriptEngineFactory, Map<Object, Object>> engineSpiServices = new HashMap<ScriptEngineFactory, Map<Object, Object>>();

    private ServiceRegistration scriptEngineManagerRegistration;

    /**
     * Refresh the script engine manager.
     */
    private void refreshScriptEngineManager() {
        // create (empty) script engine manager
        final ClassLoader loader = getClass().getClassLoader();
        final SlingScriptEngineManager tmp = new SlingScriptEngineManager(loader);

        // register script engines from bundles
        final SortedSet<Object> extensions = new TreeSet<Object>();
        synchronized (this.engineSpiBundles) {
            for (final Bundle bundle : this.engineSpiBundles) {
                extensions.addAll(registerFactories(tmp, bundle));
            }
        }

        // register script engines from registered services
        synchronized (this.engineSpiServices) {
            for (final Map.Entry<ScriptEngineFactory, Map<Object, Object>> factory : this.engineSpiServices.entrySet()) {
                extensions.addAll(registerFactory(tmp, factory.getKey(),
                    factory.getValue()));
            }
        }

        scriptEngineManager.setDelegatee(tmp);

        // Log messages to verify which ScriptEngine is actually used
        // for our registered extensions
        if (log.isInfoEnabled()) {
            for (Object o : extensions) {
                final String ext = o.toString();
                final ScriptEngine e = tmp.getEngineByExtension(ext);
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
    private Collection<?> registerFactories(final SlingScriptEngineManager mgr, final Bundle bundle) {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        final SortedSet<String> extensions = new TreeSet<String>();
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.trim().length() > 0) {
	                try {
	                    Class<ScriptEngineFactory> clazz = bundle.loadClass(line);
	                    ScriptEngineFactory spi = clazz.newInstance();
	                    registerFactory(mgr, spi, null);
	                    extensions.addAll(spi.getExtensions());
	                } catch (Throwable t) {
	                    log.error("Cannot register ScriptEngineFactory " + line, t);
	                }
            	}
            }
        } catch (IOException ioe) {
            // ignore
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

    private Collection<?> registerFactory(final SlingScriptEngineManager mgr, final ScriptEngineFactory factory, final Map<Object, Object> props) {
        log.info("Adding ScriptEngine {}, {} for language {}, {}", new Object[] { factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion() });

        mgr.registerScriptEngineFactory(factory, props);

        return factory.getExtensions();
    }

    // ---------- BundleListener interface -------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED
            && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null) {
            synchronized (this.engineSpiBundles) {
                this.engineSpiBundles.add(event.getBundle());
            }
            this.refreshScriptEngineManager();
        } else if (event.getType() == BundleEvent.STOPPED) {
            boolean refresh;
            synchronized (this.engineSpiBundles) {
                refresh = this.engineSpiBundles.remove(event.getBundle());
            }
            if (refresh) {
                this.refreshScriptEngineManager();
            }
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
        synchronized (this.engineSpiBundles) {
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE
                    && bundle.getEntry(ENGINE_FACTORY_SERVICE) != null) {
                    this.engineSpiBundles.add(bundle);
                }
            }
        }

        // create a script engine manager
        this.refreshScriptEngineManager();

        scriptEngineManagerRegistration = this.bundleContext.registerService(
            new String[] { ScriptEngineManager.class.getName(),
                SlingScriptEngineManager.class.getName() },
            scriptEngineManager, new Hashtable<String, Object>());

        org.apache.sling.scripting.core.impl.ScriptEngineConsolePlugin.initPlugin(context.getBundleContext(), this);
    }

    protected void deactivate(ComponentContext context) {
        org.apache.sling.scripting.core.impl.ScriptEngineConsolePlugin.destroyPlugin();

        context.getBundleContext().removeBundleListener(this);

        if (scriptEngineManagerRegistration != null) {
            scriptEngineManagerRegistration.unregister();
            scriptEngineManagerRegistration = null;
        }

        synchronized ( this ) {
            this.engineSpiBundles.clear();
            this.engineSpiServices.clear();
        }

        scriptEngineManager.setDelegatee(null);

        if (this.eventAdminTracker != null) {
            this.eventAdminTracker.close();
            this.eventAdminTracker = null;
        }

        this.bundleContext = null;
    }

    protected void bindScriptEngineFactory(final ScriptEngineFactory scriptEngineFactory, final Map<Object, Object> props) {
        if (scriptEngineFactory != null) {
            synchronized ( this.engineSpiServices) {
                this.engineSpiServices.put(scriptEngineFactory, props);
            }

            this.refreshScriptEngineManager();

            // send event
            postEvent(SlingScriptConstants.TOPIC_SCRIPT_ENGINE_FACTORY_ADDED, scriptEngineFactory);
        }
    }

    protected void unbindScriptEngineFactory(final ScriptEngineFactory scriptEngineFactory) {
        boolean refresh;
        synchronized (this.engineSpiServices) {
            refresh = this.engineSpiServices.remove(scriptEngineFactory) != null;
        }

        if (refresh) {
            this.refreshScriptEngineManager();
        }

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

    private String[] toArray(final List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    /**
     * Post a notification with the EventAdmin
     */
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

    /**
     * Get the script engine manager.
     * Refresh the manager if changes occured.
     */
    ScriptEngineManager getScriptEngineManager() {
        return this.scriptEngineManager;
    }
}
