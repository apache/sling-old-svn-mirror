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
 * provided by a bundle to the ID of a user account used to access the
 * ResourceResolver used by the service to access its data.
 * <p>
 * The goal of this service is to allow services to be implemented accessing the
 * storage with service-specific accounts which are tailored to allow the
 * service appropriate access without requiring administrative level access to
 * the storage.
 * <p>
 * In general a service is implement in a single bundle such as the JSP compiler
 * bundle. Other services may be implemented in multiple bundles. In certain
 * cases there may be sub-services requiring different access levels. For
 * example a couple of bundles may implement a "mail" service where each bundle
 * implements a part of the service such as the "smtp", "queuing", and
 * "delivery" sub services. Such sub services are identified with the
 * {@code subServiceName} parameter on the method calls.
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
 *
 * @see <a href=
 *      "http://sling.apache.org/documentation/the-sling-engine/service-authentication.html"
 *      >Service Authentication</a>
 */
@ProviderType
public interface ServiceUserMapper {

    /**
     * Returns the ID of a user to access the data store on behalf of the
     * service.
     *
     * @param bundle The bundle implementing the service request access to
     *            resources.
     * @param subServiceName Name of the sub service. This parameter is optional and
     *            may be an empty string or {@code null}.
     * @return The ID of the user to use to provide access to the resources for
     *         the service. This may be {@code null} if no particular user can
     *         be derived for the service identified by the bundle and the
     *         optional {@code serviceInfo}.
     */
    String getServiceUserID(Bundle bundle, String subServiceName);
}
