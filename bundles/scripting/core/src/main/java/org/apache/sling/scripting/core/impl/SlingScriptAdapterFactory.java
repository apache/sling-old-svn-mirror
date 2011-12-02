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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdapterFactory that adapts Resources to the DefaultSlingScript servlet, which
 * executes the Resources as scripts.
 */
@Component(metatype=false, immediate=true)
@Service({AdapterFactory.class, MimeTypeProvider.class})
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="Default SlingScriptResolver"),
    @Property(name="adaptables", value="org.apache.sling.api.resource.Resource"),
    @Property(name="adapters", value={"org.apache.sling.api.scripting.SlingScript",
                                      "javax.servlet.Servlet"}),
    @Property(name="adapter.condition", value="If the resource's path ends in an extension registered by a script engine.")
})
public class SlingScriptAdapterFactory implements AdapterFactory, MimeTypeProvider {

    private final Logger log = LoggerFactory.getLogger(SlingScriptAdapterFactory.class);

    /** list of service property values which indicate 'any' script engine */
    private static final List<String> ANY_ENGINE = Arrays.asList("*", "ANY");

    private BundleContext bundleContext;

    /**
     * The service tracker for BindingsValuesProvider impls
     */
    private ServiceTracker bindingsValuesProviderTracker;

    /**
     * The service tracker for Map impls with scripting bindings
     */
    private ServiceTracker mapBindingsValuesProviderTracker;

    /**
     * The BindingsValuesProvider impls which apply to all languages. Keys are serviceIds.
     */
    private Map<Object, BindingsValuesProvider> genericBindingsValuesProviders;

    /**
     * The BindingsValuesProvider impls which apply to a specific language.
     */
    private Map<String, Map<Object, BindingsValuesProvider>> langBindingsValuesProviders;

    /**
     * The service cache for script execution.
     */
    private ServiceCache serviceCache;

    /**
     * The script engine manager.
     */
    @Reference
    private SlingScriptEngineManager scriptEngineManager;

    // ---------- AdapterFactory -----------------------------------------------

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {

        Resource resource = (Resource) adaptable;
        String path = resource.getPath();
        String ext = path.substring(path.lastIndexOf('.') + 1);

        ScriptEngine engine = scriptEngineManager.getEngineByExtension(ext);
        if (engine != null) {
            Collection<BindingsValuesProvider> bindingsValuesProviders = getBindingsValuesProviders(engine.getFactory());
            // unchecked cast
            return (AdapterType) new DefaultSlingScript(this.bundleContext,
                    resource, engine, bindingsValuesProviders, this.serviceCache);
        }

        return null;
    }

    // ---------- MimeTypeProvider

    /**
     * Returns the first MIME type entry of the supported MIME types of a
     * ScriptEngineFactory which is registered for the extension of the given
     * name. If no ScriptEngineFactory is registered for the given extension or
     * the registered ScriptEngineFactory is not registered for a MIME type,
     * this method returns <code>null</code>.
     *
     * @param name The name whose extension is to be mapped to a MIME type. The
     *            extension is the string after the last dot in the name. If the
     *            name contains no dot, the entire name is considered the
     *            extension.
     */
    public String getMimeType(String name) {
        name = name.substring(name.lastIndexOf('.') + 1);
        ScriptEngine se = scriptEngineManager.getEngineByExtension(name);
        if (se != null) {
            List<?> mimeTypes = se.getFactory().getMimeTypes();
            if (mimeTypes != null && mimeTypes.size() > 0) {
                return String.valueOf(mimeTypes.get(0));
            }
        }

        return null;
    }

    /**
     * Returns the first extension entry of the supported extensions of a
     * ScriptEngineFactory which is registered for the given MIME type. If no
     * ScriptEngineFactory is registered for the given MIME type or the
     * registered ScriptEngineFactory is not registered for an extensions, this
     * method returns <code>null</code>.
     *
     * @param mimeType The MIME type to be mapped to an extension.
     */
    public String getExtension(String mimeType) {
        ScriptEngine se = scriptEngineManager.getEngineByMimeType(mimeType);
        if (se != null) {
            List<?> extensions = se.getFactory().getExtensions();
            if (extensions != null && extensions.size() > 0) {
                return String.valueOf(extensions.get(0));
            }
        }

        return null;
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();

        this.genericBindingsValuesProviders = new ConcurrentHashMap<Object, BindingsValuesProvider>();
        this.langBindingsValuesProviders = new ConcurrentHashMap<String, Map<Object, BindingsValuesProvider>>();

        ServiceTrackerCustomizer customizer = new BindingsValuesProviderCustomizer();

        this.bindingsValuesProviderTracker = new ServiceTracker(this.bundleContext, BindingsValuesProvider.class.getName(), customizer);
        this.bindingsValuesProviderTracker.open();

        try {
            Filter filter = this.bundleContext.createFilter(String.format("(&(objectclass=%s)(javax.script.name=*))",
                    Map.class.getName()));

            this.mapBindingsValuesProviderTracker = new ServiceTracker(this.bundleContext, filter, customizer);
            this.mapBindingsValuesProviderTracker.open();
        } catch (InvalidSyntaxException e) {
            log.warn("Unable to create ServiceTracker for Map-based script bindiings", e);
        }
        this.serviceCache = new ServiceCache(this.bundleContext);
    }

