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

package org.apache.sling.scripting.sightly;

import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * A key-value immutable object understood by the Sightly runtime
 * @param <T> the type of values for this record
 */
@ProviderType
public interface Record<T> {

    /**
     * Get the value of the specified property
     * @param name the name of the property
     * @return the value of the property or null if this record does not
     * have the specified property
     */
    T get(String name);

    /**
     * Get the set of properties for this record
     * @return this record's properties
     */
    Set<String> properties();

}
