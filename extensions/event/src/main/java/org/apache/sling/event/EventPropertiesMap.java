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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.service.event.Event;

/**
 * An implementation of a map that wrapps an OSGi event.
 */
public class EventPropertiesMap implements Map<String, Object> {

    private final Map<String, Object> delegatee = new HashMap<String, Object>();

    public EventPropertiesMap(final Event event) {
        if ( event.getPropertyNames() != null ) {
            final String[] names = event.getPropertyNames();
            for(final String key : names) {
                this.delegatee.put(key, event.getProperty(key));
            }
        }
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("Clear is not supported.");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("Clear is not supported.");
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException("Clear is not supported.");
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException("Clear is not supported.");
    }

    public boolean containsKey(Object key) {
        return delegatee.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegatee.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegatee.entrySet();
    }

    public boolean equals(Object o) {
        return delegatee.equals(o);
    }

    public Object get(Object key) {
        return delegatee.get(key);
    }

    public int hashCode() {
        return delegatee.hashCode();
    }

    public boolean isEmpty() {
        return delegatee.isEmpty();
    }

    public Set<String> keySet() {
        return delegatee.keySet();
    }

    public int size() {
        return delegatee.size();
    }

    public Collection<Object> values() {
        return delegatee.values();
    }
}
