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
package org.apache.sling.tenant;

import java.util.Iterator;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>Tenant</code> interface represents a tenant which may be used to
 * further customize request and other processing.
 * <p>
 * This interface is intended to be implemented by the implementor of the
 * {@link TenantProvider} interface to be returned by the tenant accessor
 * methods.
 */
@ProviderType
public interface Tenant {

	/**
	 * The name of the {@link #getProperty(String) property} whose string
	 * representation is used as this tenant's {@link #getName() name} (value is
	 * "sling.tenant.name").
	 *
	 * @see #getName()
	 * @see #getProperty(String)
	 */
	String PROP_NAME = "tenant.name";

	/**
	 * The name of the {@link #getProperty(String) property} whose string
	 * representation is used as this tenant's {@link #getDescription()
	 * description} (value is "sling.tenant.description").
	 *
	 * @see #getDescription()
	 * @see #getProperty(String)
	 */
	String PROP_DESCRIPTION = "tenant.description";

	/**
	 * Returns the unique identifier of this tenant.
	 * <p>
	 * The tenant identifier has not predefined mapping to a property and may be
	 * generated automatically by the TenantProvider.
	 */
	String getId();

	    /**
     * Returns the name of the tenant. This is a short name for quickly
     * identifying this tenant. This name is not required to be globally unique.
     * <p>
     * The name of the tenant is the string representation of the
     * {@link #PROP_NAME} property or {@code null} if the property is not
     * defined.
     */
    String getName();

    /**
     * Returns a human readable description of this tenant.
     * <p>
     * The description of the tenant is the string representation of the
     * {@link #PROP_DESCRIPTION} property or {@code null} if the property is not
     * defined.
     */
	String getDescription();

	/**
	 * Returns the named property or <code>null</code> if no such property
	 * exists or if the property value itself is <code>null</code>.
	 */
	Object getProperty(String name);

	/**
	 * Returns the named property converted to the requested type or
	 * <code>null</code> if the property does not exist, the property value
	 * itself is <code>null</code> or cannot be converted to the requested type.
	 */
	<Type> Type getProperty(String name, Type type);

	/**
	 * Returns an iterator or String values representing the names of defined
	 * properties of this tenant.
	 */
	Iterator<String> getPropertyNames();
}
