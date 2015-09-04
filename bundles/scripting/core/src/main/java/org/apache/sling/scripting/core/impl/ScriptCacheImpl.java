/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.scripting.core.impl;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.scripting.api.CachedScript;
import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.core.impl.helper.CachingMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        metatype = true,
        label = "Apache Sling Script Cache",
        description = "The Script Cache is useful for running previously compiled scripts."
)
@Properties({
        @Property(
                name = ScriptCacheImpl.PROP_CACHE_SIZE,
                intValue = ScriptCacheImpl.DEFAULT_CACHE_SIZE,
                label = "Cache Size",
                description = "The Cache Size defines the maximum number of compiled script references that will be stored in the cache's" +
                        " internal map."
        ),
        @Property(
                name = ScriptCacheImpl.PROP_ADDITIONAL_EXTENSIONS,
                value = "",
                label = "Additional Extensions",
                description = "Scripts from the search paths with these extensions will also be monitored so that changes to them will " +
                        "clean the cache if the cache contains them.",
                unbounded = PropertyUnbounded.ARRAY
        )
})
@Service(ScriptCache.class)
@Reference(
        name = "scriptEngineFactory",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        referenceInterface = ScriptEngineFactory.class,
        policy = ReferencePolicy.DYNAMIC
)
@SuppressWarnings("unused")
/**
 * The {@code ScriptCache} stores information about {@link CompiledScript} instances evaluated by various {@link ScriptEngine}s that
 * implement the {@link Compilable} interface.
 */
public class ScriptCacheImpl implements EventHandler, ScriptCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptCacheImpl.class);

    public static final int DEFAULT_CACHE_SIZE = 65536;
    public static final String PROP_CACHE_SIZE = "org.apache.sling.scripting.cache.size";
    public static final String PROP_ADDITIONAL_EXTENSIONS = "org.apache.sling.scripting.cache.additional_extensions";

    private BundleContext bundleContext;
    private Map<String, SoftReference<CachedScript>> internalMap;
    private ServiceRegistration eventHandlerServiceRegistration = null;
    private Set<String> extensions = new HashSet<String>();
    private String[] additionalExtensions = new String[]{};
    private String[] searchPaths = {};

    // use a static policy so that we can reconfigure the watched script files if the search paths are changed
    @Reference(policy = ReferencePolicy.STATIC)
    private ResourceResolverFactory rrf = null;

    @Reference
    private ThreadPoolManager threadPoolManager = null;

    private ThreadPool threadPool;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();
    boolean active = false;

    public ScriptCacheImpl() {
        internalMap = new CachingMap<CachedScript>(DEFAULT_CACHE_SIZE);
    }

    @Override
    public CachedScript getScript(String scriptPath) {
        readLock.lock();
        SoftReference<CachedScript> reference = null;
        try {
            reference = internalMap.get(scriptPath);
        } finally {
            readLock.unlock();
        }
        return reference != null ? reference.get() : null;
    }

    @Override
    public void putScript(CachedScript script) {
        writeLock.lock();
        try {
            for (String searchPath : searchPaths) {
                if (script.getScriptPath().startsWith(searchPath)) {
                    SoftReference<CachedScript> reference = new SoftReference<CachedScript>(script);
                    internalMap.put(script.getScriptPath(), reference);
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            internalMap.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeScript(String scriptPath) {
        writeLock.lock();
        try {
            SoftReference<CachedScript> reference = internalMap.remove(scriptPath);
            if (reference != null) {
                return true;
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void handleEvent(final Event event) {
        /**
         * since the events trigger a synchronised map operation (remove in this case) we should handle events asynchronously so that we
         * don't block event processing
         */
        final String topic = event.getTopic();
        if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(topic) || SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
            Runnable eventTask = new Runnable() {
                @Override
                public void run() {
                    String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
                    writeLock.lock();
                    try {
                        internalMap.remove(path);
                        LOGGER.debug("Detected script change for {} - removed entry from the cache.", path);
                    } finally {
                        writeLock.unlock();
                    }
                }
            };
            threadPool.execute(eventTask);
        }
    }

    protected Set<String> getCachedScripts() {
        readLock.lock();
        try {
            return internalMap.keySet();
        } finally {
            readLock.unlock();
        }
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        threadPool = threadPoolManager.get("Script Cache Thread Pool");
        bundleContext = componentContext.getBundleContext();
        Dictionary properties = componentContext.getProperties();
        additionalExtensions = PropertiesUtil.toStringArray(properties.get(PROP_ADDITIONAL_EXTENSIONS));
        int newMaxCacheSize = PropertiesUtil.toInteger(properties.get(PROP_CACHE_SIZE), DEFAULT_CACHE_SIZE);
        if (newMaxCacheSize != DEFAULT_CACHE_SIZE) {
            // change the map only if there's a configuration change regarding the cache's max size
            CachingMap<CachedScript> newMap = new CachingMap<CachedScript>(newMaxCacheSize);
            newMap.putAll(internalMap);
            internalMap = newMap;
        }
        ResourceResolver resolver = null;
        try {
            resolver = rrf.getAdministrativeResourceResolver(null);
            searchPaths = resolver.getSearchPath();
        } catch (LoginException e) {
            LOGGER.error("Unable to store search paths.", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
        configureCache();
        active = true;
    }

    @SuppressWarnings("unchecked")
    private void configureCache() {
        writeLock.lock();
        ResourceResolver adminResolver = null;
        try {
            if (eventHandlerServiceRegistration != null) {
                eventHandlerServiceRegistration.unregister();
                eventHandlerServiceRegistration = null;
            }
            internalMap.clear();
            extensions.addAll(Arrays.asList(additionalExtensions));
            if (extensions.size() > 0) {
                adminResolver = rrf.getAdministrativeResourceResolver(null);
                StringBuilder eventHandlerFilter = new StringBuilder("(|");
                for (String searchPath : adminResolver.getSearchPath()) {
                    for (String extension : extensions) {
                        eventHandlerFilter.append("(path=").append(searchPath).append("**/*.").append(extension).append(")");
                    }
                }
                eventHandlerFilter.append(")");
                Dictionary eventHandlerProperties = new Hashtable();
                eventHandlerProperties.put(EventConstants.EVENT_FILTER, eventHandlerFilter.toString());
                eventHandlerProperties.put(EventConstants.EVENT_TOPIC,
                        new String[]{SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED});
                eventHandlerServiceRegistration = bundleContext.registerService(EventHandler.class.getName(), this, eventHandlerProperties);
            }
        } catch (LoginException e) {
            LOGGER.error("Unable to set automated cache invalidation for the ScriptCache.", e);
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
            writeLock.unlock();
        }
    }

    @Deactivate
    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext componentContext) {
        internalMap.clear();
        if (eventHandlerServiceRegistration != null) {
            eventHandlerServiceRegistration.unregister();
            eventHandlerServiceRegistration = null;
        }
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
            threadPool = null;
        }
        active = false;
    }

    protected void bindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory, Map<String, Object> properties) {
        ScriptEngine engine = scriptEngineFactory.getScriptEngine();
        if (engine instanceof Compilable) {
            /**
             * we only care about creating an EventHandler that monitors scripts generated by script engines which implement Compilable
             */
            for (String extension : scriptEngineFactory.getExtensions()) {
                extensions.add(extension);
            }
            if (active) {
                configureCache();
            }
        }
    }

    protected void unbindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory, Map<String, Object> properties) {
        for (String extension : scriptEngineFactory.getExtensions()) {
            extensions.remove(extension);
        }
        if (active) {
            configureCache();
        }
    }
}
