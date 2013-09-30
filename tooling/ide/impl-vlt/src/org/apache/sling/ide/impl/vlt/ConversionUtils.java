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
package org.apache.sling.ide.impl.vlt;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public abstract class ConversionUtils {

    public static Object getPropertyValue(Property property) throws RepositoryException {

        Object propertyValue = null;

        // TODO - handle isMultiple() == true
        switch (property.getType()) {
            case PropertyType.BOOLEAN:
                if (property.isMultiple()) {
                    propertyValue = toBooleanArray(property.getValues());
                } else {
                    propertyValue = property.getBoolean();
                }
                break;

            case PropertyType.DATE:
                if (property.isMultiple()) {
                    propertyValue = toCalendarArray(property.getValues());
                } else {
                    propertyValue = property.getDate();
                }
                break;

            case PropertyType.DECIMAL:
                if (property.isMultiple()) {
                    propertyValue = toDecimalArray(property.getValues());
                } else {
                    propertyValue = property.getDecimal();
                }
                break;

            case PropertyType.DOUBLE:
                if (property.isMultiple()) {
                    propertyValue = toDoubleArray(property.getValues());
                } else {
                    propertyValue = property.getDouble();
                }
                break;

            case PropertyType.LONG:
                if (property.isMultiple()) {
                    propertyValue = toLongArray(property.getValues());
                } else {
                    propertyValue = property.getLong();
                }
                break;

            case PropertyType.PATH:
            case PropertyType.STRING:
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
            case PropertyType.NAME:
                if (property.isMultiple()) {
                    propertyValue = toStringArray(property.getValues());
                } else {
                    propertyValue = property.getString();
                }
                break;

            case PropertyType.BINARY:
                // explicitly skip, not part of the ResourceProxy abstraction
                break;

            default:
                // TODO warn if property type not known
                break;

        }
        return propertyValue;
    }

    private static String[] toStringArray(Value[] values) throws RepositoryException {

        String[] ret = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getString();
        }
        return ret;
    }

    private static Boolean[] toBooleanArray(Value[] values) throws RepositoryException {

        Boolean[] ret = new Boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getBoolean();
        }

        return ret;
    }

    private static Calendar[] toCalendarArray(Value[] values) throws RepositoryException {

        Calendar[] ret = new Calendar[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getDate();
        }

        return ret;
    }

    private static BigDecimal[] toDecimalArray(Value[] values) throws RepositoryException {

        BigDecimal[] ret = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getDecimal();
        }

        return ret;
    }

    private static Double[] toDoubleArray(Value[] values) throws RepositoryException {

        Double[] ret = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getDouble();
        }

        return ret;
    }

    private static Long[] toLongArray(Value[] values) throws RepositoryException {

        Long[] ret = new Long[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getLong();
        }

        return ret;
    }

    private ConversionUtils() {

    }
}
