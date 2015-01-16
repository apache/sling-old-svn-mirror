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
package org.apache.sling.scripting.sightly.render;

import java.util.Collection;
import java.util.Map;

import javax.script.Bindings;

import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code RenderContext} defines the context for executing Sightly scripts (see {@link RenderUnit}).
 */
@ProviderType
public interface RenderContext {

    /**
     * Returns the map of script bindings available to Sightly scripts.
     *
     * @return the global bindings for a script
     */
    Bindings getBindings();

    /**
     * Call one of the registered {@link RuntimeExtension}s.
     *
     * @param functionName the name under which the extension is registered
     * @param arguments    the extension's arguments
     * @return the {@link RuntimeExtension}'s result
     */
    Object call(String functionName, Object... arguments);

    /**
     * Retrieve the specified property from the given object
     *
     * @param target   - the target object
     * @param property - the property name
     * @return - the value of the property or null if the object has no such property
     */
    Object resolveProperty(Object target, Object property);

    /**
     * Convert the given object to a string.
     *
     * @param target - the target object
     * @return - the string representation of the object
     */
    String toString(Object target);

    /**
     * Convert the given object to a boolean value
     *
     * @param object - the target object
     * @return - the boolean representation of that object
     */
    boolean toBoolean(Object object);

    /**
     * Coerce the object to a numeric value
     *
     * @param object - the target object
     * @return - the numeric representation
     */
    Number toNumber(Object object);

    boolean isCollection(Object obj);

    /**
     * Force the conversion of the object to a collection
     *
     * @param object - the target object
     * @return the collection representation of the object
     */
    Collection<Object> toCollection(Object object);

    /**
     * Force the conversion of the target object to a map
     *
     * @param object - the target object
     * @return - a map representation of the object. Default is an empty map
     */
    Map toMap(Object object);

}
