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
package org.apache.sling.servlethelpers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

/**
 * Mock implementation of {@link RequestParameterMap}.
 */
class MockRequestParameterMap implements RequestParameterMap {
    
    private final Map<String,RequestParameter[]> delegate = new HashMap<String, RequestParameter[]>();
    
    public RequestParameter getValue(String name) {
        RequestParameter[] params = getValues(name);
        return (params != null && params.length > 0) ? params[0] : null;
    }

    public RequestParameter[] getValues(String name) {
        return delegate.get(name);
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

    public RequestParameter[] get(Object key) {
        return delegate.get(key);
    }

    public RequestParameter[] put(String key, RequestParameter[] value) {
        return delegate.put(key, value);
    }

    public RequestParameter[] remove(Object key) {
        return delegate.remove(key);
    }

    public void putAll(Map<? extends String, ? extends RequestParameter[]> m) {
        delegate.putAll(m);
    }

    public void clear() {
        delegate.clear();
    }

    public Set<String> keySet() {
        return delegate.keySet();
    }

    public Collection<RequestParameter[]> values() {
        return delegate.values();
    }

    public Set<java.util.Map.Entry<String, RequestParameter[]>> entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

}
