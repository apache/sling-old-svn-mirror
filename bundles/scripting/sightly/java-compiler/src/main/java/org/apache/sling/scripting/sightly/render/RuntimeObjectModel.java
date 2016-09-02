/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.render;

import java.util.Collection;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@code RuntimeObjectModel} provides various utility object inspection &amp; conversion methods that can be applied to runtime
 * objects when executing HTL scripts.
 */
@ProviderType
public interface RuntimeObjectModel {

    /**
     * Checks if the provided object represents a primitive data type or not.
     *
     * @param obj the target object
     * @return {@code true} if the {@code target} is a primitive, {@code false} otherwise
     */
    boolean isPrimitive(Object obj);

    /**
     * Checks if an object is a {@link Collection} or is backed by one.
     *
     * @param target the target object
     * @return {@code true} if the {@code target} is a collection or is backed by one, {@code false} otherwise
     */
    boolean isCollection(Object target);

    /**
     * Resolve a property of a target object and return its value. The property can
     * be either an index or a name
     *
     * @param target   the target object
     * @param property the property to be resolved
     * @return the value of the property
     */
    Object resolveProperty(Object target, Object property);

    /**
     * Convert the given object to a boolean value
     *
     * @param object the target object
     * @return the boolean representation of that object
     */
    boolean toBoolean(Object object);

    /**
     * Coerce the object to a numeric value
     *
     * @param object the target object
     * @return the numeric representation
     */
    Number toNumber(Object object);

    /**
     * Convert the given object to a string.
     *
     * @param target the target object
     * @return the string representation of the object
     */
    String toString(Object target);

    /**
     * Force the conversion of the object to a collection
     *
     * @param object the target object
     * @return the collection representation of the object
     */
    Collection<Object> toCollection(Object object);

    /**
     * Force the conversion of the target object to a map
     *
     * @param object the target object
     * @return a map representation of the object. Default is an empty map
     */
    Map toMap(Object object);
}
