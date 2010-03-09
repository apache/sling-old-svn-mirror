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
package org.apache.sling.commons.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * The <code>OsgiUtil</code> is a utility class providing some usefull utility
 * methods.
 */
public class OsgiUtil {

    /**
     * Returns the boolean value of the parameter or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * If the parameter is not a <code>Boolean</code> it is converted
     * by calling <code>Boolean.valueOf</code> on the string value of the
     * object.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default boolean value
     */
    public static boolean toBoolean(Object propValue, boolean defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Boolean) {
            return (Boolean) propValue;
        } else if (propValue != null) {
            return Boolean.valueOf(String.valueOf(propValue));
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a string or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default string value
     */
    public static String toString(Object propValue, String defaultValue) {
        propValue = toObject(propValue);
        return (propValue != null) ? propValue.toString() : defaultValue;
    }

    /**
     * Returns the parameter as a long or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Long</code> and cannot be converted to
     * a <code>Long</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default long value
     */
    public static long toLong(Object propValue, long defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Long) {
            return (Long) propValue;
        } else if (propValue != null) {
            try {
                return Long.valueOf(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as an integer or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not an <code>Integer</code> and cannot be converted to
     * an <code>Integer</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default integer value
     */
    public static int toInteger(Object propValue, int defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Integer) {
            return (Integer) propValue;
        } else if (propValue != null) {
            try {
                return Integer.valueOf(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a double or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Double</code> and cannot be converted to
     * a <code>Double</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default double value
     *
     * @deprecated since 2.0.4, use {@link #toDouble(Object, double)} instead
     */
    @Deprecated
    public static double getProperty(Object propValue, double defaultValue) {
        return toDouble(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a double or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Double</code> and cannot be converted to
     * a <code>Double</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default double value
     *
     * @since 2.0.4
     */
    public static double toDouble(Object propValue, double defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Double) {
            return (Double) propValue;
        } else if (propValue != null) {
            try {
                return Double.valueOf(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a single value. If the
     * parameter is neither an array nor a <code>java.util.Collection</code> the
     * parameter is returned unmodified. If the parameter is a non-empty array,
     * the first array element is returned. If the property is a non-empty
     * <code>java.util.Collection</code>, the first collection element is returned.
     * Otherwise <code>null</code> is returned.
     * @param propValue the parameter to convert.
     */
    public static Object toObject(Object propValue) {
        if (propValue == null) {
            return null;
        } else if (propValue.getClass().isArray()) {
            Object[] prop = (Object[]) propValue;
            return prop.length > 0 ? prop[0] : null;
        } else if (propValue instanceof Collection<?>) {
            Collection<?> prop = (Collection<?>) propValue;
            return prop.isEmpty() ? null : prop.iterator().next();
        } else {
            return propValue;
        }
    }

    /**
     * Returns the parameter as an array of Strings. If
     * the parameter is a scalar value its string value is returned as a single
     * element array. If the parameter is an array, the elements are converted to
     * String objects and returned as an array. If the parameter is a collection, the
     * collection elements are converted to String objects and returned as an array.
     * Otherwise (if the parameter is <code>null</code>) <code>null</code> is
     * returned.
     * @param propValue The object to convert.
     */
    public static String[] toStringArray(Object propValue) {
        return toStringArray(propValue, null);
    }

    /**
     * Returns the parameter as an array of Strings. If
     * the parameter is a scalar value its string value is returned as a single
     * element array. If the parameter is an array, the elements are converted to
     * String objects and returned as an array. If the parameter is a collection, the
     * collection elements are converted to String objects and returned as an array.
     * Otherwise (if the property is <code>null</code>) a provided default value is
     * returned.
     * @since 2.0.4
     * @param propValue The object to convert.
     * @param defaultArray The default array to return.
     */
    public static String[] toStringArray(Object propValue, String[] defaultArray) {
        if (propValue == null) {
            // no value at all
            return defaultArray;

        } else if (propValue instanceof String) {
            // single string
            return new String[] { (String) propValue };

        } else if (propValue instanceof String[]) {
            // String[]
            return (String[]) propValue;

        } else if (propValue.getClass().isArray()) {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray) {
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        } else if (propValue instanceof Collection<?>) {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection) {
                if (value != null) {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return defaultArray;
    }

    /**
     * Create an osgi event with the given topic and properties.
     * If a bundle parameter is provided the symbolic name is added
     * as a property.
     * If a service parameter is provided, information about the service
     * is added to the properties.
     * @param sourceBundle Optional source bundle
     * @param sourceService Optional source service
     * @param topic The event topic.
     * @param props A non-null map of properties for the event.
     * @return The OSGi event.
     */
    public static Event createEvent(Bundle sourceBundle,
            ServiceReference sourceService, String topic,
            Map<String, Object> props) {

        // get a private copy of the properties
        Dictionary<String, Object> table = new Hashtable<String, Object>(props);

        // service information of the provide service reference
        if (sourceService != null) {
            table.put(EventConstants.SERVICE, sourceService);
            table.put(
                EventConstants.SERVICE_ID,
                sourceService.getProperty(org.osgi.framework.Constants.SERVICE_ID));
            table.put(
                EventConstants.SERVICE_OBJECTCLASS,
                sourceService.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
            if (sourceService.getProperty(org.osgi.framework.Constants.SERVICE_PID) != null) {
                table.put(
                    EventConstants.SERVICE_PID,
                    sourceService.getProperty(org.osgi.framework.Constants.SERVICE_PID));
            }
        }

        // source bundle information (if available)
        if (sourceBundle != null) {
            table.put(EventConstants.BUNDLE_SYMBOLICNAME,
                sourceBundle.getSymbolicName());
        }

        // timestamp the event
        table.put(EventConstants.TIMESTAMP,
            new Long(System.currentTimeMillis()));

        // create the event
        return new Event(topic, table);
    }

    /**
     * Return the service ranking
     * @param props A property map
     * @return The service ranking.
     * @since 2.0.6
     */
    public static int getServiceRanking(final Map<String, Object> props) {
        int ranking = 0;
        if ( props != null && props.get(Constants.SERVICE_RANKING) instanceof Integer) {
            ranking = (Integer)props.get(Constants.SERVICE_RANKING);
        }
        return ranking;
    }

    /**
     * Return the service ranking
     * @param ref The service reference.
     * @return The service ranking.
     * @since 2.0.6
     */
    public static int getServiceRanking(final ServiceReference ref) {
        int ranking = 0;
        if ( ref.getProperty(Constants.SERVICE_RANKING) instanceof Integer) {
            ranking = (Integer)ref.getProperty(Constants.SERVICE_RANKING);
        }
        return ranking;
    }
}
