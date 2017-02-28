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
package org.apache.sling.fsprovider.internal.mapper.jcr;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.sling.api.resource.ValueMap;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
class FsValue implements Value {
    
    private final ValueMap props;
    private final String propertyName;
    private final int arrayIndex;
    
    public FsValue(ValueMap props, String propertyName) {
        this.props = props;
        this.propertyName = propertyName;
        this.arrayIndex = -1;
    }

    public FsValue(ValueMap props, String propertyName, int arrayIndex) {
        this.props = props;
        this.propertyName = propertyName;
        this.arrayIndex = arrayIndex;
    }

    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, String[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, String.class);
        }
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, Long[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, 0L);
        }
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, Double[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, 0d);
        }
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, BigDecimal[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, BigDecimal.ZERO);
        }
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, Calendar[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, Calendar.class);
        }
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        if (arrayIndex >= 0) {
            return props.get(propertyName, Boolean[].class)[arrayIndex];
        }
        else {
            return props.get(propertyName, false);
        }
    }

    @Override
    public int getType() {
        Object value = props.get(propertyName);
        if (value == null) {
            return PropertyType.UNDEFINED;
        }
        Class type = value.getClass();
        if (type.isArray() && Array.getLength(value) > 0) {
            Object firstItem = Array.get(value, 0);
            if (firstItem != null) {
                type = firstItem.getClass();
            }
        }
        if (type == String.class) {
            return PropertyType.STRING;
        }
        if (type == Boolean.class || type == boolean.class) {
            return PropertyType.BOOLEAN;
        }
        if (type == BigDecimal.class) {
            return PropertyType.DECIMAL;
        }
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            return PropertyType.DOUBLE;
        }
        if (Number.class.isAssignableFrom(type)) {
            return PropertyType.LONG;
        }
        if (type == Calendar.class) {
            return PropertyType.DATE;
        }
        return PropertyType.UNDEFINED;
    }


    // --- unsupported methods ---
    
    @Override
    public InputStream getStream() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
