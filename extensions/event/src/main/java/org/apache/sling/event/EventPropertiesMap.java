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
package org.apache.sling.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.event.Event;

/**
 * An implementation of a map that wrapps an OSGi event.
 */
public class EventPropertiesMap implements Map<String, Object> {

    private final boolean isEmpty;

    private final Event event;

    public EventPropertiesMap(final Event event) {
        this.event = event;
        this.isEmpty = (event.getPropertyNames() == null || event.getPropertyNames().length == 0);
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("clear is not supported.");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("put is not supported.");
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException("putAll is not supported.");
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException("remove is not supported.");
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        if ( this.isEmpty || key == null ) {
            return false;
        }
        final String[] names = this.event.getPropertyNames();
        for(final String name : names) {
            if ( name.equals(key) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        if ( this.isEmpty ) {
            return false;
        }
        final String[] names = this.event.getPropertyNames();
        for(final String name : names) {
            if ( this.event.getProperty(name).equals(value) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        if ( this.isEmpty ) {
            return Collections.emptySet();
        }
        final Set<Map.Entry<String, Object>> set = new HashSet<Map.Entry<String,Object>>();
        final String[] names = event.getPropertyNames();
        for(final String key : names) {
            set.add(new PropertyEntry(key, this.event.getProperty(key)));
        }
        return set;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if ( o instanceof EventPropertiesMap ) {
            return this.event.equals(((EventPropertiesMap)o).event);
        }
        return false;
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        return (this.isEmpty || key == null ? null : this.event.getProperty(key.toString()));
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.event.hashCode();
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        if ( this.isEmpty ) {
            return Collections.emptySet();
        }
        final Set<String> set = new HashSet<String>();
        set.addAll(Arrays.asList(this.event.getPropertyNames()));
        return set;
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        return (this.isEmpty ? 0 : this.event.getPropertyNames().length);
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        if ( this.isEmpty ) {
            return Collections.emptySet();
        }
        final Set<Object> set = new HashSet<Object>();
        final String[] names = this.event.getPropertyNames();
        for(final String name : names) {
            set.add(this.event.getProperty(name));
        }
        return set;
    }

    protected static final class PropertyEntry implements Map.Entry<String, Object> {

        private final String key;
        private final Object value;

        public PropertyEntry(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }

        /**
         * @see java.util.Map.Entry#getKey()
         */
        public String getKey() {
            return this.key;
        }

        /**
         * @see java.util.Map.Entry#getValue()
         */
        public Object getValue() {
            return this.value;
        }

        /**
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        public Object setValue(Object value) {
            throw new UnsupportedOperationException("setValue is not supported.");
        }
    }
}
