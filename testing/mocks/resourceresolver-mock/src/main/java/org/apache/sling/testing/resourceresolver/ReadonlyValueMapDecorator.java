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
package org.apache.sling.testing.resourceresolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;

/**
 * Decorator that disallows access to all methods that modify the value map.
 */
class ReadonlyValueMapDecorator implements ValueMap {
    
    private final ValueMap delegate;

    public ReadonlyValueMapDecorator(ValueMap base) {
        this.delegate = base;
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("ValueMap is read-only.");
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException("ValueMap is read-only.");
    }

    public void putAll(Map<? extends String, ?> t) {
        throw new UnsupportedOperationException("ValueMap is read-only.");
    }

    public void clear() {
        throw new UnsupportedOperationException("ValueMap is read-only.");
    }

    public <T> T get(String name, Class<T> type) {
        return delegate.get(name, type);
    }

    public <T> T get(String name, T defaultValue) {
        return delegate.get(name, defaultValue);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Object get(Object key) {
        return delegate.get(key);
    }

    public Set<String> keySet() {
        return delegate.keySet();
    }

    public Collection<Object> values() {
        return delegate.values();
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    ValueMap getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
