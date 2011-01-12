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
package org.apache.sling.installer.api.tasks;

import java.util.Map;

/** A resource that's been registered in the OSGi controller.
 * 	Data can be either an InputStream or a Dictionary, and we store
 *  it locally to avoid holding up to classes or data from our
 *  clients, in case those disappear while we're installing stuff.
 */
public interface TaskResource extends RegisteredResource {

	/**
	 * Attributes include the bundle symbolic name, bundle version, etc.
	 */
	Map<String, Object> getAttributes();

    /**
     * Get the current state of the resource.
     */
    ResourceState getState();

    /**
     * Set the new state of teh resource.
     */
    void setState(final ResourceState s);

    /**
     * Get the value of a temporary attribute.
     * @param key The name of the attribute
     * @return The value of the attribute or <code>null</code>
     */
    Object getTemporaryAttribute(String key);

    /**
     * Set the value of a temporary attribute.
     * @param key The name of the attribute
     * @param value The attribute value or <code>null</code> to remove it.
     */
    void setTemporaryAttributee(String key, Object value);
}
