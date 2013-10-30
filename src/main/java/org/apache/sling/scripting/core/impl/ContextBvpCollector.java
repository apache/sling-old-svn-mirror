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
    private final Map<Object, BindingsValuesProvider> genericBindingsValuesProviders;

    /**
     * The BindingsValuesProvider impls which apply to a specific language.
     */
    private final Map<String, Map<Object, BindingsValuesProvider>> langBindingsValuesProviders;
    
    ContextBvpCollector(BundleContext bc) {
        bundleContext = bc;
        genericBindingsValuesProviders = new ConcurrentHashMap<Object, BindingsValuesProvider>();
        langBindingsValuesProviders = new ConcurrentHashMap<String, Map<Object, BindingsValuesProvider>>();
    }

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
                coll.remove(serviceId);
            }
        }
    }
    
    Map<Object, BindingsValuesProvider> getGenericBindingsValuesProviders() {
        return genericBindingsValuesProviders;
    }

    Map<String, Map<Object, BindingsValuesProvider>> getLangBindingsValuesProviders() {
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