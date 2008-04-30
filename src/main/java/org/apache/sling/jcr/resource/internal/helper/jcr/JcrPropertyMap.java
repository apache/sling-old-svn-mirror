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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.resource.JcrResourceUtil;

class JcrPropertyMap implements Map<String, Object> {

    private final Node node;

    private final Map<String, Object> cache;

    private boolean fullyRead;

    JcrPropertyMap(Node node) {
        this.node = node;
        this.cache = new HashMap<String, Object>();
        this.fullyRead = false;
    }

    public Object get(Object key) {
        Object value = cache.get(key);
        if (value == null) {
            value = read((String) key);
        }

        return value;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        readFully();
        return cache.containsValue(value);
    }

    public boolean isEmpty() {
        if (cache.isEmpty()) {
            readFully();
        }

        return cache.isEmpty();
    }

    public int size() {
        readFully();
        return cache.size();
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        readFully();
        return cache.entrySet();
    }

    public Set<String> keySet() {
        readFully();
        return cache.keySet();
    }

    public Collection<Object> values() {
        readFully();
        return cache.values();
    }

    // ---------- Helpers to access the node's property ------------------------

    private Object read(String key) {

        // if the node has been completely read, we need not check
        // again, as we certainly will not find the key
        if (fullyRead) {
            return null;
        }

        try {
            if (node.hasProperty(key)) {
                Property prop = node.getProperty(key);
                Object value = JcrResourceUtil.toJavaObject(prop);
                cache.put(key, value);
                return value;
            }
        } catch (RepositoryException re) {
            // TODO: log !!
        }

        // property not found or some error accessing it
        return null;
    }

    private void readFully() {
        if (!fullyRead) {
            try {
                PropertyIterator pi = node.getProperties();
                while (pi.hasNext()) {
                    Property prop = pi.nextProperty();
                    String key = prop.getName();
                    if (!cache.containsKey(key)) {
                        cache.put(key, JcrResourceUtil.toJavaObject(prop));
                    }
                }
                fullyRead = true;
            } catch (RepositoryException re) {
                // TODO: log !!
            }
        }
    }

    // ---------- Unsupported Modification methods -----------------------------

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

}
