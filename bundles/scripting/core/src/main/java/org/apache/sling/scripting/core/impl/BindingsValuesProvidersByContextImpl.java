/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.core.impl;

import static org.apache.sling.scripting.api.BindingsValuesProvider.CONTEXT;
import static org.apache.sling.scripting.api.BindingsValuesProvider.DEFAULT_CONTEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptEngineFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Our default {@link BindingsValuesProvidersByContext} implementation */
@Component
@Service
public class BindingsValuesProvidersByContextImpl implements BindingsValuesProvidersByContext, ServiceTrackerCustomizer {

    private final Map<String, ContextBvpCollector> customizers = new HashMap<String, ContextBvpCollector>();
    public static final String [] DEFAULT_CONTEXT_ARRAY = new String [] { DEFAULT_CONTEXT };

    private static final String TOPIC_CREATED = "org/apache/sling/scripting/core/BindingsValuesProvider/CREATED";
    private static final String TOPIC_MODIFIED = "org/apache/sling/scripting/core/BindingsValuesProvider/MODIFIED";
    private static final String TOPIC_REMOVED = "org/apache/sling/scripting/core/BindingsValuesProvider/REMOVED";
    
    private ServiceTracker bvpTracker;
    private ServiceTracker mapsTracker;
    private BundleContext bundleContext;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<ServiceReference> pendingRefs = new ArrayList<ServiceReference>();
    
    @Reference
    private SlingScriptEngineManager scriptEngineManager;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EventAdmin eventAdmin;

    private abstract class ContextLoop {
        Object apply(ServiceReference ref) {
            final Object service = bundleContext.getService(ref);
            if(service != null) {
                for(String context : getContexts(ref)) {
                    ContextBvpCollector c = customizers.get(context);
                    if(c == null) {
                        synchronized (BindingsValuesProvidersByContextImpl.this) {
                            c = new ContextBvpCollector(bundleContext);
                            customizers.put(context, c);
                        }
                    }
                    applyInContext(c);
                }
            }
            return service;
        }
        
        protected abstract void applyInContext(ContextBvpCollector c);
    };
    
    @Activate
    public void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        
        synchronized (pendingRefs) {
            for(ServiceReference ref : pendingRefs) {
                addingService(ref);
            }
            pendingRefs.clear();
        }
        
        bvpTracker = new ServiceTracker(bundleContext, BindingsValuesProvider.class.getName(), this);
        bvpTracker.open();
        
        // Map services can also be registered to provide bindings
        mapsTracker = new ServiceTracker(bundleContext, Map.class.getName(), this);
        mapsTracker.open();
    }
    
    @Deactivate
    public void deactivate(ComponentContext ctx) {
        bvpTracker.close();
        mapsTracker.close();
        bundleContext = null;
    }
    
    public Collection<BindingsValuesProvider> getBindingsValuesProviders(
            ScriptEngineFactory scriptEngineFactory,
            String context) {
        final List<BindingsValuesProvider> results = new ArrayList<BindingsValuesProvider>();
        if(context == null) {
            context = DEFAULT_CONTEXT;
        }
        final ContextBvpCollector bvpc = customizers.get(context);
        if(bvpc == null) {
            logger.debug("no BindingsValuesProviderCustomizer available for context '{}'", context);
            return results;
        }
        
        results.addAll(bvpc.getGenericBindingsValuesProviders().values());
        logger.debug("Generic BindingsValuesProviders added for engine {}: {}", scriptEngineFactory.getNames(), results);

        // we load the compatible language ones first so that the most specific
        // overrides these
        Map<Object, Object> factoryProps = scriptEngineManager.getProperties(scriptEngineFactory);
        if (factoryProps != null) {
            String[] compatibleLangs = PropertiesUtil.toStringArray(factoryProps.get("compatible.javax.script.name"), new String[0]);
            for (final String name : compatibleLangs) {
                final Map<ServiceReference, BindingsValuesProvider> langProviders = bvpc.getLangBindingsValuesProviders().get(name);
                if (langProviders != null) {
                    results.addAll(langProviders.values());
                }
            }
            logger.debug("Compatible BindingsValuesProviders added for engine {}: {}", scriptEngineFactory.getNames(), results);
        }

        for (final String name : scriptEngineFactory.getNames()) {
            final Map<ServiceReference, BindingsValuesProvider> langProviders = bvpc.getLangBindingsValuesProviders().get(name);
            if (langProviders != null) {
                results.addAll(langProviders.values());
            }
        }
        logger.debug("All BindingsValuesProviders added for engine {}: {}", scriptEngineFactory.getNames(), results);

        return results;
    }

    private String [] getContexts(ServiceReference reference) {
        return PropertiesUtil.toStringArray(reference.getProperty(CONTEXT), new String[] { DEFAULT_CONTEXT });
    }

    private Event newEvent(final String topic, final ServiceReference reference) {
        Dictionary<Object, Object> props = new Properties();
        props.put("service.id", reference.getProperty(Constants.SERVICE_ID));
        return new Event(topic, props);
    }

    public Object addingService(final ServiceReference reference) {
        if(bundleContext == null) {
            synchronized (pendingRefs) {
                pendingRefs.add(reference);
            }
            return null;
        }
        return new ContextLoop() {
            @Override
            protected void applyInContext(ContextBvpCollector c) {
                c.addingService(reference);
                if (eventAdmin != null) {
                    eventAdmin.postEvent(newEvent(TOPIC_CREATED, reference));
                }
            }
        }.apply(reference);
    }

    public void modifiedService(final ServiceReference reference, final Object service) {
        new ContextLoop() {
            @Override
            protected void applyInContext(ContextBvpCollector c) {
                c.modifiedService(reference);
                if (eventAdmin != null) {
                    eventAdmin.postEvent(newEvent(TOPIC_MODIFIED, reference));
                }
            }
        }.apply(reference);
    }

    public void removedService(final ServiceReference reference, final Object service) {
        if(bundleContext == null) {
            synchronized (pendingRefs) {
                pendingRefs.remove(reference);
            }
            return;
        }
        new ContextLoop() {
            @Override
            protected void applyInContext(ContextBvpCollector c) {
                c.removedService(reference);
                if (eventAdmin != null) {
                    eventAdmin.postEvent(newEvent(TOPIC_REMOVED, reference));
                }
            }
        }.apply(reference);
    }
}
