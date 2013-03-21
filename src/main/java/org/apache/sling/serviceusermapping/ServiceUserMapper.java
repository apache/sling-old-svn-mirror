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
package org.apache.sling.serviceusermapping;

import org.osgi.framework.Bundle;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ServiceUserMapper</code> service can be used to map a service
 * provided by a bundle to the name of a user account used to access the
 * ResourceResolver used by the service to access its data.
 * <p>
 * The goal of this service is to allow services to be implemented accessing the
 * storage with service-specific accounts which are tailored to allow the
 * service appropriate access without requiring administrative level access to
 * the storage.
 * <p>
 * In addition to allowing to phase out the use of
 * {@code ResourceResolver.getAdministrativeResourceResolver} and
 * {@code SlingRepository.loginAdministrative} it also allows to better account
 * for changes to the storage by the different services.
 * <p>
 * This service is not intended to be used by the general user but by
 * implementations of the {@code ResourceResolverFactory} and
 * {@code SlingRepository} services.
 * <p>
 * This service is not intended to be implemented by clients.
 */
@ProviderType
public interface ServiceUserMapper {

    /**
     * The name of the Bundle manifest header providing the name of the service
     * provided by the bundle. If this header is missing or empty, the bundle's
     * symbolic name is used instead to name the service.
     */
    String BUNDLE_HEADER_SERVICE_NAME = "Sling-Service";

    /**
     * Returns the name of the service represented by the {@code bundle} and the
     * {@code serviceInfo}.
     *
     * @param bundle The bundle implementing the service request access to
     *            resources.
     * @param serviceInfo Additional information about the concrete service
     *            requesting access. This parameter is optional and may be
     *            {@code null}.
     * @return The name of the service represented by the bundle along with the
     *         additional service information.
     */
    String getServiceName(Bundle bundle, String serviceInfo);

    /**
     * Returns the name of a user to the be used to access the Sling Resource
     * tree or the JCR Repository.
     *
     * @param bundle The bundle implementing the service request access to
     *            resources.
     * @param serviceInfo Additional information about the concrete service
     *            requesting access. This parameter is optional and may be
     *            {@code null}.
     * @return The name of the user to use to provide access to the resources
     *         for the service. This may be {@code null} to only grant guest
     *         level (or anonymous level) access to the resources.
     */
    String getUserForService(Bundle bundle, String serviceInfo);
}
