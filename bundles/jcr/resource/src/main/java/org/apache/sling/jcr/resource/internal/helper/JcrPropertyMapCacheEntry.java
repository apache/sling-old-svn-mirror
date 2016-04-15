/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPropertyMapCacheEntry {

    /** Global logger */
    private static Logger LOGGER = LoggerFactory.getLogger(JcrPropertyMapCacheEntry.class);

    /**
     * The JCR property - only set for existing values.
     */
    private final Property property;

    /**
     * Whether this is an array or a single value.
     */
    private final boolean isArray;

    /**
     * The value of the object.
     */
    private final Object propertyValue;

    /**
     * Create a new cache entry from a property.
     *
     * @param prop the property
     * @throws RepositoryException if the provided property cannot be converted to a Java Object
     */
    public JcrPropertyMapCacheEntry(final Property prop)
            throws RepositoryException {
        this.property = prop;
        this.isArray = prop.isMultiple();
        if (property.getType() != PropertyType.BINARY) {
            this.propertyValue = JcrResourceUtil.toJavaObject(prop);
        } else {
            this.propertyValue = null;
        }
    }

    /**
     * Create a new cache entry from a value.
     *
     * @param value the value
     * @param node the node
     * @throws RepositoryException if the provided value cannot be stored
     */
    public JcrPropertyMapCacheEntry(final Object value, final Node node)
            throws RepositoryException {
        this.property = null;
        this.propertyValue = value;
        this.isArray = value.getClass().isArray();
        // check if values can be stored in JCR
        if (isArray) {
            final Object[] values = convertToObjectArray(value);
            for (int i = 0; i < values.length; i++) {
                failIfCannotStore(values[i], node);
            }
        } else {
            failIfCannotStore(value, node);
        }
    }

    private void failIfCannotStore(final Object value, final Node node)
            throws RepositoryException {
        if (value instanceof InputStream) {
            // InputStream is storable and calling createValue for nothing
            // eats its contents
            return;
        }
        final Value val = this.createValue(value, node);
        if (val == null) {
            throw new IllegalArgumentException("Value can't be stored in the repository: " + value);
        }
    }

    /**
     * Create a value for the object. If the value type is supported directly through a jcr property type, the
     * corresponding value is created. If the value is serializable, it is serialized through an object stream.
     * Otherwise null is returned.
     *
     * @param obj the object
     * @param node the node
     * @return the converted value
     */
    private Value createValue(final Object obj, final Node node)
            throws RepositoryException {
        final Session session = node.getSession();
        Value value = JcrResourceUtil.createValue(obj, session);
        if (value == null && obj instanceof Serializable) {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(obj);
                oos.close();
                final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                value = session.getValueFactory().createValue(session.getValueFactory().createBinary(bais));
            } catch (IOException ioe) {
                // we ignore this here and return null
            }
        }
        return value;
    }

    /**
     * Convert the object to an array
     *
     * @param value The array
     * @return an object array
     */
    private Object[] convertToObjectArray(final Object value) {
        final Object[] values;
        if (value instanceof long[]) {
            values = ArrayUtils.toObject((long[]) value);
        } else if (value instanceof int[]) {
            values = ArrayUtils.toObject((int[]) value);
        } else if (value instanceof double[]) {
            values = ArrayUtils.toObject((double[]) value);
        } else if (value instanceof byte[]) {
            values = ArrayUtils.toObject((byte[]) value);
        } else if (value instanceof float[]) {
            values = ArrayUtils.toObject((float[]) value);
        } else if (value instanceof short[]) {
            values = ArrayUtils.toObject((short[]) value);
        } else if (value instanceof boolean[]) {
            values = ArrayUtils.toObject((boolean[]) value);
        } else if (value instanceof char[]) {
            values = ArrayUtils.toObject((char[]) value);
        } else {
            values = (Object[]) value;
        }
        return values;
    }

    /**
     * Whether this value is an array or not
     *
     * @return {@code true} if an array.
     */
    public boolean isArray() {
        return this.isArray;
    }

    /**
     * Get the current property value.
     *
     * @return The current value
     * @throws RepositoryException If something goes wrong
     */
    public Object getPropertyValue() throws RepositoryException {
        return this.propertyValue != null ? this.propertyValue : JcrResourceUtil.toJavaObject(property);
    }

    /**
     * Get the current property value.
     *
     * @return The current value or {@code null} if not possible.
     */
    public Object getPropertyValueOrNull() {
        try {
            return getPropertyValue();
        } catch (final RepositoryException e) {
            return null;
        }
    }

    /**
     * Convert the default value to the given type
     *
     * @param type The type class
     * @param node The node
     * @param dynamicClassLoader The classloader
     * @param <T> The type
     * @return The converted object
     */
    @SuppressWarnings("unchecked")
    public <T> T convertToType(final Class<T> type,
            final Node node,
            final ClassLoader dynamicClassLoader) {
        T result = null;

        try {
            final boolean targetIsArray = type.isArray();

            if (this.isArray) {

                final Object[] sourceArray = convertToObjectArray(this.getPropertyValue());
                if (targetIsArray) {
                    result = (T) convertToArray(sourceArray, type.getComponentType(), node, dynamicClassLoader);
                } else if (sourceArray.length > 0) {
                    result = convertToType(-1, sourceArray[0], type, node, dynamicClassLoader);
                }

            } else {

                final Object sourceObject = this.getPropertyValue();
                if (targetIsArray) {
                    result = (T) convertToArray(new Object[]{sourceObject}, type.getComponentType(), node, dynamicClassLoader);
                } else {
                    result = convertToType(-1, sourceObject, type, node, dynamicClassLoader);
                }
            }

        } catch (final NumberFormatException vfe) {
            LOGGER.info("converToType: Cannot convert value of " + this.getPropertyValueOrNull()
                    + " to " + type, vfe);
        } catch (final IllegalArgumentException vfe) {
            LOGGER.info("converToType: Cannot convert value of " + this.getPropertyValueOrNull()
                    + " to " + type, vfe);
        } catch (final ValueFormatException vfe) {
            LOGGER.info("converToType: Cannot convert value of " + this.getPropertyValueOrNull()
                    + " to " + type, vfe);
        } catch (RepositoryException re) {
            LOGGER.info("converToType: Cannot get value of " + this.getPropertyValueOrNull(), re);
        }

        // fall back to nothing
        return result;
    }

    private <T> T[] convertToArray(final Object[] sourceArray,
            final Class<T> type,
            final Node node,
            final ClassLoader dynamicClassLoader)
            throws RepositoryException {
        List<T> values = new ArrayList<T>();
        for (int i = 0; i < sourceArray.length; i++) {
            T value = convertToType(i, sourceArray[i], type, node, dynamicClassLoader);
            if (value != null) {
                values.add(value);
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());

        return values.toArray(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(final int index,
            final Object initialValue,
            final Class<T> type,
            final Node node,
            final ClassLoader dynamicClassLoader)
            throws RepositoryException {
        if (type.isInstance(initialValue)) {
            return (T) initialValue;
        }

        Object value = initialValue;

        // special case input stream first
        if (value instanceof InputStream) {
            InputStream is = (InputStream) value;
            try {
                is.reset();
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            // object input stream
            if (ObjectInputStream.class.isAssignableFrom(type)) {
                try {
                    return (T) new PropertyObjectInputStream(is, dynamicClassLoader);
                } catch (final IOException ioe) {
                    // ignore and use fallback
                }

                // any number: length of binary
            } else if (Number.class.isAssignableFrom(type)) {
                if (index == -1) {
                    value = Long.valueOf(this.property.getLength());
                } else {
                    value = Long.valueOf(this.property.getLengths()[index]);
                }

                // string: read binary
            } else if (String.class == type) {

                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[2048];
                    int l;
                    while ((l = is.read(buffer)) >= 0) {
                        if (l > 0) {
                            baos.write(buffer, 0, l);
                        }
                    }
                    value = new String(baos.toByteArray(), "UTF-8");
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                } finally {
                    try {
                        is.close();
                    } catch (final IOException ignore) {
                        // ignore
                    }
                }

                // any serializable
            } else if (Serializable.class.isAssignableFrom(type)) {
                ObjectInputStream ois = null;
                try {
                    ois = new PropertyObjectInputStream(is, dynamicClassLoader);
                    final Object obj = ois.readObject();
                    if (type.isInstance(obj)) {
                        return (T) obj;
                    }
                    value = obj;
                } catch (final ClassNotFoundException cnfe) {
                    // ignore and use fallback
                } catch (final IOException ioe) {
                    // ignore and use fallback
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (final IOException ignore) {
                            // ignore
                        }
                    }
                }
            }
        }

        if (String.class == type) {
            return (T) getConverter(value).toString();

        } else if (Byte.class == type) {
            return (T) getConverter(value).toByte();

        } else if (Short.class == type) {
            return (T) getConverter(value).toShort();

        } else if (Integer.class == type) {
            return (T) getConverter(value).toInteger();

        } else if (Long.class == type) {
            return (T) getConverter(value).toLong();

        } else if (Float.class == type) {
            return (T) getConverter(value).toFloat();

        } else if (Double.class == type) {
            return (T) getConverter(value).toDouble();

        } else if (BigDecimal.class == type) {
            return (T) getConverter(value).toBigDecimal();

        } else if (Boolean.class == type) {
            return (T) getConverter(value).toBoolean();

        } else if (Date.class == type) {
            return (T) getConverter(value).toDate();

        } else if (Calendar.class == type) {
            return (T) getConverter(value).toCalendar();

        } else if (Value.class == type) {
            return (T) this.createValue(value, node);

        } else if (Property.class == type) {
            return (T) this.property;
        }

        // fallback in case of unsupported type
        return null;
    }

    /**
     * Create a converter for an object.
     *
     * @param value The object to convert
     * @return A converter for {@code value}
     */
    private Converter getConverter(final Object value) {
        if (value instanceof Number) {
            // byte, short, int, long, double, float, BigDecimal
            return new NumberConverter((Number) value);
        } else if (value instanceof Boolean) {
            return new BooleanConverter((Boolean) value);
        } else if (value instanceof Date) {
            return new DateConverter((Date) value);
        } else if (value instanceof Calendar) {
            return new CalendarConverter((Calendar) value);
        }
        // default string based
        return new StringConverter(value);
    }

    /**
     * This is an extended version of the object input stream which uses the thread context class loader.
     */
    private static class PropertyObjectInputStream extends ObjectInputStream {

        private final ClassLoader classloader;

        public PropertyObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classloader = classLoader;
        }

        /**
         * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
         */
        @Override
        protected Class<?> resolveClass(final ObjectStreamClass classDesc)
                throws IOException, ClassNotFoundException {
            if (this.classloader != null) {
                return this.classloader.loadClass(classDesc.getName());
            }
            return super.resolveClass(classDesc);
        }
    }
}
