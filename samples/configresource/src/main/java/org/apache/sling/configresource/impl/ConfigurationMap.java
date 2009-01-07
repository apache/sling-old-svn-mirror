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
package org.apache.sling.configresource.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.osgi.service.cm.Configuration;

public class ConfigurationMap implements PersistableValueMap {

    private final Configuration configuration;

    private Map<String, Object> properties;

    private boolean hasChanges = false;

    public ConfigurationMap(Configuration config) {
        this.configuration = config;
    }

    private void createMap() {
        this.properties = new HashMap<String, Object>();
        final Dictionary<?, ?> dict = this.configuration.getProperties();
        final Enumeration<?> e = dict.keys();
        while ( e.hasMoreElements() ) {
            final String key = e.nextElement().toString();
            this.properties.put(key, dict.get(key));
        }
        this.hasChanges = false;
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#reset()
     */
    public void reset() {
        // just recreate the map
        this.createMap();
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#save()
     */
    public void save() throws PersistenceException {
        if ( this.hasChanges ) {
            @SuppressWarnings("unchecked")
            final Dictionary dict = new Hashtable();
            final Iterator<Map.Entry<String, Object>> i = this.properties.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, Object> current = i.next();
                dict.put(current.getKey(), current.getValue());
            }
            try {
                this.configuration.update(dict);
            } catch (IOException e) {
                throw new PersistenceException("Unable to persist configuration.", e);
            }

            this.hasChanges = false;
        }
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        final Object value = this.get(name);
        if (type == null) {
            return (T) value;
        }
        if ( value != null ) {
            if ( type.isAssignableFrom(value.getClass()) ) {
                return (T)value;
            }
            if ( type == String.class ) {
                return (T)value.toString();
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if ( defaultValue == null ) {
            return (T)this.get(name);
        }
        final Class<T> type = (Class<T>) defaultValue.getClass();

        T value = this.get(name, type);
        if ( value == null ) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        this.hasChanges = true;
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return this.properties.containsKey(key);
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return this.properties.containsValue(value);
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return this.properties.entrySet();
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        return this.properties.get(key);
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        return this.properties.keySet();
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        final Object oldObject = this.properties.get(key);
        this.properties.put(key, value);
        this.hasChanges = true;
        return oldObject;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> t) {
        this.properties.putAll(t);
        this.hasChanges = true;
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        final Object oldObject = this.properties.remove(key);
        if ( oldObject != null ) {
            this.hasChanges = true;
        }
        return oldObject;
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        return this.properties.size();
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        return this.properties.values();
    }
}
