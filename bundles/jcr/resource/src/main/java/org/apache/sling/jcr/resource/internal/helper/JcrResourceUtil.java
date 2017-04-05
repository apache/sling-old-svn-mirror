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
package org.apache.sling.jcr.resource.internal.helper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * The <code>JcrResourceUtil</code> class provides helper methods used
 * throughout this bundle.
 *
 */
public class JcrResourceUtil {

    /**
     * Helper method to execute a JCR query.
     *
     * @param session the session
     * @param query the query
     * @param language the language
     * @return the query's result
     * @throws RepositoryException if the {@link QueryManager} cannot be retrieved
     */
    public static QueryResult query(Session session, String query,
            String language) throws RepositoryException {
        QueryManager qManager = session.getWorkspace().getQueryManager();
        Query q = qManager.createQuery(query, language);
        return q.execute();
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

    /**
     * Converts the value(s) of a JCR Property to a corresponding Java Object.
     * If the property has multiple values the result is an array of Java
     * Objects representing the converted values of the property.
     *
     * @param property the property to be converted to the corresponding Java Object
     * @throws RepositoryException if the conversion cannot take place
     * @return the Object resulting from the conversion
     */
    public static Object toJavaObject(Property property)
            throws RepositoryException {
        // multi-value property: return an array of values
        if (property.isMultiple()) {
            Value[] values = property.getValues();
            final Object firstValue = values.length > 0 ? toJavaObject(values[0]) : null;
            final Object[] result;
            if ( firstValue instanceof Boolean ) {
                result = new Boolean[values.length];
            } else if ( firstValue instanceof Calendar ) {
                result = new Calendar[values.length];
            } else if ( firstValue instanceof Double ) {
                result = new Double[values.length];
            } else if ( firstValue instanceof Long ) {
                result = new Long[values.length];
            } else if ( firstValue instanceof BigDecimal) {
                result = new BigDecimal[values.length];
            } else if ( firstValue instanceof InputStream) {
                result = new Object[values.length];
            } else {
                result = new String[values.length];
            }
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                if (value != null) {
                    result[i] = toJavaObject(value);
                }
            }
            return result;
        }

        // single value property
        return toJavaObject(property.getValue());
    }

    /**
     * Creates a {@link javax.jcr.Value JCR Value} for the given object with
     * the given Session.
     * Selects the the {@link javax.jcr.PropertyType PropertyType} according
     * the instance of the object's Class
     *
     * @param value object
     * @param session to create value for
     * @return the value or null if not convertible to a valid PropertyType
     * @throws RepositoryException in case of error, accessing the Repository
     */
    public static Value createValue(final Object value, final Session session)
    throws RepositoryException {
        Value val;
        ValueFactory fac = session.getValueFactory();
        if(value instanceof Calendar) {
            val = fac.createValue((Calendar)value);
        } else if (value instanceof InputStream) {
            val = fac.createValue(fac.createBinary((InputStream)value));
        } else if (value instanceof Node) {
            val = fac.createValue((Node)value);
        } else if (value instanceof BigDecimal) {
            val = fac.createValue((BigDecimal)value);
        } else if (value instanceof Long) {
            val = fac.createValue((Long)value);
        } else if (value instanceof Short) {
            val = fac.createValue((Short)value);
        } else if (value instanceof Integer) {
            val = fac.createValue((Integer)value);
        } else if (value instanceof Number) {
            val = fac.createValue(((Number)value).doubleValue());
        } else if (value instanceof Boolean) {
            val = fac.createValue((Boolean) value);
        } else if ( value instanceof String ) {
            val = fac.createValue((String)value);
        } else {
            val = null;
        }
        return val;
    }
}
