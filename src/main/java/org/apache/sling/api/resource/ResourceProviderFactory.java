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
package org.apache.sling.api.resource;

import java.util.Map;

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>ResourceProviderFactory</code> defines the service API to get and
 * create <code>ResourceProviders</code>s dynamically on a per usage base.
 * Instead of sharing a resource provider between resource resolvers, the
 * factory allows to create an own instance of a resource provider per resource
 * resolver. The factory also supports authentication.
 * <p>
 * If the resource provider is not used anymore and implements the
 * {@link DynamicResourceProvider} interface, the close method should be called.
 *
 * @since 2.2.0
 */
@ConsumerType
public interface ResourceProviderFactory {

    /**
     * A required resource provider factory is accessed directly when a new
     * resource resolver is created. Only if authentication against all required
     * resource provider factories is successful, a resource resolver is created
     * by the resource resolver factory. Boolean service property, default value
     * is <code>false</true>
     */
    String PROPERTY_REQUIRED = "required";

    /**
     * The authentication information property referring to the bundle
     * providing a service for which a resource provider is to be retrieved. If
     * this property is provided, the
     * {@link ResourceResolverFactory#SUBSERVICE} property may also be
     * present.
     * <p>
     * {@link ResourceResolverFactory} implementations must provide this
     * property if their implementation of the
     * {@link ResourceResolverFactory#getServiceResourceResolver(Map)} method
     * use a resource provider factory.
     * <p>
     * The type of this property, if present, is
     * <code>org.osgi.framework.Bundle</code>.
     *
     * @since 2.4 (bundle version 2.5.0)
     */
    String SERVICE_BUNDLE = "sling.service.bundle";

    /**
     * Returns a new {@link ResourceProvider} instance with further
     * configuration taken from the given <code>authenticationInfo</code> map.
     * Generally this map will contain a user name and password to authenticate.
     * <p>
     * The <code>authenticationInfo</code> map will in general contain the same
     * information as provided to the respective {@link ResourceResolver}
     * method. For
     * <p>
     * If the <code>authenticationInfo</code> map is <code>null</code> the
     * <code>ResourceProvider</code> returned will generally not be
     * authenticated and only provide minimal privileges, if any at all.
     * <p>
     * Implementations must ignore the {@value ResourceResolverFactory#USER}
     * property the {@link #SERVICE_BUNDLE} property is provided to implement
     * service authentication.
     * <p>
     * The {@value ResourceResolverFactory#USER_IMPERSONATION} property is
     * obeyed but requires that the actual user has permission to impersonate as
     * the requested user. If such permission is missing, a
     * {@code LoginException} is thrown.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource provider is created. This may be <code>null</code>.
     * @return A {@link ResourceProvider} according to the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurs creating the new
     *             <code>ResourceProvider</code> with the provided credential
     *             data.
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/service-authentication.html">Service
     *      Authentication</a>
     */
    ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException;

    /**
     * Returns a new {@link ResourceProvider} instance with administrative
     * privileges with further configuration taken from the given
     * <code>authenticationInfo</code> map.
     * <p>
     * Note, that if the <code>authenticationInfo</code> map contains the
     * {@link ResourceResolverFactory#USER_IMPERSONATION} attribute the
     * <code>ResourceProvider</code> returned will only have administrative
     * privileges if the user identified by the property has administrative
     * privileges.
     * <p>
     * Implementations of this method should throw {@code LoginException} if
     * they don't support it.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource provider is created. This may be <code>null</code>.
     * @return A {@link ResourceProvider} with administrative privileges unless
     *         the {@link ResourceResolverFactory#USER_IMPERSONATION} was set in
     *         the <code>authenticationInfo</code>.
     * @throws LoginException If an error occurs creating the new
     *             <code>ResourceResolverFactory</code> with the provided
     *             credential data.
     * @deprecated as of 2.4 (bundle version 2.5.0) because of inherent security
     *             issues. Implementations may implement this method at their
     *             discretion but must support the new service based resource
     *             provider generation in the {@link #getResourceProvider(Map)}
     *             method honoring the {@link #SERVICE_BUNDLE} and
     *             {@link ResourceResolverFactory#SUBSERVICE} properties.
     */
    @Deprecated
    ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException;
}
