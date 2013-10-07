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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.service.event.Event;

/**
 * An implementation of a map that helps in dealing with properties
 * of an OSGi event.
 * This map implements both, the map and the dictionary interfaces.
 * @deprecated EventAdmin supports creating events with maps now as well.
 */
@Deprecated
public class EventPropertiesMap
    extends Dictionary<String, Object>
    implements Map<String, Object>, Serializable {

    private static final long serialVersionUID = 835179638502569708L;

    /** The delegatee map. */
    private final Map<String, Object> delegatee;

    /**
     * Construct a new map out of an event object.
     * The resulting map is modifiable. But any modification has
     * no influence on the original properties of the event!
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
        this.delegatee = props;
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
    @Override
    public void clear() {
        delegatee.clear();
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return delegatee.containsKey(key);
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return delegatee.containsValue(value);
    }

    /**
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegatee.entrySet();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if ( o instanceof EventPropertiesMap ) {
            return delegatee.equals(((EventPropertiesMap)o).delegatee);
        }
        return delegatee.equals(o);
    }

    /**
     * @see java.util.Dictionary#get(java.lang.Object)
     */
    @Override
    public Object get(Object key) {
        return delegatee.get(key);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return delegatee.hashCode();
    }

    /**
     * @see java.util.Dictionary#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return delegatee.isEmpty();
    }

    /**
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<String> keySet() {
        return delegatee.keySet();
    }

    /**
     * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public Object put(String key, Object value) {
        return delegatee.put(key, value);
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> t) {
        delegatee.putAll(t);
    }

    /**
     * @see java.util.Dictionary#remove(java.lang.Object)
     */
    @Override
    public Object remove(Object key) {
        return delegatee.remove(key);
    }

    /**
     * @see java.util.Dictionary#size()
     */
    @Override
    public int size() {
        return delegatee.size();
    }

    /**
     * @see java.util.Map#values()
     */
    @Override
    public Collection<Object> values() {
        return delegatee.values();
    }

    /**
     * @see java.util.Dictionary#elements()
     */
    @Override
    public Enumeration<Object> elements() {
        return Collections.enumeration(this.values());
    }

    /**
     * @see java.util.Dictionary#keys()
     */
    @Override
    public Enumeration<String> keys() {
        return Collections.enumeration(this.keySet());
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.delegatee.toString();
    }
}
