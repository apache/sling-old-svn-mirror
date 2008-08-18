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

import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.PersistenceException;

/**
 * This implementation of the value map allows to change
 * the properies and save them later on.
 */
public class JcrModifiablePropertyMap
    extends JcrPropertyMap
    implements PersistableValueMap {

    /** Set of removed and changed properties. */
    private Set<String> changedProperties;

    public JcrModifiablePropertyMap(Node node) {
        super(node);
    }

    // ---------- Map

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        Object value = cache.get(key);
        if (value == null &&  !this.fullyRead ) {
            value = read((String) key);
        }

        return value;
    }

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
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        readFully();
        final Object oldValue = this.get(key);
        if ( this.changedProperties == null ) {
            this.changedProperties = new HashSet<String>();
        }
        this.changedProperties.add(key);
        this.cache.put(key, value);
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
                final Entry<? extends String, ? extends Object> entry = (Entry<? extends String, ? extends Object>) i.next();
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        readFully();
        final Object oldValue = this.get(key);
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
        this.fullyRead = false;
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
            for(final String key : this.changedProperties) {
                if ( cache.containsKey(key) ) {
                    JcrResourceUtil.setProperty(node, key, this.cache.get(key));
                } else {
                    node.setProperty(key, (String)null);
                }
            }
            node.save();
        } catch (RepositoryException re) {
            throw new PersistenceException("Unable to persist changes.", re);
        }
        this.reset();
    }
}
