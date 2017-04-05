/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jackrabbit.usermanager.impl.resource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ValueMap implementation for Authorizable Resources
 */
public class AuthorizableValueMap implements ValueMap {

    private static final String DECLARED_MEMBERS_KEY = "declaredMembers";

    private static final String MEMBERS_KEY = "members";

    private static final String DECLARED_MEMBER_OF_KEY = "declaredMemberOf";

    private static final String MEMBER_OF_KEY = "memberOf";

    private static final String PATH_KEY = "path";

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizableValueMap.class);

    private boolean fullyRead;

    private final Map<String, Object> cache;

    private Authorizable authorizable;

    public AuthorizableValueMap(Authorizable authorizable) {
        this.authorizable = authorizable;
        this.cache = new LinkedHashMap<String, Object>();
        this.fullyRead = false;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        if (type == null) {
            return (T) get(name);
        }

        return convertToType(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if (defaultValue == null) {
            return (T) get(name);
        }

        // special handling in case the default value implements one
        // of the interface types supported by the convertToType method
        Class<T> type = (Class<T>) normalizeClass(defaultValue.getClass());

        T value = get(name, type);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        readFully();
        return cache.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        readFully();
        return cache.entrySet();
    }

    public Object get(Object key) {
        Object value = cache.get(key);
        if (value == null) {
            value = read((String) key);
        }

        return value;
    }

    public Set<String> keySet() {
        readFully();
        return cache.keySet();
    }

    public int size() {
        readFully();
        return cache.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Collection<Object> values() {
        readFully();
        return cache.values();
    }

    protected Object read(String key) {
        // if the item has been completely read, we need not check
        // again, as we certainly will not find the key
        if (fullyRead) {
            return null;
        }

        try {
            if (key.equals(MEMBERS_KEY) && authorizable.isGroup()) {
                return getMembers((Group) authorizable, true);
            }
            if (key.equals(DECLARED_MEMBERS_KEY) && authorizable.isGroup()) {
                return getMembers((Group) authorizable, false);
            }
            if (key.equals(MEMBER_OF_KEY)) {
                return getMemberships(true);
            }
            if (key.equals(DECLARED_MEMBER_OF_KEY)) {
                return getMemberships(false);
            }
            if (key.equals(PATH_KEY)) {
                return getPath();
            }
            if (authorizable.hasProperty(key)) {
                final Value[] property = authorizable.getProperty(key);
                final Object value = valuesToJavaObject(property);
                cache.put(key, value);
                return value;
            }
        } catch (RepositoryException re) {
            LOG.error("Could not access authorizable property", re);
        }

        // property not found or some error accessing it
        return null;
    }

    /**
     * Converts a JCR Value to a corresponding Java Object
     *
     * @param value the JCR Value to convert
     * @return the Java Object
     * @throws RepositoryException if the value cannot be converted
     */
    public static Object toJavaObject(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.DECIMAL:
                return value.getDecimal();
            case PropertyType.BINARY:
                return new LazyInputStream(value);
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME: // fall through
            case PropertyType.PATH: // fall through
            case PropertyType.REFERENCE: // fall through
            case PropertyType.STRING: // fall through
            case PropertyType.UNDEFINED: // not actually expected
            default: // not actually expected
                return value.getString();
        }
    }
    protected Object valuesToJavaObject(Value[] values)
            throws RepositoryException {
        if (values == null) {
            return null;
        } else if (values.length == 1) {
            return toJavaObject(values[0]);
        } else {
            Object[] valuesObjs = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                valuesObjs[i] = toJavaObject(values[i]);
            }
            return valuesObjs;
        }
    }

    protected void readFully() {
        if (!fullyRead) {
            try {
                if (authorizable.isGroup()) {
                    cache.put(MEMBERS_KEY, getMembers((Group) authorizable, true));
                    cache.put(DECLARED_MEMBERS_KEY, getMembers((Group) authorizable, false));
                }
                cache.put(MEMBER_OF_KEY, getMemberships(true));
                cache.put(DECLARED_MEMBER_OF_KEY, getMemberships(false));

                String path = getPath();
                if (path != null) {
                    cache.put(PATH_KEY, path);
                }
                // only direct property
                Iterator<String> pi = authorizable.getPropertyNames();
                while (pi.hasNext()) {
                    String key = (String) pi.next();
                    if (!cache.containsKey(key)) {
                        Value[] property = authorizable.getProperty(key);
                        Object value = valuesToJavaObject(property);
                        cache.put(key, value);
                    }
                }

                fullyRead = true;
            } catch (RepositoryException re) {
                LOG.error("Could not access certain properties of user {}", authorizable, re);
            }
        }
    }

    /**
     * Reads the authorizable map completely and returns the string
     * representation of the cached properties.
     */
    @Override
    public String toString() {
        readFully();
        return cache.toString();
    }

    // ---------- Unsupported Modification methods

    public Object remove(Object arg0) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object put(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> arg0) {
        throw new UnsupportedOperationException();
    }

    // ---------- Implementation helper

    @SuppressWarnings("unchecked")
    private <T> T convertToType(String name, Class<T> type) {
        T result = null;

        try {
            if (authorizable.hasProperty(name)) {
                Value[] values = authorizable.getProperty(name);

                if (values == null) {
                    return null;
                }

                boolean multiValue = values.length > 1;
                boolean array = type.isArray();

                if (multiValue) {
                    if (array) {
                        result = (T) convertToArray(values,
                            type.getComponentType());
                    } else if (values.length > 0) {
                        result = convertToType(-1, values[0], type);
                    }
                } else {
                    Value value = values[0];
                    if (array) {
                        result = (T) convertToArray(new Value[] { value },
                            type.getComponentType());
                    } else {
                        result = convertToType(-1, value, type);
                    }
                }
            }

        } catch (ValueFormatException vfe) {
            LOG.info("converToType: Cannot convert value of " + name
                + " to " + type, vfe);
        } catch (RepositoryException re) {
            LOG.info("converToType: Cannot get value of " + name, re);
        }

        // fall back to nothing
        return result;
    }

    private <T> T[] convertToArray(Value[] jcrValues, Class<T> type)
            throws ValueFormatException, RepositoryException {
        List<T> values = new ArrayList<T>();
        for (int i = 0; i < jcrValues.length; i++) {
            T value = convertToType(i, jcrValues[i], type);
            if (value != null) {
                values.add(value);
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());

        return values.toArray(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(int index, Value jcrValue, Class<T> type)
            throws ValueFormatException, RepositoryException {

        if (String.class == type) {
            return (T) jcrValue.getString();
        } else if (Byte.class == type) {
            return (T) new Byte((byte) jcrValue.getLong());
        } else if (Short.class == type) {
            return (T) new Short((short) jcrValue.getLong());
        } else if (Integer.class == type) {
            return (T) new Integer((int) jcrValue.getLong());
        } else if (Long.class == type) {
            return (T) new Long(jcrValue.getLong());
        } else if (Float.class == type) {
            return (T) new Float(jcrValue.getDouble());
        } else if (Double.class == type) {
            return (T) new Double(jcrValue.getDouble());
        } else if (Boolean.class == type) {
            return (T) Boolean.valueOf(jcrValue.getBoolean());
        } else if (Date.class == type) {
            return (T) jcrValue.getDate().getTime();
        } else if (Calendar.class == type) {
            return (T) jcrValue.getDate();
        } else if (Value.class == type) {
            return (T) jcrValue;
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

    private String[] getMembers(Group group, boolean includeAll) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        for (Iterator<Authorizable> it = includeAll ? group.getMembers() : group.getDeclaredMembers();
                it.hasNext();) {
            Authorizable auth = it.next();
            if (auth.isGroup()) {
                results.add(AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + auth.getID());
            } else {
                results.add(AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + auth.getID());
            }
        }
        return results.toArray(new String[results.size()]);
    }

    private String[] getMemberships(boolean includeAll) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        for (Iterator<Group> it = includeAll ? authorizable.memberOf() : authorizable.declaredMemberOf();
                it.hasNext();) {
            Group group = it.next();
            results.add(AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + group.getID());
        }
        return results.toArray(new String[results.size()]);
    }
    
    private String getPath() throws RepositoryException {
        try {
            return authorizable.getPath();
        } catch (UnsupportedRepositoryOperationException e) {
            LOG.debug("Could not retrieve path of authorizable {}", authorizable, e);
            return null;
        }
    }

    public static class LazyInputStream extends InputStream {

        /** The JCR Value from which the input stream is requested on demand */
        private final Value value;

        /** The inputstream created on demand, null if not used */
        private InputStream delegatee;

        public LazyInputStream(Value value) {
            this.value = value;
        }

        /**
         * Closes the input stream if acquired otherwise does nothing.
         */
        @Override
        public void close() throws IOException {
            if (delegatee != null) {
                delegatee.close();
            }
        }

        @Override
        public int available() throws IOException {
            return getStream().available();
        }

        @Override
        public int read() throws IOException {
            return getStream().read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return getStream().read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getStream().skip(n);
        }

        @Override
        public boolean markSupported() {
            try {
                return getStream().markSupported();
            } catch (IOException ioe) {
                // ignore
            }
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                getStream().mark(readlimit);
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            getStream().reset();
        }

        /** Actually retrieves the input stream from the underlying JCR Value */
        private InputStream getStream() throws IOException {
            if (delegatee == null) {
                try {
                    delegatee = value.getBinary().getStream();
                } catch (RepositoryException re) {
                    throw (IOException) new IOException(re.getMessage()).initCause(re);
                }
            }
            return delegatee;
        }

    }
}