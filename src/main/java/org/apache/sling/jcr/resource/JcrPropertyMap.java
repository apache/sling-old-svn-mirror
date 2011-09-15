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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the value map based on a JCR node.
 * @see JcrModifiablePropertyMap
 */
public class JcrPropertyMap
    implements ValueMap {

    /** default logger */
    private static Logger LOGGER = LoggerFactory.getLogger(JcrPropertyMap.class);

    /** The underlying node. */
    private final Node node;
    
    /** A cache for the properties. */
    final Map<String, JcrPropertyMapCacheEntry> cache;

    /** A cache for the values. */
    final Map<String, Object> valueCache;

    /** Has the node been read completly? */
    boolean fullyRead;

    private final ClassLoader dynamicClassLoader;

    /**
     * Constructor
     * @param node The underlying node.
     */
    public JcrPropertyMap(final Node node) {
        this(node, null);
    }

    /**
     * Constructor
     * @param node The underlying node.
     * @param dynamicCL Dynamic class loader for loading serialized objects.
     * @since 2.0.8
     */
    public JcrPropertyMap(final Node node, final ClassLoader dynamicCL) {
        this.node = node;
        this.cache = new LinkedHashMap<String, JcrPropertyMapCacheEntry>();
        this.valueCache = new LinkedHashMap<String, Object>();
        this.fullyRead = false;
        this.dynamicClassLoader = dynamicCL;
    }

    /**
     * Get the node.
     */
    Node getNode() {
        return node;
    }

    // ---------- ValueMap

    String checkKey(final String key) {
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

        JcrPropertyMapCacheEntry entry = cache.get(key);
        if (entry == null) {
            entry = read(key);
        }
        if ( entry == null ) {
            return null;
        }
        return convertToType(entry, type);
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
        JcrPropertyMapCacheEntry entry = cache.get(key);
        if (entry == null) {
            entry = read(key);
        }
        final Object value = (entry == null ? null : entry.getDefaultValueOrNull());
        return value;
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        readFully();
        return valueCache.containsValue(value);
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
        return cache.size();
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        readFully();
        if (cache.size() == valueCache.size()) {
            return valueCache.entrySet();
        } else {
            Set<Map.Entry<String, Object>> result = new LinkedHashSet<Map.Entry<String, Object>>(cache.size());
            for (Map.Entry<String, JcrPropertyMapCacheEntry> entry : cache.entrySet()) {
                try {
                    Map.Entry<String, Object> newEntry = new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), entry.getValue().getDefaultValue());
                    result.add(newEntry);
                } catch (RepositoryException e) {
                    
                }
            }
            return result;
        }
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        readFully();
        return cache.keySet();
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        readFully();
        if (cache.size() == valueCache.size()) {
            return valueCache.values();
        } else {
            Set<Object> values = new LinkedHashSet<Object>(cache.size());
            for (JcrPropertyMapCacheEntry entry : cache.values()) {
                try {
                    values.add(entry.getDefaultValue());
                } catch (RepositoryException e) {
                }
            }
            return values;
        }
    }

    /**
     * Return the path of the current node.
     *
     * @throws IllegalStateException If a repository exception occurs
     * @deprecated
     */
    @Deprecated
    public String getPath() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- Helpers to access the node's property ------------------------

    JcrPropertyMapCacheEntry read(final String key) {

        // if the node has been completely read, we need not check
        // again if the key does not point to a sub node
        if (fullyRead && key.indexOf('/') == -1 ) {
            // except if the key contains
            return null;
        }

        final String name = ISO9075.encodePath(key);
        try {
            if (node.hasProperty(name)) {
                final Property prop = node.getProperty(name);
                final JcrPropertyMapCacheEntry entry = new JcrPropertyMapCacheEntry(prop);
                cache.put(key, entry);
                Object defaultValue = entry.getDefaultValue();
                if (defaultValue != null) {
                    valueCache.put(key, entry.getDefaultValue());
                }
                return entry;
            }
        } catch (RepositoryException re) {
            // TODO: log !!
        }

        // property not found or some error accessing it
        return null;
    }

    void readFully() {
        if (!fullyRead) {
            try {
                PropertyIterator pi = node.getProperties();
                while (pi.hasNext()) {
                    Property prop = pi.nextProperty();
                    final String name = prop.getName();
                    final String key = ISO9075.decode(name);
                    if (!cache.containsKey(key)) {
                        final JcrPropertyMapCacheEntry entry = new JcrPropertyMapCacheEntry(prop);
                        cache.put(key, entry);
                        Object defaultValue = entry.getDefaultValue();
                        if (defaultValue != null) {
                            valueCache.put(key, entry.getDefaultValue());
                        }
                    }
                }
                fullyRead = true;
            } catch (RepositoryException re) {
                // TODO: log !!
            }
        }
    }

    // ---------- Unsupported Modification methods

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    // ---------- Implementation helper

    @SuppressWarnings("unchecked")
    private <T> T convertToType(final JcrPropertyMapCacheEntry entry, Class<T> type) {
        T result = null;

        try {
            final boolean array = type.isArray();

            if (entry.isMulti) {

                if (array) {

                    result = (T) convertToArray(entry,
                        type.getComponentType());

                } else if (entry.values.length > 0) {

                    result = convertToType(entry, -1, entry.values[0], type);

                }

            } else {

                if (array) {

                    result = (T) convertToArray(entry,
                            type.getComponentType());

                } else {

                    result = convertToType(entry, -1, entry.values[0], type);

                }
            }

        } catch (ValueFormatException vfe) {
            LOGGER.info("converToType: Cannot convert value of " + entry.getDefaultValueOrNull()
                + " to " + type, vfe);
        } catch (RepositoryException re) {
            LOGGER.info("converToType: Cannot get value of " + entry.getDefaultValueOrNull(), re);
        }

        // fall back to nothing
        return result;
    }

    private <T> T[] convertToArray(final JcrPropertyMapCacheEntry entry, Class<T> type)
    throws ValueFormatException, RepositoryException {
        List<T> values = new ArrayList<T>();
        for (int i = 0; i < entry.values.length; i++) {
            T value = convertToType(entry, i, entry.values[i], type);
            if (value != null) {
                values.add(value);
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());

        return values.toArray(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(final JcrPropertyMapCacheEntry entry,
                                final int index,
                                final Value jcrValue,
                                final Class<T> type)
    throws ValueFormatException, RepositoryException {
        Object defaultValue = entry.getDefaultValue();
        if ( type.isInstance(defaultValue) ) {
            return (T) defaultValue;
        }

        if (String.class == type) {
            return (T) jcrValue.getString();

        } else if (Byte.class == type) {
            return (T) Byte.valueOf((byte) jcrValue.getLong());

        } else if (Short.class == type) {
            return (T) Short.valueOf((short) jcrValue.getLong());

        } else if (Integer.class == type) {
            return (T) Integer.valueOf((int) jcrValue.getLong());

        } else if (Long.class == type) {
            if (jcrValue.getType() == PropertyType.BINARY) {
                if (index == -1) {
                    return (T) Long.valueOf(entry.property.getLength());
                }
                return (T) Long.valueOf(entry.property.getLengths()[index]);
            }
            return (T) Long.valueOf(jcrValue.getLong());

        } else if (Float.class == type) {
            return (T) Float.valueOf((float) jcrValue.getDouble());

        } else if (Double.class == type) {
            return (T) Double.valueOf(jcrValue.getDouble());

        } else if (Boolean.class == type) {
            return (T) Boolean.valueOf(jcrValue.getBoolean());

        } else if (Date.class == type) {
            return (T) jcrValue.getDate().getTime();

        } else if (Calendar.class == type) {
            return (T) jcrValue.getDate();

        } else if (Value.class == type) {
            return (T) jcrValue;

        } else if (Property.class == type) {
            return (T) entry.property;

        } else if (Serializable.class.isAssignableFrom(type)
                && jcrValue.getType() == PropertyType.BINARY) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(jcrValue.getStream(), this.dynamicClassLoader);
                final Object obj = ois.readObject();
                if ( type.isInstance(obj) ) {
                    return (T)obj;
                }
            } catch (ClassNotFoundException cnfe) {
                 // ignore and use fallback
            } catch (IOException ioe) {
                // ignore and use fallback
            } finally {
                if ( ois != null ) {
                    try {
                        ois.close();
                    } catch (IOException ignore) {
                        // ignore
                    }
                }
            }
        }

        // fallback in case of unsupported type
        return null;
    }

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

    /**
     * This is an extended version of the object input stream which uses the
     * thread context class loader.
     */
    private static class ObjectInputStream extends java.io.ObjectInputStream {

        private ClassLoader classloader;

        public ObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classloader = classLoader;
        }

        /**
         * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
         */
        @Override
        protected Class<?> resolveClass(java.io.ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
            if ( this.classloader != null ) {
                return this.classloader.loadClass(classDesc.getName());
            }
            return super.resolveClass(classDesc);
        }
    }
}
