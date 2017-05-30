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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.scripting.api.CachedScript;
import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.core.impl.helper.CachingMap;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ScriptCache.class,
    reference = @Reference(
        name = "ScriptEngineFactory",
        service = ScriptEngineFactory.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    ),
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = ScriptCacheImplConfiguration.class
)
/**
 * The {@code ScriptCache} stores information about {@link CompiledScript} instances evaluated by various {@link ScriptEngine}s that
 * implement the {@link Compilable} interface.
 */
public class ScriptCacheImpl implements ScriptCache, ResourceChangeListener, ExternalResourceChangeListener {

    private final Logger LOGGER = LoggerFactory.getLogger(ScriptCacheImpl.class);

    public static final int DEFAULT_CACHE_SIZE = 65536;

    private BundleContext bundleContext;
    private Map<String, SoftReference<CachedScript>> internalMap;
    private ServiceRegistration<ResourceChangeListener> resourceChangeListener;
    private Set<String> extensions = new HashSet<>();
    private String[] additionalExtensions = new String[]{};
    private String[] searchPaths = {};

    // use a static policy so that we can reconfigure the watched script files if the search paths are changed
    @Reference
    private ResourceResolverFactory rrf;

    @Reference
    private ThreadPoolManager threadPoolManager;

    private ThreadPool threadPool;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();
    private volatile boolean active = false;

    @Reference
    private ServiceUserMapped serviceUserMapped;

    public ScriptCacheImpl() {
        internalMap = new CachingMap<>(DEFAULT_CACHE_SIZE);
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
                    SoftReference<CachedScript> reference = new SoftReference<>(script);
                    internalMap.put(script.getScriptPath(), reference);
                    LOGGER.debug("Added script {} to script cache.", script.getScriptPath());
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
            LOGGER.debug("Cleared script cache.");
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeScript(String scriptPath) {
        writeLock.lock();
        try {
            SoftReference<CachedScript> reference = internalMap.remove(scriptPath);
            boolean result = reference != null;
            if (result) {
                LOGGER.debug("Removed script {} from script cache.", scriptPath);
            }
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void onChange(@Nonnull List<ResourceChange> list) {
        for (final ResourceChange change : list) {
            Runnable eventTask = new Runnable() {
                @Override
                public void run() {
                    String path = change.getPath();
                    writeLock.lock();
                    try {
                        final boolean removed = internalMap.remove(path) != null;
                        LOGGER.debug("Detected script change for {} - removed entry from the cache.", path);
                        if ( !removed && change.getType() == ChangeType.REMOVED ) {
                            final String prefix = path + "/";
                            final Set<String> removal = new HashSet<>();
                            for(final Map.Entry<String, SoftReference<CachedScript>> entry : internalMap.entrySet()) {
                                if ( entry.getKey().startsWith(prefix) ) {
                                    removal.add(entry.getKey());
                                }
                            }
                            for(final String key : removal) {
                                internalMap.remove(key);
                                LOGGER.debug("Detected removal for {} - removed entry {} from the cache.", path, key);
                            }
                        }
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
    protected void activate(ScriptCacheImplConfiguration configuration, BundleContext bundleCtx) {
        threadPool = threadPoolManager.get("Script Cache Thread Pool");
        bundleContext = bundleCtx;
        additionalExtensions = configuration.org_apache_sling_scripting_cache_additional__extensions();
        int newMaxCacheSize = configuration.org_apache_sling_scripting_cache_size();
        if (newMaxCacheSize != DEFAULT_CACHE_SIZE) {
            // change the map only if there's a configuration change regarding the cache's max size
            CachingMap<CachedScript> newMap = new CachingMap<>(newMaxCacheSize);
            newMap.putAll(internalMap);
            internalMap = newMap;
        }
        ResourceResolver resolver = null;
        try {
            resolver = rrf.getServiceResourceResolver(null);
            searchPaths = resolver.getSearchPath();
        } catch (LoginException e) {
            LOGGER.error("Unable to retrieve a ResourceResolver for determining the search paths.", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }

        configureCache();
        active = true;
    }

    private void configureCache() {
        writeLock.lock();
        try {
            if (resourceChangeListener != null) {
                resourceChangeListener.unregister();
                resourceChangeListener = null;
            }
            internalMap.clear();
            extensions.addAll(Arrays.asList(additionalExtensions));
            if (!extensions.isEmpty()) {
                Set<String> globPatterns = new HashSet<>(extensions.size());
                for (String extension : extensions) {
                    globPatterns.add("glob:**/*." + extension);
                }
                Dictionary<String, Object> resourceChangeListenerProperties = new Hashtable<>();
                resourceChangeListenerProperties.put(ResourceChangeListener.PATHS, globPatterns.toArray(new String[globPatterns.size()]));
                resourceChangeListenerProperties.put(ResourceChangeListener.CHANGES,
                        new String[]{ResourceChange.ChangeType.CHANGED.name(), ResourceChange.ChangeType.REMOVED.name()});
                resourceChangeListener =
                        bundleContext.registerService(
                                ResourceChangeListener.class,
                                this,
                                resourceChangeListenerProperties
                        );
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Deactivate
    protected void deactivate() {
        internalMap.clear();
        if (resourceChangeListener != null) {
            resourceChangeListener.unregister();
            resourceChangeListener = null;
        }
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
            threadPool = null;
        }
        active = false;
    }

    protected void bindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
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

    protected void unbindScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        for (String extension : scriptEngineFactory.getExtensions()) {
            extensions.remove(extension);
        }
        if (active) {
            configureCache();
        }
    }
}
