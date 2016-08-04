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
package org.apache.sling.tenant.spi;

import java.util.Map;

import org.apache.sling.tenant.Tenant;

import aQute.bnd.annotation.ConsumerType;

/**
 * This is a service interface to customize tenant setup and administration.
 *
 * Tools can hook into the tenant creation, changing a tenant and removing
 * thereof by implementing this interface.
 *
 * @since 1.1
 */
@ConsumerType
public interface TenantManagerHook {

    /**
     * Method called to create the given tenant. The method may return
     * additional properties to be added to the Tenant's property list.
     * <p>
     * This method is not expected to throw an exception. Any exception thrown
     * is logged but otherwise ignored.
     *
     * @param tenant The {@link Tenant} to be configured by this call
     * @return Additional properties to be added to the tenant. These properties
     *         may later be accessed through the {@linkplain Tenant tenant's}
     *         property accessor methods. {@code null} or an empty map may be
     *         returned to not add properties.
     */
    Map<String, Object> setup(Tenant tenant);

    /**
     * Method called to update the given tenant. The method may return
     * additional properties to be added to the Tenant's property list.
     * <p>
     * This method is not expected to throw an exception. Any exception thrown
     * is logged but otherwise ignored.
     *
     * @param tenant The {@link Tenant} to be configured by this call
     * @return Additional properties to be added to the tenant. These properties
     *         may later be accessed through the {@linkplain Tenant tenant's}
     *         property accessor methods. {@code null} or an empty map may be
     *         returned to not add properties.
     */
    Map<String, Object> change(Tenant tenant);

    /**
     * Called to remove the setup for the given Tenant. This reverts all changes
     * done by the #setup method.
     * <p>
     * This method is not expected to throw an exception. Any exception thrown
     * is logged but otherwise ignored.
     *
     * @param tenant The {@link Tenant} about to be removed
     */
    void remove(Tenant tenant);
}
