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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.osgi.service.event.Event;

/**
 * An implementation of a map that helps in dealing with properties
 * of an OSGi event.
 * This map implements both, the map and the dictionary interfaces.
 */
public class EventPropertiesMap
    extends Dictionary<String, Object>
    implements Map<String, Object> {

    private final Map<String, Object> delegatee;

    /**
     * Construct a new map out of an event object.
     * The resulting map is unmodifiable.
     * @param event The event object.
     */
    public EventPropertiesMap(final Event event) {
        // create a map out of the event properties
        final Map<String, Object> props = new HashMap<String, Object>();
        if ( event.getPropertyNames() != null ) {
            for(final String key : event.getPropertyNames() ) {
                props.put(key, event.getProperty(key));
            }
        }
        this.delegatee = Collections.unmodifiableMap(props);
    }

    /**
     * Construct a new map out of another map.
     * @param props The properties map object.
     */
    public EventPropertiesMap(final Map<String, Object> props) {
        this.delegatee = props;
    }

    /**
     * Construct a new map.
     */
    public EventPropertiesMap() {
        this.delegatee = new HashMap<String, Object>();
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        delegatee.clear();
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return delegatee.containsKey(key);
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return delegatee.containsValue(value);
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegatee.entrySet();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return delegatee.equals(o);
    }

    /**
     * @see java.util.Dictionary#get(java.lang.Object)
     */
    public Object get(Object key) {
        return delegatee.get(key);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return delegatee.hashCode();
    }

    /**
     * @see java.util.Dictionary#isEmpty()
     */
    public boolean isEmpty() {
        return delegatee.isEmpty();
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        return delegatee.keySet();
    }

    /**
     * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        return delegatee.put(key, value);
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> t) {
        delegatee.putAll(t);
    }

    /**
     * @see java.util.Dictionary#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        return delegatee.remove(key);
    }

    /**
     * @see java.util.Dictionary#size()
     */
    public int size() {
        return delegatee.size();
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        return delegatee.values();
    }

    /**
     * @see java.util.Dictionary#elements()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<Object> elements() {
        return new IteratorEnumeration(this.values().iterator());
    }

    /**
     * @see java.util.Dictionary#keys()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> keys() {
        return new IteratorEnumeration(this.keySet().iterator());
    }
}
