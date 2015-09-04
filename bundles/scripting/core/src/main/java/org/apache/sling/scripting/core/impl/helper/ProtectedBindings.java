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
 package org.apache.sling.scripting.core.impl.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

public class ProtectedBindings implements Bindings {

    private final Bindings wrapped;
    private final Set<String> protectedKeys;

    public ProtectedBindings(Bindings wrapped, Set<String> protectedKeys) {
        this.wrapped = wrapped;
        this.protectedKeys = protectedKeys;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the key is protected
     */
    public Object put(String key, Object value) {
        if (protectedKeys.contains(key)) {
            throw new IllegalArgumentException(String.format("Key %s is protected.", key));
        }
        return wrapped.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (String key : toMerge.keySet()) {
            if (!protectedKeys.contains(key)) {
                wrapped.put(key, toMerge.get(key));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the key is protected
     */
    public Object remove(Object key) {
        if (protectedKeys.contains(key)) {
            throw new IllegalArgumentException(String.format("Key %s is protected.", key));
        }
        return wrapped.remove(key);
    }

    /**
     * The clear operation is not supported.
     */
    public void clear() {
        throw new UnsupportedOperationException("ProtectedBindings does not support clear()");
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return wrapped.containsValue(value);
    }

    /**
     * Returns a Set view of the mappings contains in this map. The Set is
     * unmodifiable.
     *
     * @return an unmodifiable Set view of the map
     */
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(wrapped.entrySet());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    /**
     * Returns a Set view of the keys contained in this map. The Set is
     * unmodifiable.
     *
     * @return an unmodifiable Set view of the map's keys
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(wrapped.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return wrapped.size();
    }

    /**
     * Returns a Collection view of the values contained in this map. The
     * Collection is unmodifiable.
     *
     * @return an unmodifiable Collection view of the map's values
     */
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(wrapped.values());
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return wrapped.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        return wrapped.get(key);
    }

}
