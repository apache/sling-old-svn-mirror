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

import org.osgi.framework.InvalidSyntaxException;

/**
 * The <code>TenantProvider</code> defines the service interface of for a sevice
 * which may be asked for {@link Tenant tenant instances}.
 * <p>
 * For now this provider interface provides access to a tenant applying to a
 * particular request as well as to all tenants known to this provider.
 */
public interface TenantProvider {

    /**
     * Returns the {@link Tenant} with the given <code>tenantId</code> or
     * <code>null</code> if no such tenant exists.
     */
    Tenant getTenant(String tenantId);

    /**
     * Returns an iterator of all {@link Tenant tenants} known to this provider.
     * If no tenants are known the iterator is empty.
     */
    Iterator<Tenant> getTenants();

    /**
     * Returns an iterator of {@link Tenant tenants} matching the given
     * <code>tenantFilter</code>.
     * <p>
     * The <code>tenantFilter</code> is a valid OSGi filter string as defined in
     * Section 3.2.6, Filter Syntax, of the OSGi Core Specification, Release 4.
     * <p>
     * If no tenants match the <code>tenantFilter</code> or the
     * <code>tenantFilter</code> is not a valid filter string the iterator is
     * empty.
     *
     * @throws InvalidSyntaxException if filter syntax is invalid
     */
    Iterator<Tenant> getTenants(String tenantFilter) throws InvalidSyntaxException;
}
