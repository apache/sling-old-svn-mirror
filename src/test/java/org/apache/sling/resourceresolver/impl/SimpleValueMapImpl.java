/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.resourceresolver.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;

public class SimpleValueMapImpl implements ValueMap {

    private Map<String, Object> delegate;

    public SimpleValueMapImpl() {
        delegate = new HashMap<String, Object>();
    }

    public void clear() {
        delegate.clear();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    public Object get(Object key) {
        return delegate.get(key);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set<String> keySet() {
        return delegate.keySet();
    }

    public Object put(String key, Object value) {
        return delegate.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> m) {
        delegate.putAll(m);
    }

    public Object remove(Object key) {
        return delegate.remove(key);
    }

    public int size() {
        return delegate.size();
    }

    public Collection<Object> values() {
        return delegate.values();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object o = delegate.get(name);
        if ( type.equals(String[].class) && ! ( o instanceof String[])) {
            o = new String[] { String.valueOf(o) };
        }
        return (T) o;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if ( delegate.containsKey(name)) {
            return (T) delegate.get(name);
        } 
        return defaultValue;
    }

}
