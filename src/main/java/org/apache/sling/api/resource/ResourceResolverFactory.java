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
     * If this property is set in the authentication information which is passed
     * into {@link #getResourceResolver(Map)} or
     * {@link #getAdministrativeResourceResolver(Map)} then after a successful
     * authentication attempt, a sudo to the provided user id is tried.
     */
    String SUDO_USER_ID = "sudo.user.id";

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
     * {@link #SUDO_USER_ID} attribute the <code>ResourceResolver</code>
     * returned will only have administrative privileges if the user identified
     * by the property has administrative privileges.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parametrize how the
     *            resource resolver is created. This may be <code>null</code>.
     * @return A {@link ResourceResolver} with administrative privileges unless
     *         the {@link #SUDO_USER_ID} was set in the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurrs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     */
    ResourceResolver getAdministrativeResourceResolver(
            Map<String, Object> authenticationInfo) throws LoginException;
}
