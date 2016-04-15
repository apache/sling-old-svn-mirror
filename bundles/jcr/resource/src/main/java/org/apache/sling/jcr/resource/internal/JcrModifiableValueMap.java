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

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.jcr.resource.JcrCacheableValueMap;
import org.apache.sling.jcr.resource.ValueMapCacheProvider;
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
    public JcrModifiableValueMap(final Node node, final HelperData helper, final ValueMapCacheProvider cacheProvider) {
        super(node, cacheProvider);
        this.helper = helper;
    }

    // ---------- ValueMap

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
        if ( entry == null ) {
            return null;
        }
        return entry.convertToType(type, node, helper.getDynamicClassLoader());
    }

    // ---------- Map

    /**
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
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
            this.cacheProvider.getCache().put(key, entry);
            final String name = escapeKeyName(key);
            if ( NodeUtil.MIXIN_TYPES.equals(name) ) {
                NodeUtil.handleMixinTypes(node, entry.convertToType(String[].class, node, this.helper.getDynamicClassLoader()));
            } else if ( "jcr:primaryType".equals(name) ) {
                node.setPrimaryType(entry.convertToType(String.class, node, this.helper.getDynamicClassLoader()));
            } else if ( entry.isArray() ) {
                node.setProperty(name, entry.convertToType(Value[].class, node, this.helper.getDynamicClassLoader()));
            } else {
                node.setProperty(name, entry.convertToType(Value.class, node, this.helper.getDynamicClassLoader()));
            }
        } catch (final RepositoryException re) {
            throw new IllegalArgumentException("Value for key " + key + " can't be put into node: " + value, re);
        }
        this.cacheProvider.getValueCache().put(key, value);

        return oldValue;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
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
    @Override
    public Object remove(final Object aKey) {
        final String key = checkKey(aKey.toString());
        readFully();
        final Object oldValue = this.cacheProvider.getCache().remove(key);
        this.cacheProvider.getValueCache().remove(key);
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
