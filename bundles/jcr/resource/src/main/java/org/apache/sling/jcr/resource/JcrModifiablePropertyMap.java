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
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;

/**
 * This implementation of the value map allows to change
 * the properies and save them later on.
 */
public final class JcrModifiablePropertyMap
    extends JcrPropertyMap
    implements PersistableValueMap {

    /** Set of removed and changed properties. */
    private Set<String> changedProperties;

    /**
     * Constructor
     * @param node The underlying node.
     */
    public JcrModifiablePropertyMap(final Node node) {
        super(node);
    }

    /**
     * Constructor
     * @param node The underlying node.
     * @param dynamicCL Dynamic class loader for loading serialized objects.
     * @since 2.0.6
     */
    public JcrModifiablePropertyMap(final Node node, final ClassLoader dynamicCL) {
        super(node, dynamicCL);
    }

    // ---------- Map
    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        // we have to read all properties first
        this.readFully();
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.addAll(this.cache.keySet());
        this.cache.clear();
        this.valueCache.clear();
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
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
            this.cache.put(key, new JcrPropertyMapCacheEntry(value, getNode().getSession()));
        } catch (RepositoryException re) {
            throw new IllegalArgumentException("Value for key " + key + " can't be put into node: " + value, re);
        }
        this.valueCache.put(key, value);
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.add(key);
        return oldValue;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
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
    public Object remove(Object aKey) {
        final String key = checkKey(aKey.toString());
        readFully();
        final Object oldValue = this.cache.remove(key);
        this.valueCache.remove(key);
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.add(key.toString());
        return oldValue;
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#reset()
     */
    public void reset() {
        if ( this.changedProperties != null ) {
            this.changedProperties = null;
        }
        this.cache.clear();
        this.valueCache.clear();
        this.fullyRead = false;
    }

    /** Property for the mixin node types. */
    private static final String MIXIN_TYPES = "jcr:mixinTypes";

    /**
     * Update the mixin node types
     */
    private void handleMixinTypes(final Node node, final Value[] mixinTypes) throws RepositoryException {
        final Set<String> newTypes = new HashSet<String>();
        if ( mixinTypes != null ) {
            for(final Value value : mixinTypes ) {
                newTypes.add(value.getString());
            }
        }
        final Set<String> oldTypes = new HashSet<String>();
        for(final NodeType mixinType : node.getMixinNodeTypes()) {
            oldTypes.add(mixinType.getName());
        }
        for(final String name : oldTypes) {
            if ( !newTypes.contains(name) ) {
                node.removeMixin(name);
            } else {
                newTypes.remove(name);
            }
        }
        for(final String name : newTypes) {
            node.addMixin(name);
        }
    }

    /**
     * @see org.apache.sling.api.resource.PersistableValueMap#save()
     */
    public void save() throws PersistenceException {
        if ( this.changedProperties == null || this.changedProperties.size() == 0 ) {
            // nothing has changed
            return;
        }
        try {
            final Node node = getNode();
            // check for mixin types
            final String mixinTypesKey = ISO9075.decode(MIXIN_TYPES);
            if ( this.changedProperties.contains(mixinTypesKey) ) {
                if ( cache.containsKey(mixinTypesKey) ) {
                    final JcrPropertyMapCacheEntry entry = cache.get(mixinTypesKey);
                    handleMixinTypes(node, entry.values);
                } else {
                    // remove all mixin types!
                    handleMixinTypes(node, null);
                }
            }

            for(final String key : this.changedProperties) {
                final String name = ISO9075.encodePath(key);
                if ( !MIXIN_TYPES.equals(name) ) {
                    if ( cache.containsKey(key) ) {
                        final JcrPropertyMapCacheEntry entry = cache.get(key);
                        if ( entry.isMulti ) {
                            node.setProperty(name, entry.values);
                        } else {
                            node.setProperty(name, entry.values[0]);
                        }
                    } else {
                        node.setProperty(name, (String)null);
                    }
                }
            }
            node.getSession().save();
        } catch (RepositoryException re) {
            throw new PersistenceException("Unable to persist changes.", re);
        }
        this.reset();
    }
}
