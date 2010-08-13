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

/**
 * The <code>ResourceResolverFactory</code> defines the service API to get and
 * create <code>ResourceResolver</code>s.
 * <p>
 * As soon as the resource resolver is not used anymore,
 * {@link ResourceResolver#close()} should be called.
 *
 * @since 2.1
 */
public interface ResourceResolverFactory {

    /**
     * Name of the authentication information property providing the name of the
     * user for which the {@link #getResourceResolver(Map)} and
     * {@link #getAdministrativeResourceResolver(Map)} create resource
     * resolvers. on whose behalf the request is being handled. This property
     * may be missing in which case an anonymous (unauthenticated) resource
     * resolver is returned if possible.
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
     * {@link #getResourceResolver(Map)} and
     * {@link #getAdministrativeResourceResolver(Map)} methods to try to
     * impersonate the created resource resolver to the requested user and
     * return the impersonated resource resolver.
     * <p>
     * If this impersonation fails the actual creation of the resource resolver
     * fails.
     * <p>
     * If this property is not set in the authentication info or is set to the
     * same name as the {@link #USER user.name} property this property is
     * ignored.
     * <p>
     * The type of this property, if present, is <code>String</code>.
     */
    String USER_IMPERSONATION = "user.impersonation";

    /**
     * Returns a new {@link ResourceResolver} instance with further
     * configuration taken from the given <code>authenticationInfo</code> map.
     * Generally this map will contain a user name and password to authenticate.
     * <p>
     * If the <code>authenticationInfo</code> map is <code>null</code> the
     * <code>ResourceResolver</code> returned will generally not be
     * authenticated and only provide minimal privileges, if any at all.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parametrize how the
     *            resource resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} according to the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurrs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     */
    ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo)
            throws LoginException;

    /**
     * Returns a new {@link ResourceResolver} instance with administrative
     * privileges with further configuration taken from the given
     * <code>authenticationInfo</code> map.
     * <p>
     * Note, that if the <code>authenticationInfo</code> map contains the
     * {@link #USER_IMPERSONATION} attribute the <code>ResourceResolver</code>
     * returned will only have administrative privileges if the user identified
     * by the property has administrative privileges.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parametrize how the
     *            resource resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} with administrative privileges unless
     *         the {@link #USER_IMPERSONATION} was set in the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurrs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     */
    ResourceResolver getAdministrativeResourceResolver(
            Map<String, Object> authenticationInfo) throws LoginException;
}
