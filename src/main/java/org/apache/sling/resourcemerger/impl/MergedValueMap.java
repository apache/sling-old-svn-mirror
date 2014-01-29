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
package org.apache.sling.resourcemerger.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * A <code>MergedValueMap</code> is a {@link ValueMap} aggregated from the
 * different resources mapped to a {@link MergedResource}.
 */
public class MergedValueMap implements ValueMap {

    /**
     * Set of properties to exclude from override
     */
    private static final Set<String> EXCLUDED_PROPERTIES = new HashSet<String>();

    static {
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_PROPERTIES);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_RESOURCE);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_CHILDREN);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_ORDER_BEFORE);
    }

    /**
     * Final properties
     */
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    /**
     * Constructor
     *
     * @param resource The merged resource to get properties from
     */
    public MergedValueMap(MergedResource resource) {
        // Iterate over physical resources
        for (String r : resource.getMappedResources()) {
            ValueMap vm = ResourceUtil.getValueMap(resource.getResourceResolver().getResource(r));
            if (properties.isEmpty()) {
                // Add all properties
                properties.putAll(vm);
            } else {
                // Get properties to add or override
                for (String key : vm.keySet()) {
                    if (!isExcludedProperty(key)) {
                        properties.put(key, vm.get(key));
                    }
                }

                // Get properties to hide
                String[] propertiesToHide = vm.get(MergedResourceConstants.PN_HIDE_PROPERTIES, new String[0]);
                if (propertiesToHide.length == 0) {
                    String propertyToHide = vm.get(MergedResourceConstants.PN_HIDE_PROPERTIES, String.class);
                    if (propertyToHide != null) {
                        propertiesToHide = new String[]{propertyToHide};
                    }
                }
                for (String propName : propertiesToHide) {
                    if (propName.equals("*")) {
                        properties.clear();
                        break;
                    } else {
                        properties.remove(propName);
                    }
                }
            }

            // Hide excluded properties
            for (String excludedProperty : EXCLUDED_PROPERTIES) {
                properties.remove(excludedProperty);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        return (T) properties.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        Object o = properties.get(name);
        return o == null ? defaultValue : (T) o;
    }

    public int size() {
        return properties != null ? properties.size() : 0;
    }

    public boolean isEmpty() {
        return properties == null || properties.isEmpty();
    }

    public boolean containsKey(Object o) {
        return properties != null && properties.containsKey(o);
    }

    public boolean containsValue(Object o) {
        return properties != null && properties.containsValue(o);
    }

    public Object get(Object o) {
        return properties != null ? properties.get(o) : null;
    }

    public Object put(String s, Object o) {
        return properties != null ? properties.put(s, o) : null;
    }

    public Object remove(Object o) {
        return properties != null ? properties.remove(o) : null;
    }

    public void putAll(Map<? extends String, ?> map) {
        if (properties != null) {
            properties.putAll(map);
        }
    }

    public void clear() {
        if (properties != null) {
            properties.clear();
        }
    }

    public Set<String> keySet() {
        return properties != null ? properties.keySet() : Collections.<String>emptySet();
    }

    public Collection<Object> values() {
        return properties != null ? properties.values() : Collections.emptyList();
    }

    public Set<Entry<String, Object>> entrySet() {
        return properties != null ? properties.entrySet() : Collections.<Entry<String, Object>>emptySet();
    }

    private boolean isExcludedProperty(String key) {
        return EXCLUDED_PROPERTIES.contains(key);
    }

}
