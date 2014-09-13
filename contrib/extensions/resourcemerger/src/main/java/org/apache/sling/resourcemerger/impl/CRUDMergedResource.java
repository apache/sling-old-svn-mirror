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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;

/**
 * {@inheritDoc}
 */
public class CRUDMergedResource extends MergedResource {

    private final MergedResourcePicker picker;

    private final String relativePath;

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath   Merge root path
     * @param relativePath    Relative path
     * @param mappedResources List of physical mapped resources' paths
     */
    CRUDMergedResource(final ResourceResolver resolver,
                   final String mergeRootPath,
                   final String relativePath,
                   final List<Resource> mappedResources,
                   final List<ValueMap> valueMaps,
                   final MergedResourcePicker picker) {
        super(resolver, mergeRootPath, relativePath, mappedResources, valueMaps);
        this.picker = picker;
        this.relativePath = relativePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ModifiableValueMap.class) {
            final Iterator<Resource> iter = this.picker.pickResources(this.getResourceResolver(), this.relativePath);
            Resource highestRsrc = null;
            while ( iter.hasNext() ) {
                highestRsrc = iter.next();
            }
            if ( ResourceUtil.isNonExistingResource(highestRsrc) ) {
                final String paths[] = (String[])this.getResourceMetadata().get(MergedResourceConstants.METADATA_RESOURCES);

                final Resource copyResource = this.getResourceResolver().getResource(paths[paths.length - 1]);
                try {
                    final Resource newResource = ResourceUtil.getOrCreateResource(this.getResourceResolver(), highestRsrc.getPath(), copyResource.getResourceType(), null, false);
                    final ModifiableValueMap target = newResource.adaptTo(ModifiableValueMap.class);
                    if ( target != null ) {
                        return (AdapterType)new ModifiableProperties(this, target);
                    }
                } catch ( final PersistenceException pe) {
                    // we ignore this for now
                }
                return super.adaptTo(type);
            }
            final ModifiableValueMap target = highestRsrc.adaptTo(ModifiableValueMap.class);
            if ( target != null ) {
                return (AdapterType)new ModifiableProperties(this, target);
            }
        }
        return super.adaptTo(type);
    }

    private static final class ModifiableProperties implements ModifiableValueMap {

        private final ModifiableValueMap targetMap;

        private final ValueMap properties;

        public ModifiableProperties(final Resource rsrc, final ModifiableValueMap targetMap) {
            this.properties = new DeepReadValueMapDecorator(rsrc, new ValueMapDecorator(new HashMap<String, Object>(rsrc.getValueMap())));
            this.targetMap = targetMap;
        }

        public <T> T get(final String name, final Class<T> type) {
            return properties.get(name, type);
        }

        public <T> T get(final String name, final T defaultValue) {
            return properties.get(name, defaultValue);
        }

        public int size() {
            return properties.size();
        }

        public boolean isEmpty() {
            return properties.isEmpty();
        }

        public boolean containsKey(final Object key) {
            return properties.containsKey(key);
        }

        public boolean containsValue(final Object value) {
            return properties.containsValue(value);
        }

        public Object get(final Object key) {
            return properties.get(key);
        }

        public Object put(final String key, final Object value) {
            final Object result = this.properties.get(key);
            this.targetMap.put(key, value);
            return result;
        }

        public Object remove(final Object key) {
            final Object result = this.properties.get(key);
            if ( this.targetMap.remove(key) == null ) {
                final String[] hiddenProps = this.targetMap.get(MergedResourceConstants.PN_HIDE_PROPERTIES, String[].class);
                final String[] newHiddenProps;
                if ( hiddenProps == null || hiddenProps.length == 0 ) {
                    newHiddenProps = new String[] {key.toString()};
                } else {
                    newHiddenProps = new String[hiddenProps.length + 1];
                    System.arraycopy(hiddenProps, 0, newHiddenProps, 0, hiddenProps.length);
                    newHiddenProps[hiddenProps.length] = key.toString();
                }
                this.targetMap.put(MergedResourceConstants.PN_HIDE_PROPERTIES, newHiddenProps);
            }
            return result;
        }

        public void putAll(final Map<? extends String, ? extends Object> m) {
            if ( m != null ) {
                for(final Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
                    this.put(entry.getKey(), entry.getValue());
                }
            }
        }

        public void clear() {
            // not supported
        }

        public Set<String> keySet() {
            return this.properties.keySet();
        }

        public Collection<Object> values() {
            return this.properties.values();
        }

        public Set<Entry<String, Object>> entrySet() {
            return this.properties.entrySet();
        }
    }
}
