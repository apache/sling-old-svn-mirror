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
 * The <code>TenantProvider</code> defines the service interface
 * which may be asked for {@link Tenant tenant instances}.
 */
@ProviderType
public interface TenantProvider {

    /**
     * Returns the {@link Tenant} with the given <code>tenantId</code> or
     * <code>null</code> if no such tenant exists.
     */
    Tenant getTenant(String tenantId);

    /**
     * Returns an iterator of all {@link Tenant tenants} known to this provider.
     * If no tenants are known the iterator is empty.
     * <p>
     * This method is equivalent to calling {@link #getTenants(String)} with
     * {@code null} or an empty string.
     */
    Iterator<Tenant> getTenants();

    /**
     * Returns an iterator of {@link Tenant tenants} matching the given
     * <code>tenantFilter</code>.
     * <p>
     * The <code>tenantFilter</code> is a valid OSGi filter string as defined in
     * Section 3.2.6, Filter Syntax, of the OSGi Core Specification, Release 4
     * or {@code null} to return all tenants.
     * <p>
     * Calling this method with an empty string or {@code null} is equivalent to
     * calling the {@link #getTenants()} method and returns all tenants.
     * <p>
     * If no tenants match the <code>tenantFilter</code> the iterator is empty.
     * {@code null} is never returned.
     *
     * @throws IllegalArgumentException if filter syntax is invalid. A more
     *             detailed exception may be wrapped by the exception.
     */
    Iterator<Tenant> getTenants(String tenantFilter);
}
