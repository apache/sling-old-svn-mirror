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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.tenant.Tenant;

import aQute.bnd.annotation.ConsumerType;

/**
 * This is a service interface to customize tenant setup and administration.
 *
 * Tools can hook into the tenant creation, changing a tenant and removing
 * thereof by implementing this interface.
 */
@ConsumerType
public interface TenantCustomizer {

    /**
     * Method called to create or update the given tenant. The method may return
     * additional properties to be added to the Tenant's property list. The
     * ResourceResolver allows for access to the persistence.
     * <p>
     * The {@code ResourceResolver.commit} method must not be called by this
     * method.
     * <p>
     * This method is not expected to throw an exception. Any exception thrown
     * is logged but otherwise ignored.
     *
     * @param tenant The {@link Tenant} to be configured by this call
     * @param resolver The {@code ResourceResolver} providing access to the
     *            persistence for further setup. Note, that this
     *            {@code resolver} will have administrative privileges.
     * @return Additional properties to be added to the tenant. These properties
     *         may later be accessed through the {@linkplain Tenant tenant's}
     *         property accessor methods. {@code null} or an empty map may be
     *         returned to not add properties.
     */
    public Map<String, Object> setup(Tenant tenant, ResourceResolver resolver);

    /**
     * Called to remove the setup for the given Tenant. This reverts all changes
     * done by the #setup method. The ResourceResolver allows for access to the
     * persistence.
     * <p>
     * The {@code ResourceResolver.commit} method must not be called by this
     * method.
     * <p>
     * This method is not expected to throw an exception. Any exception thrown
     * is logged but otherwise ignored.
     *
     * @param tenant The {@link Tenant} about to be removed
     * @param resolver The {@code ResourceResolver} providing access to the
     *            persistence for further cleanup. Note, that this
     *            {@code resolver} will have administrative privileges.
     */
    public void remove(Tenant tenant, ResourceResolver resolver);
}
