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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.jcr.resource.internal.NodeUtil;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the value map allows to change
 * the properties and save them later on.
 *
 * @deprecated Resources should be adapted to a modifiable value map instead
 */
@SuppressWarnings("deprecation")
@Deprecated
public final class JcrModifiablePropertyMap
    extends JcrPropertyMap
    implements PersistableValueMap {

    private static volatile boolean LOG_DEPRECATED = true;

    /** Set of removed and changed properties. */
    private Set<String> changedProperties;

    /**
     * Constructor
     * @param node The underlying node.
     * @param cacheProvider cacheProvider that holds the cache for this particular PropertyMap
     */
    public JcrModifiablePropertyMap(final Node node, final ValueMapCacheProvider cacheProvider) {
        super(node, cacheProvider);
        if ( LOG_DEPRECATED ) {
            LOG_DEPRECATED = false;
            LoggerFactory.getLogger(this.getClass()).warn("DEPRECATION WARNING: JcrModifiablePropertyMap is deprecated. Please switch to resource API.");
        }
    }

    /**
     * Constructor
     * @param node The underlying node.
     * @param dynamicCL Dynamic class loader for loading serialized objects.
     * @param cacheProvider cacheProvider that holds the cache for this particular PropertyMap
     * @since 2.0.6
     */
    public JcrModifiablePropertyMap(final Node node, final ClassLoader dynamicCL, final ValueMapCacheProvider cacheProvider) {
        super(node, dynamicCL, cacheProvider);
        if ( LOG_DEPRECATED ) {
            LOG_DEPRECATED = false;
            LoggerFactory.getLogger(this.getClass()).warn("DEPRECATION WARNING: JcrModifiablePropertyMap is deprecated. Please switch to resource API.");
        }
    }

    // ---------- Map
    /**
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        // we have to read all properties first
        this.readFully();
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.addAll(this.cacheProvider.getCache().keySet());
        this.cacheProvider.getCache().clear();
        this.cacheProvider.getValueCache().clear();
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public Object put(String aKey, Object value) {
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
            this.cacheProvider.getCache().put(key, new JcrPropertyMapCacheEntry(value, this.getNode()));
        } catch (final RepositoryException re) {
            throw new IllegalArgumentException("Value for key " + key + " can't be put into node: " + value, re);
        }
        this.cacheProvider.getValueCache().put(key, value);
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.add(key);
        return oldValue;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> t) {
        readFully();
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
    public Object remove(Object aKey) {
        final String key = checkKey(aKey.toString());
        readFully();
        final Object oldValue = this.cacheProvider.getCache().remove(key);
        this.cacheProvider.getValueCache().remove(key);
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.add(key);
        return oldValue;
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#reset()
     */
    @Override
    public void reset() {
        if ( this.changedProperties != null ) {
            this.changedProperties = null;
        }
        this.cacheProvider.getCache().clear();
        this.cacheProvider.getValueCache().clear();
        this.fullyRead = false;
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#save()
     */
    @Override
    @SuppressWarnings("javadoc")
    public void save() throws PersistenceException {
        if ( this.changedProperties == null || this.changedProperties.size() == 0 ) {
            // nothing has changed
            return;
        }
        try {
            final Node node = getNode();
            // check for mixin types
            if ( this.changedProperties.contains(NodeUtil.MIXIN_TYPES) ) {
                if ( cacheProvider.getCache().containsKey(NodeUtil.MIXIN_TYPES) ) {
                    final JcrPropertyMapCacheEntry entry = cacheProvider.getCache().get(NodeUtil.MIXIN_TYPES);
                    NodeUtil.handleMixinTypes(node, entry.convertToType(String[].class, node, getDynamicClassLoader()));
                } else {
                    // remove all mixin types!
                    NodeUtil.handleMixinTypes(node, null);
                }
            }

            for(final String key : this.changedProperties) {
                final String name = escapeKeyName(key);
                if ( !NodeUtil.MIXIN_TYPES.equals(name) ) {
                    if ( cacheProvider.getCache().containsKey(key) ) {
                        final JcrPropertyMapCacheEntry entry = cacheProvider.getCache().get(key);
                        if ( entry.isArray() ) {
                            node.setProperty(name, entry.convertToType(Value[].class, node, getDynamicClassLoader()));
                        } else {
                            node.setProperty(name, entry.convertToType(Value.class, node, getDynamicClassLoader()));
                        }
                    } else {
                        if ( node.hasProperty(name) ) {
                            node.getProperty(name).remove();
                        }
                    }
                }
            }
            node.getSession().save();
            this.reset();
        } catch (final RepositoryException re) {
            throw new PersistenceException("Unable to persist changes.", re, getPath(), null);
        }
    }
}
