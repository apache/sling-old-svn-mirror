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

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ResourceResolverFactory</code> defines the service API to get and
 * create <code>ResourceResolver</code>s.
 * <p>
 * As soon as the resource resolver is not used anymore,
 * {@link ResourceResolver#close()} should be called.
 *
 * @since 2.1
 */
@ProviderType
public interface ResourceResolverFactory {

    /**
     * Name of the authentication information property providing the name of the
     * user for which the {@link #getResourceResolver(Map)} method creates
     * resource resolvers. This property may be missing in which case an
     * anonymous (unauthenticated) resource resolver is returned if possible.
     * <p>
     * The type of this property, if present, is <code>String</code>.
     */
    String USER = "user.name";

    /**
     * Name of the authentication information property providing the password of
     * the user for which to create a resource resolver. If this property is
     * missing an empty password is assumed.
     * <p>
     * The type of this property, if present, is <code>char[]</code>.
     */
    String PASSWORD = "user.password";

    /**
     * Name of the authentication information property causing the
     * {@link #getResourceResolver(Map)},
     * {@link #getAdministrativeResourceResolver(Map)}, and
     * {@link #getServiceResourceResolver(Map)} methods to try to impersonate
     * the created resource resolver to the requested user and return the
     * impersonated resource resolver.
     * <p>
     * If this impersonation fails the actual creation of the resource resolver
     * fails and a {@code LoginException} is thrown.
     * <p>
     * If this property is not set in the authentication info or is set to the
     * same name as the {@link #USER user.name} property this property is
     * ignored.
     * <p>
     * The type of this property, if present, is <code>String</code>.
     */
    String USER_IMPERSONATION = "user.impersonation";

    /**
     * Name of the authentication information property providing the Subservice
     * Name for the service requesting a resource resolver.
     * <p>
     * The type of this property, if present, is <code>String</code>.
     *
     * @see #getServiceResourceResolver(Map)
     * @since 2.4 (bundle version 2.5.0)
     */
    String SUBSERVICE = "sling.service.subservice";

    /**
     * Returns a new {@link ResourceResolver} instance with further
     * configuration taken from the given <code>authenticationInfo</code> map.
     * Generally this map will contain a user name and password to authenticate.
     * <p>
     * If the <code>authenticationInfo</code> map is <code>null</code> the
     * <code>ResourceResolver</code> returned will generally not be
     * authenticated and only provide minimal privileges, if any at all.
     * <p>
     * The {@link #USER_IMPERSONATION} property is obeyed but requires that the
     * actual user has permission to impersonate as the requested user. If such
     * permission is missing, a {@code LoginException} is thrown.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} according to the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     */
    ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo) throws LoginException;

    /**
     * Returns a new {@link ResourceResolver} instance with administrative
     * privileges with further configuration taken from the given
     * <code>authenticationInfo</code> map.
     * <p>
     * Note, that if the <code>authenticationInfo</code> map contains the
     * {@link #USER_IMPERSONATION} attribute the <code>ResourceResolver</code>
     * returned will only have administrative privileges if the user identified
     * by the property has administrative privileges.
     * <p>
     * <b><i>NOTE: This method is intended for use by infrastructure bundles to
     * access the repository and provide general services. This method MUST not
     * be used to handle client requests of whatever kinds. To handle client
     * requests a regular authenticated resource resolver retrieved through
     * {@link #getResourceResolver(Map)} must be used.</i></b>
     * <p>
     * This method is deprecated. Services running in the Sling system should
     * use the {@link #getServiceResourceResolver(Map)} method instead.
     * Implementations of this method should throw {@code LoginException} if
     * they don't support it.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} with administrative privileges unless
     *         the {@link #USER_IMPERSONATION} was set in the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     * @deprecated as of 2.4 (bundle version 2.5.0) because of inherent security
     *             issues. Services requiring specific permissions should use
     *             the {@link #getServiceResourceResolver(Map)} instead.
     */
    @Deprecated
    ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authenticationInfo) throws LoginException;

    /**
     * Returns a new {@link ResourceResolver} instance with privileges assigned
     * to the service provided by the calling bundle.
     * <p>
     * The provided {@code authenticationInfo} map may be used to provide
     * additional information such as the {@value #SUBSERVICE}.
     * {@link #USER} and {@link #PASSWORD} properties provided in the map are
     * ignored.
     * <p>
     * The {@link #USER_IMPERSONATION} property is obeyed but requires that the
     * actual service user has permission to impersonate as the requested user.
     * If such permission is missing, a {@code LoginException} is thrown.
     *
     * @param authenticationInfo A map of further service information which may
     *            be used by the implementation to parametrize how the resource
     *            resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} with appropriate permissions to
     *         execute the service.
     * @throws LoginException If an error occurrs creating the new
     *             <code>ResourceResolver</code> for the service represented by
     *             the calling bundle.
     * @since 2.4 (bundle version 2.5.0) to replace
     *        {@link #getAdministrativeResourceResolver(Map)}
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/service-authentication.html">Service
     *      Authentication</a>
     */
    ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo) throws LoginException;
}
