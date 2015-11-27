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
package org.apache.sling.jcr.resource.internal;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.jcr.resource.JcrCacheableValueMap;
import org.apache.sling.jcr.resource.ValueMapCache;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;

/**
 * This implementation of the value map allows to change
 * the properties.
 *
 * TODO : This adds a lot of duplicate code - we should consolidate.
 */
public final class JcrModifiableValueMap extends JcrCacheableValueMap implements ModifiableValueMap {

    private final HelperData helper;

    /**
     * Constructor
     * @param node The underlying node.
     * @param helper Helper data object
     */
    public JcrModifiableValueMap(final Node node, final HelperData helper, final ValueMapCache cache) {
		super(node, cache);
        this.helper = helper;
    }

    /**
     * Get the node.
     */
    private Node getNode() {
        return node;
    }

    // ---------- ValueMap

    private String checkKey(final String key) {
        if ( key == null ) {
            throw new NullPointerException("Key must not be null.");
        }
        if ( key.startsWith("./") ) {
            return key.substring(2);
        }
        return key;
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String aKey, final Class<T> type) {
        final String key = checkKey(aKey);
        if (type == null) {
            return (T) get(key);
        }

        final JcrPropertyMapCacheEntry entry = this.read(key);
        if ( entry == null ) {
            return null;
        }
        return entry.convertToType(type, node, helper.dynamicClassLoader);
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String aKey,final T defaultValue) {
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
    public Object get(final Object aKey) {
        final String key = checkKey(aKey.toString());
        final JcrPropertyMapCacheEntry entry = this.read(key);
        final Object value = (entry == null ? null : entry.getPropertyValueOrNull());
        return value;
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(final Object value) {
        readFully();
        return cache.getValueCache().containsValue(value);
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        readFully();
        return cache.getCache().size();
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        readFully();
        final Map<String, Object> sourceMap;
        if (cache.getCache().size() == cache.getValueCache().size()) {
            sourceMap = cache.getValueCache();
        } else {
            sourceMap = transformEntries(cache.getCache());
        }
        return Collections.unmodifiableSet(sourceMap.entrySet());
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        readFully();
        return Collections.unmodifiableSet(cache.getCache().keySet());
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        readFully();
        final Map<String, Object> sourceMap;
        if (cache.getCache().size() == cache.getValueCache().size()) {
            sourceMap = cache.getValueCache();
        } else {
            sourceMap = transformEntries(cache.getCache());
        }
        return Collections.unmodifiableCollection(sourceMap.values());
    }

    /**
     * Return the path of the current node.
     *
     * @return the path
     * @throws IllegalStateException If a repository exception occurs
     */
    public String getPath() {
        try {
            return node.getPath();
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- Helpers to access the node's property ------------------------
    /**
     * Read all properties.
     * @throws IllegalArgumentException if a repository exception occurs
     */
    void readFully() {
        if (!fullyRead) {
            try {
                final PropertyIterator pi = node.getProperties();
                while (pi.hasNext()) {
                    final Property prop = pi.nextProperty();
                    this.cacheProperty(prop);
                }
                fullyRead = true;
            } catch (final RepositoryException re) {
                throw new IllegalArgumentException(re);
            }
        }
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

    private Map<String, Object> transformEntries(final Map<String, JcrPropertyMapCacheEntry> map) {

        final Map<String, Object> transformedEntries = new LinkedHashMap<String, Object>(map.size());
        for ( final Map.Entry<String, JcrPropertyMapCacheEntry> entry : map.entrySet() )
            transformedEntries.put(entry.getKey(), entry.getValue().getPropertyValueOrNull());

        return transformedEntries;
    }

    // ---------- Map

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(final String aKey, final Object value) {
        final String key = checkKey(aKey);
        if ( key.indexOf('/') != -1 ) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        if ( value == null ) {
            throw new NullPointerException("Value should not be null (key = " + key + ")");
        }
        readFully();
        final Object oldValue = this.get(key);
        try {
            final JcrPropertyMapCacheEntry entry = new JcrPropertyMapCacheEntry(value, this.node);
            this.cache.getCache().put(key, entry);
            final String name = escapeKeyName(key);
            if ( NodeUtil.MIXIN_TYPES.equals(name) ) {
                NodeUtil.handleMixinTypes(node, entry.convertToType(String[].class, node, this.helper.dynamicClassLoader));
            } else if ( "jcr:primaryType".equals(name) ) {
                node.setPrimaryType(entry.convertToType(String.class, node, this.helper.dynamicClassLoader));
            } else if ( entry.isArray() ) {
                node.setProperty(name, entry.convertToType(Value[].class, node, this.helper.dynamicClassLoader));
            } else {
                node.setProperty(name, entry.convertToType(Value.class, node, this.helper.dynamicClassLoader));
            }
        } catch (final RepositoryException re) {
            throw new IllegalArgumentException("Value for key " + key + " can't be put into node: " + value, re);
        }
        this.cache.getValueCache().put(key, value);

        return oldValue;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map<? extends String, ? extends Object> t) {
        if ( t != null ) {
            final Iterator<?> i = t.entrySet().iterator();
            while (i.hasNext() ) {
                @SuppressWarnings("unchecked")
                final Map.Entry<? extends String, ? extends Object> entry = (Map.Entry<? extends String, ? extends Object>) i.next();
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(final Object aKey) {
        final String key = checkKey(aKey.toString());
        readFully();
        final Object oldValue = this.cache.getCache().remove(key);
        this.cache.getValueCache().remove(key);
        try {
            final String name = escapeKeyName(key);
            if ( node.hasProperty(name) ) {
                node.getProperty(name).remove();
            }
        } catch (final RepositoryException re) {
            throw new IllegalArgumentException("Value for key " + key + " can't be removed from node.", re);
        }

        return oldValue;
    }
}
