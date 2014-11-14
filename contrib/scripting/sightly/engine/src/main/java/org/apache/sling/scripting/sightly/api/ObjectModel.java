/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.api;

import java.util.Collection;
import java.util.Map;

/**
 * Defines the dynamic queries that values must support
 * in Sightly
 */
public interface ObjectModel {

    String PROPERTY_ACCESS = "resolveProperty";
    String COLLECTION_COERCE = "coerceToCollection";
    String NUMERIC_COERCE = "coerceNumeric";
    String STRING_COERCE = "coerceToString";
    String BOOLEAN_COERCE = "coerceToBoolean";

    String STRICT_EQ = "strictEq";
    String LEQ = "leq";
    String LT = "lt";

    /**
     * Retrieve the specified property from the given object
     * @param target - the target object
     * @param property - the property name
     * @return - the value of the property or null if the object has no such property
     */
    Object resolveProperty(Object target, Object property);

    /**
     * Convert the given object to a string.
     * @param target - the target object
     * @return - the string representation of the object
     */
    String coerceToString(Object target);

    /**
     * Convert the given object to a boolean value
     * @param object - the target object
     * @return - the boolean representation of that object
     */
    boolean coerceToBoolean(Object object);

    /**
     * Coerce the object to a numeric value
     * @param object - the target object
     * @return - the numeric representation
     */
    Number coerceNumeric(Object object);

    /**
     * Force the conversion of the object to a collection
     * @param object - the target object
     * @return the collection representation of the object
     */
    Collection<Object> coerceToCollection(Object object);

    /**
     * Force the conversion of the target object to a map
     *
     * @param object - the target object
     * @return - a map representation of the object. Default is an empty map
     */
    Map coerceToMap(Object object);

    /**
     * Check whether the left argument equals the right one
     * @param left the left argument
     * @param right the right argument
     * @return true if arguments are equal
     */
    boolean strictEq(Object left, Object right);

    /**
     * Check if left < right
     * @param left the left argument
     * @param right the right argument
     * @return true if left < right
     */
    boolean lt(Object left, Object right);

    /**
     * Check if left < right
     * @param left the left argument
     * @param right the right argument
     * @return true if left < right
     */
    boolean leq(Object left, Object right);
}
