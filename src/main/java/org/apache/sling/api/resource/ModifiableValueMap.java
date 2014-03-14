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

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>ModifiableValueMap</code> is an extension
 * of the {@link ValueMap} which allows to modify and
 * persist properties. All changes to this map are
 * stored in the transient layer of the resource resolver
 * or more precisely in the transient layer of the
 * resource provider managing this resource.
 * <p>
 * Once {@link ResourceResolver#commit()} is called, the
 * changes are finally persisted.
 * <p>
 * The modifiable value map is only changeable through
 * one of these methods
 * <ul>
 *  <li>{@link #put(String, Object)}</li>
 *  <li>{@link #putAll(java.util.Map)}</li>
 *  <li>{@link #remove(Object)}</li>
 * </ul>
 * <p>
 * The map is not modifiable through the collections provided
 * by
 * <ul>
 *  <li>{@link #entrySet()}</li>
 *  <li>{@link #keySet()}</li>
 *  <li>{@link #values()}</li>
 * </ul>
 * And it can't be modified by these methods:
 * <ul>
 *  <li>{@link #clear()}</li>
 * </ul>
 * <p>
 *
 * A modifiable value map should value {@link ResourceResolver#PROPERTY_RESOURCE_TYPE}
 * to set the resource type of a resource.
 *
 * A modifiable value map must not support deep writes. A call of a modification method
 * with a path should result in an IllegalArgumentException.
 *
 * @since 2.2
 */
@ConsumerType
public interface ModifiableValueMap extends ValueMap {

    // just a marker
}
