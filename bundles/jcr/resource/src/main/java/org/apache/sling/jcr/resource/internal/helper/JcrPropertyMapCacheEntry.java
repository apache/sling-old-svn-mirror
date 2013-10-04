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
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.jcr.resource.JcrResourceUtil;

public class JcrPropertyMapCacheEntry {
    public final Property property;
    public final boolean isMulti;
    public final Value[] values;

    private final Object defaultValue;

    /**
     * Create a value for the object.
     * If the value type is supported directly through a jcr property type,
     * the corresponding value is created. If the value is serializable,
     * it is serialized through an object stream. Otherwise null is returned.
     */
    private Value createValue(final Object obj, final Session session)
    throws RepositoryException {
        Value value = JcrResourceUtil.createValue(obj, session);
        if ( value == null && obj instanceof Serializable ) {
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
     * Create a new cache entry from a property.
     */
    public JcrPropertyMapCacheEntry(final Property prop)
    throws RepositoryException {
        this.property = prop;
        if ( prop.isMultiple() ) {
            isMulti = true;
            values = prop.getValues();
        } else {
            isMulti = false;
            values = new Value[] {prop.getValue()};
        }
        Object tmp = JcrResourceUtil.toJavaObject(prop);
        if (isDefaultValueCacheable()) {
            this.defaultValue = tmp;
        } else {
            this.defaultValue = null;
        }
    }

    /**
     * Create a new cache entry from a value.
     */
    public JcrPropertyMapCacheEntry(final Object value, final Session session)
    throws RepositoryException {
        this.property = null;
        this.defaultValue = value;
        if ( value.getClass().isArray() ) {
            this.isMulti = true;
            final Object[] values = convertToObject(value);;
            this.values = new Value[values.length];
            for(int i=0; i<values.length; i++) {
                this.values[i] = this.createValue(values[i], session);
                if ( this.values[i] == null ) {
                    throw new IllegalArgumentException("Value can't be stored in the repository: " + values[i]);
                }
            }
        } else {
            this.isMulti = false;
            this.values = new Value[] {this.createValue(value, session)};
            if ( this.values[0] == null ) {
                throw new IllegalArgumentException("Value can't be stored in the repository: " + value);
            }
        }
    }

    private Object[] convertToObject(final Object value) {
        final Object[] values;
        if (value instanceof long[]) {
            values = ArrayUtils.toObject((long[])value);
        } else if (value instanceof int[]) {
            values = ArrayUtils.toObject((int[])value);
        } else if (value instanceof double[]) {
            values = ArrayUtils.toObject((double[])value);
        } else if (value instanceof byte[]) {
            values = ArrayUtils.toObject((byte[])value);
        } else if (value instanceof float[]) {
            values = ArrayUtils.toObject((float[])value);
        } else if (value instanceof short[]) {
            values = ArrayUtils.toObject((short[])value);
        } else if (value instanceof long[]) {
            values = ArrayUtils.toObject((long[])value);
        } else if (value instanceof boolean[]) {
            values = ArrayUtils.toObject((boolean[])value);
        } else if (value instanceof char[]) {
            values = ArrayUtils.toObject((char[])value);
        } else {
            values = (Object[]) value;
        }
        return values;
    }

    public Object getDefaultValue() throws RepositoryException {
        return this.defaultValue != null ? this.defaultValue : JcrResourceUtil.toJavaObject(property);
    }

    public Object getDefaultValueOrNull() {
        try {
            return getDefaultValue();
        } catch (RepositoryException e) {
            return null;
        }
    }

    private boolean isDefaultValueCacheable() throws RepositoryException {
        return property.getType() != PropertyType.BINARY;
    }


}