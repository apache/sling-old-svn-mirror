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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * The <code>OsgiUtil</code> is a utility class providing some usefull utility
 * methods.
 * @deprecated Use PropertiesUtil and ServiceUtil instead
 */
@Deprecated
public class OsgiUtil {

    /**
     * Returns the boolean value of the parameter or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * If the parameter is not a <code>Boolean</code> it is converted
     * by calling <code>Boolean.valueOf</code> on the string value of the
     * object.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default boolean value
     * @return Boolean value
     */
    public static boolean toBoolean(final Object propValue, final boolean defaultValue) {
        return PropertiesUtil.toBoolean(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a string or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default string value
     * @return String value
     */
    public static String toString(final Object propValue, final String defaultValue) {
        return PropertiesUtil.toString(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a long or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Long</code> and cannot be converted to
     * a <code>Long</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default long value
     * @return Long value
     */
    public static long toLong(final Object propValue, final long defaultValue) {
        return PropertiesUtil.toLong(propValue, defaultValue);
    }

    /**
     * Returns the parameter as an integer or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not an <code>Integer</code> and cannot be converted to
     * an <code>Integer</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default integer value
     * @return Integer value
     */
    public static int toInteger(final Object propValue, final int defaultValue) {
        return PropertiesUtil.toInteger(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a double or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Double</code> and cannot be converted to
     * a <code>Double</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default double value
     * @return Double value
     *
     * @deprecated since 2.0.4, use {@link #toDouble(Object, double)} instead
     */
    @Deprecated
    public static double getProperty(final Object propValue, final double defaultValue) {
        return PropertiesUtil.toDouble(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a double or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Double</code> and cannot be converted to
     * a <code>Double</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default double value
     * @return Double value
     *
     * @since 2.0.4
     */
    public static double toDouble(final Object propValue, final double defaultValue) {
        return PropertiesUtil.toDouble(propValue, defaultValue);
    }

    /**
     * Returns the parameter as a single value. If the
     * parameter is neither an array nor a <code>java.util.Collection</code> the
     * parameter is returned unmodified. If the parameter is a non-empty array,
     * the first array element is returned. If the property is a non-empty
     * <code>java.util.Collection</code>, the first collection element is returned.
     * Otherwise <code>null</code> is returned.
     * @param propValue the parameter to convert.
     * @return Object value
     */
    public static Object toObject(final Object propValue) {
        return PropertiesUtil.toObject(propValue);
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
     * @return String array value
     */
    public static String[] toStringArray(final Object propValue) {
        return PropertiesUtil.toStringArray(propValue);
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
     * @return String array value
     */
    public static String[] toStringArray(final Object propValue, final String[] defaultArray) {
        return PropertiesUtil.toStringArray(propValue, defaultArray);

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
    public static Event createEvent(final Bundle sourceBundle,
            final ServiceReference sourceService,
            final String topic,
            final Map<String, Object> props) {

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
     * Create a comparable object out of the service properties. With the result
     * it is possible to compare service properties based on the service ranking
     * of a service. Therefore this object acts like {@link ServiceReference#compareTo(Object)}.
     * @param props The service properties.
     * @return A comparable for the ranking of the service
     * @since 2.0.6
     */
    public static Comparable<Object> getComparableForServiceRanking(final Map<String, Object> props) {
        return ServiceUtil.getComparableForServiceRanking(props);
    }
}