    protected void deactivate(ComponentContext context) {
        this.serviceCache.dispose();
        this.serviceCache = null;

        if (this.bindingsValuesProviderTracker != null) {
            this.bindingsValuesProviderTracker.close();
            this.bindingsValuesProviderTracker = null;
        }
        if (this.mapBindingsValuesProviderTracker != null) {
            this.mapBindingsValuesProviderTracker.close();
            this.mapBindingsValuesProviderTracker = null;
        }
        this.bundleContext = null;
    }

    private Collection<BindingsValuesProvider> getBindingsValuesProviders(ScriptEngineFactory scriptEngineFactory) {
        final List<BindingsValuesProvider> results = new ArrayList<BindingsValuesProvider>();
        results.addAll(genericBindingsValuesProviders.values());

        // we load the compatible language ones first so that the most specific
        // overrides these
        Map<Object, Object> factoryProps = scriptEngineManager.getProperties(scriptEngineFactory);
        if (factoryProps != null) {
            String[] compatibleLangs = PropertiesUtil.toStringArray(factoryProps.get("compatible.javax.script.name"), new String[0]);
            for (final String name : compatibleLangs) {
                final Map<Object, BindingsValuesProvider> langProviders = langBindingsValuesProviders.get(name);
                if (langProviders != null) {
                    results.addAll(langProviders.values());
                }
            }
        }

        for (final String name : scriptEngineFactory.getNames()) {
            final Map<Object, BindingsValuesProvider> langProviders = langBindingsValuesProviders.get(name);
            if (langProviders != null) {
                results.addAll(langProviders.values());
            }
        }

        return results;
    }

    private class BindingsValuesProviderCustomizer implements ServiceTrackerCustomizer {

        @SuppressWarnings("unchecked")
        public Object addingService(final ServiceReference ref) {
            final String[] engineNames = PropertiesUtil
                    .toStringArray(ref.getProperty(ScriptEngine.NAME), new String[0]);
            final Object serviceId = ref.getProperty(Constants.SERVICE_ID);
            Object service = bundleContext.getService(ref);
            if (service != null) {
                if (service instanceof Map) {
                    service = new MapWrappingBindingsValuesProvider((Map<String, Object>) service);
                }
                if (engineNames.length == 0) {
                    genericBindingsValuesProviders.put(serviceId, (BindingsValuesProvider) service);
                } else if (engineNames.length == 1 && ANY_ENGINE.contains(engineNames[0].toUpperCase())) {
                    genericBindingsValuesProviders.put(serviceId, (BindingsValuesProvider) service);
                } else {
                    for (String engineName : engineNames) {
                        Map<Object, BindingsValuesProvider> langProviders = langBindingsValuesProviders.get(engineName);
                        if (langProviders == null) {
                            langProviders = new ConcurrentHashMap<Object, BindingsValuesProvider>();
                            langBindingsValuesProviders.put(engineName, langProviders);
                        }

                        langProviders.put(serviceId, (BindingsValuesProvider) service);
                    }
                }
            }
            return service;
        }

        public void modifiedService(final ServiceReference ref, final Object service) {
            removedService(ref, service);
            addingService(ref);
        }

        public void removedService(final ServiceReference ref, final Object service) {
            Object serviceId = ref.getProperty(Constants.SERVICE_ID);
            if (genericBindingsValuesProviders.remove(serviceId) == null) {
                for (Map<Object, BindingsValuesProvider> coll : langBindingsValuesProviders.values()) {
                    if (coll.remove(service) != null) {
                        return;
                    }
                }
            }
        }

    }

    private class MapWrappingBindingsValuesProvider implements BindingsValuesProvider {

        private Map<String,Object> map;

        MapWrappingBindingsValuesProvider(Map<String, Object> map) {
            this.map = map;
        }

        public void addBindings(Bindings bindings) {
            for (String key : map.keySet()) {
                bindings.put(key, map.get(key));
            }
        }

    }

}
