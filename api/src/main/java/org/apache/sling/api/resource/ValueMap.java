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
package org.apache.sling.api.resource;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * The <code>ValueMap</code> is an easy way to access
 * properties of a resource. With most resources you can
 * use {@link Resource#adaptTo(Class)} to adapt the resource
 * to a value map. The various getter methods can be used
 * to get the properties of the resource.
 */
public interface ValueMap extends Map<String, Object> {

    /**
     * Empty value map
     */
    final ValueMap EMPTY = new ValueMapDecorator(Collections.<String, Object>emptyMap());

    /**
     * Get a named property and convert it into the given type.
     * @param name The name of the property
     * @param type The class of the type
     * @return Return named value converted to type T or <code>null</code>
     *         if non existing or can't be converted.
     */
    <T> T get(String name, Class<T> type);

    /**
     * Get a named property and convert it into the given type.
     * @param name The name of the property
     * @param type The class of the type
     * @return Return named value converted to type T or the
     *         default value if non existing or can't be converted.
     */
    <T> T get(String name, T defaultValue);
}
