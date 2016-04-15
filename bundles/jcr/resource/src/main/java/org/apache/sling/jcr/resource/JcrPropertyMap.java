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
package org.apache.sling.jcr.resource;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the value map based on a JCR node.
 *
 * @see JcrModifiablePropertyMap
 *
 * @deprecated A (JCR) resource should be adapted to a {@link ValueMap}.
 */
@Deprecated
public class JcrPropertyMap extends JcrCacheableValueMap implements ValueMap {

    private static volatile boolean LOG_DEPRECATED = true;

    private final ClassLoader dynamicClassLoader;

    /**
     * Create a new JCR property map based on a node.
     *
     * @param node The underlying node.
     * @param cache The cache for this valuemap
     */
    public JcrPropertyMap(final Node node, final ValueMapCacheProvider cache) {
        this(node, null, cache);
    }

    /**
     * Create a new JCR property map based on a node.
     *
     * @param node The underlying node.
     * @param dynamicCL Dynamic class loader for loading serialized objects.
     * @param cacheProvider The cache for this valuemap
     * @since 2.0.8
     */
    public JcrPropertyMap(final Node node, final ClassLoader dynamicCL, final ValueMapCacheProvider cacheProvider) {
        super(node, cacheProvider);
        this.dynamicClassLoader = dynamicCL;
        if ( LOG_DEPRECATED ) {
            LOG_DEPRECATED = false;
            LoggerFactory.getLogger(this.getClass()).warn("DEPRECATION WARNING: JcrPropertyMap is deprecated. Please switch to resource API.");
        }
    }

    protected ClassLoader getDynamicClassLoader() {
        return this.dynamicClassLoader;
    }

    /**
     * Get the node.
     *
     * @return the node
     */
    protected Node getNode() {
        return node;
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final String aKey, final Class<T> type) {
        final String key = checkKey(aKey);
        if (type == null) {
            return (T) get(key);
        }

        final JcrPropertyMapCacheEntry entry = this.read(key);
        if (entry == null) {
            return null;
        }
        return entry.convertToType(type, this.node, this.getDynamicClassLoader());
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final String aKey, final T defaultValue) {
        final String key = checkKey(aKey);
        if (defaultValue == null) {
            return (T) get(key);
        }

        // special handling in case the default value implements one
        // of the interface types supported by the convertToType method
        Class<T> type = (Class<T>) normalizeClass(defaultValue.getClass());

        T value = get(key, type);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    // ---------- Map
    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public Object get(final Object aKey) {
        final String key = checkKey(aKey.toString());
        final JcrPropertyMapCacheEntry entry = this.read(key);
        final Object value = (entry == null ? null : entry.getPropertyValueOrNull());
        return value;
    }

    // ---------- Unsupported Modification methods
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    // ---------- Implementation helper
    private Class<?> normalizeClass(Class<?> type) {
        if (Calendar.class.isAssignableFrom(type)) {
            type = Calendar.class;
        } else if (Date.class.isAssignableFrom(type)) {
            type = Date.class;
        } else if (Value.class.isAssignableFrom(type)) {
            type = Value.class;
        } else if (Property.class.isAssignableFrom(type)) {
            type = Property.class;
        }
        return type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JcrPropertyMap [node=");
        sb.append(this.node);
        sb.append(", values={");
        final Iterator<Map.Entry<String, Object>> iter = this.entrySet().iterator();
        boolean first = true;
        while (iter.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            final Map.Entry<String, Object> e = iter.next();
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
        }
        sb.append("}]");
        return sb.toString();
    }
}
