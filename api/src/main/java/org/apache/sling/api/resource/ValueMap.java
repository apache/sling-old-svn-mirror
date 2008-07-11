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

import java.util.Map;
import java.util.Collections;

import org.apache.sling.api.wrappers.ValueMapDecorator;

public interface ValueMap extends Map<String, Object> {

    /**
     * Empty value map
     */
    final ValueMap EMPTY = new ValueMapDecorator(Collections.<String, Object>emptyMap());
    
    // return named value converted to type T or
    // null if not existing
    <T> T get(String name, Class<T> type);

    // return named value converted to the type T of
    // the default value or the default value if the
    // named value does not exist
    <T> T get(String name, T defaultValue);
    
}
