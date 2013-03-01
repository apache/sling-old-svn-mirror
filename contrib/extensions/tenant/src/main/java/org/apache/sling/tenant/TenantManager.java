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

import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>TenantManager</code> service interface defines the API that
 * administrative tools will use to created, update or remove Tenants.
 * <p>
 * The implementation will make use of
 * {@link org.apache.sling.tenant.spi.TenantCustomizer} services to customize
 * management of tenants.
 * <p>
 * Tenant properties can be created, modified, and removed with the
 * {@link #setProperty(Tenant, String, Object)},
 * {@link #setProperties(Tenant, Map)} and
 * {@link #removeProperties(Tenant, String...)} methods. Please note that every
 * call to any of these methods causes the
 * {@link org.apache.sling.tenant.spi.TenantCustomizer} services to be called.
 * To limit these calls for multiple changes the
 * {@link #setProperties(Tenant, Map)} method should be called.
 */
@ProviderType
public interface TenantManager {

    /**
     * Creates a new tenant with the given tenant ID and the initial set of
     * properties.
     * <p>
     * After creating the tenant, the
     * {@link org.apache.sling.tenant.spi.TenantCustomizer#setup(Tenant, org.apache.sling.api.resource.ResourceResolver)}
     * method is called to allow customizers to configure additional properties.
     * <p>
     * Before returning the newly created tenant object the data is persisted.
     *
     * @param tenantId The name of the new tenant. This name must not be
     *            {@code null} or an empty string. It must also not be the same
     *            name as that of an existing tenant.
     * @param properties An optional map of initial properties. This may be
     *            {@code null} to not preset any properties. It is recommended,
     *            though, that this map contain at least the
     *            {@link Tenant#PROP_NAME} and {@link Tenant#PROP_DESCRIPTION}
     *            properties.
     * @return The newly created {@link Tenant} instance.
     * @throws NullPointerException if {@code tenantId} is {@code null}.
     * @throws IllegalArgumentException if a tenant with the same
     *             {@code tentantId} already exists.
     */
    Tenant create(String tenantId, Map<String, Object> properties);

    /**
     * Sets a single property of the tenant to a new value or removes the
     * property if the value is {@code null}.
     * <p>
     * Before returning the
     * {@link org.apache.sling.tenant.spi.TenantCustomizer#setup(Tenant, org.apache.sling.api.resource.ResourceResolver)}
     * method is called to allow customizers to configure additional properties.
     *
     * @param tenant The tenant whose property is to be set or remove.
     * @param name The name of the property to set or remove.
     * @param value The new value of the property. If this value is {@code null}
     *            the property is actually removed.
     * @throws NullPointerException if {@code tenant} or {@code name} is
     *             {@code null}.
     */
    void setProperty(Tenant tenant, String name, Object value);

    /**
     * Sets or removes multiple properties on the tenant.
     * <p>
     * Before returning the
     * {@link org.apache.sling.tenant.spi.TenantCustomizer#setup(Tenant, org.apache.sling.api.resource.ResourceResolver)}
     * method is called to allow customizers to configure additional properties.
     *
     * @param tenant The tenant whose properties are to be modified.
     * @param properties The map of properties to set or remove. A property
     *            whose value is {@code null} is removed from the tenant.
     * @throws NullPointerException if {@code tenant} or {@code properties} is
     *             {@code null}.
     */
    void setProperties(Tenant tenant, Map<String, Object> properties);

    /**
     * Removes one or more properties from the tenant.
     * <p>
     * Before returning the
     * {@link org.apache.sling.tenant.spi.TenantCustomizer#setup(Tenant, org.apache.sling.api.resource.ResourceResolver)}
     * method is called to allow customizers to configure additional properties
     * unless the {@code properties} parameter is {@code null} or empty.
     *
     * @param tenant The tenant from which to remove properties.
     * @param properties The list of properties to be removed. If this is
     *            {@code null} or empty, nothing happens and the
     *            {@link org.apache.sling.tenant.spi.TenantCustomizer} is not
     *            called.
     * @throws NullPointerException if {@code tenant} is {@code null}.
     */
    void removeProperties(Tenant tenant, String... propertyNames);

    /**
     * Removes the given tenant.
     * <p>
     * Before returning the
     * {@link org.apache.sling.tenant.spi.TenantCustomizer#remove(Tenant, org.apache.sling.api.resource.ResourceResolver)}
     * method is called to allow customizers to implement further cleanup upon
     * tenant removal.
     *
     * @param tenant The tenant to remove.
     * @throws NullPointerException if {@code tenant} is {@code null}
     */
    void remove(Tenant tenant);
}
