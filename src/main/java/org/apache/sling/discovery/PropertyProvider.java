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
package org.apache.sling.discovery;

import aQute.bnd.annotation.ConsumerType;


/**
 * The <code>PropertyProvider</code> service interface may be implemented by
 * components that wish to define properties on the local instance which then
 * are broadcast to the <code>TopologyView</code> instances.
 * <p>
 * The provided properties are registered with the {@link #PROPERTY_PROPERTIES}
 * service property.
 * If the set of provided properties changes or one of the provided values
 * change, the service registration of the provider should be updated.
 * This avoids periodic polling for changes.
 */
@ConsumerType
public interface PropertyProvider {

    /**
     * The name of the service registration property containing the names
     * of the properties provided by this provider.
     * The value is either a string or an array of strings.
     * A property name must only contain alphanumeric characters plus <code>.</code>,
     * <code>_</code>, <code>-</code>.
     */
    String PROPERTY_PROPERTIES = "instance.properties";

	/**
	 * Retrieves a property that is subsequently set on the local instance
	 * and broadcast to the <code>TopologyView</code> instances.
	 * <p>
	 * These properties are non-persistent and disappear after the local instance goes down.
	 *
	 * @return The value of the property or <code>null</code>. If the property
	 *         value can't be provided or if the provider does not support this
	 *         property, it must return <code>null</code>.
	 */
	String getProperty(final String name);
}
