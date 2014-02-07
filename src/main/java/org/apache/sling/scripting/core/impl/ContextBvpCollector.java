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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/** Keeps track of {@link BindingsValuesProvider} for a single context */
class ContextBvpCollector {
    
    /** list of service property values which indicate 'any' script engine */
    private static final List<String> ANY_ENGINE = Arrays.asList("*", "ANY");

    private final BundleContext bundleContext;
    
    /**
     * The BindingsValuesProvider impls which apply to all languages. Keys are serviceIds.
     */
    private final Map<ServiceReference, BindingsValuesProvider> genericBindingsValuesProviders;

    /**
     * The BindingsValuesProvider impls which apply to a specific language.
     */
    private final Map<String, Map<ServiceReference, BindingsValuesProvider>> langBindingsValuesProviders;
    
    ContextBvpCollector(BundleContext bc) {
        bundleContext = bc;
        genericBindingsValuesProviders = new ConcurrentSkipListMap<ServiceReference, BindingsValuesProvider>();
        langBindingsValuesProviders = new ConcurrentHashMap<String, Map<ServiceReference, BindingsValuesProvider>>();
    }

    @SuppressWarnings("unchecked")
    public Object addingService(final ServiceReference ref) {
        final String[] engineNames = PropertiesUtil
                .toStringArray(ref.getProperty(ScriptEngine.NAME), new String[0]);
        Object service = bundleContext.getService(ref);
        if (service != null) {
            if (service instanceof Map) {
                service = new MapWrappingBindingsValuesProvider((Map<String, Object>) service);
            }
            if (engineNames.length == 0) {
                genericBindingsValuesProviders.put(ref, (BindingsValuesProvider) service);
            } else if (engineNames.length == 1 && ANY_ENGINE.contains(engineNames[0].toUpperCase())) {
                genericBindingsValuesProviders.put(ref, (BindingsValuesProvider) service);
            } else {
                for (String engineName : engineNames) {
                    Map<ServiceReference, BindingsValuesProvider> langProviders = langBindingsValuesProviders.get(engineName);
                    if (langProviders == null) {
                        langProviders = new ConcurrentSkipListMap<ServiceReference, BindingsValuesProvider>();
                        langBindingsValuesProviders.put(engineName, langProviders);
                    }

                    langProviders.put(ref, (BindingsValuesProvider) service);
                }
            }
        }
        return service;
    }

    public void modifiedService(final ServiceReference ref) {
        removedService(ref);
        // Note that any calls to our get* methods at this
        // point won't see the service. We could synchronize
        // to make sure this methods acts atomically, but it
        // doesn't seem worth it, as we don't expect BVPs to 
        // be modified often. Living with that small inconsistency
        // is probably worth it for the sake of simpler code.
        addingService(ref);
    }

    public void removedService(final ServiceReference ref) {
        Object serviceId = ref.getProperty(Constants.SERVICE_ID);
        if (genericBindingsValuesProviders.remove(serviceId) == null) {
            for (Map<ServiceReference, BindingsValuesProvider> coll : langBindingsValuesProviders.values()) {
                coll.remove(ref);
            }
        }
    }
    
    Map<ServiceReference, BindingsValuesProvider> getGenericBindingsValuesProviders() {
        return genericBindingsValuesProviders;
    }

    Map<String, Map<ServiceReference, BindingsValuesProvider>> getLangBindingsValuesProviders() {
        return langBindingsValuesProviders;
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
        
        @Override
        public String toString() {
            return map.toString();
        }
    }

}