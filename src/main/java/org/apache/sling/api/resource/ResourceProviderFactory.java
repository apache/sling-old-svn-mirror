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
 * The <code>ResourceProviderFactory</code> defines the service API to get and
 * create <code>ResourceProviders</code>s.
 * <p>
 * If the resource provider is not used anymore and implements the {@link DynamicResourceProvider}
 * interface, the close method should be called.
 *
 * @since 2.2.0
 */
public interface ResourceProviderFactory {

    /**
     * A required resource provider factory is accessed directly when a new resource resolver
     * is created. Only if authentication against all required resource provider factories
     * is successful, a resource resolver is created by the resource resolver factory.
     * Boolean service property, default vaule is <code>false</true>
     */
    String PROPERTY_REQUIRED = "required";

    /**
     * Returns a new {@link ResourceProvider} instance with further
     * configuration taken from the given <code>authenticationInfo</code> map.
     * Generally this map will contain a user name and password to authenticate.
     * <p>
     * If the <code>authenticationInfo</code> map is <code>null</code> the
     * <code>ResourceProvider</code> returned will generally not be authenticated and only provide
     * minimal privileges, if any at all.
     *
     * @param authenticationInfo
     *            A map of further credential information which may be used by
     *            the implementation to parametrize how the resource provider is
     *            created. This may be <code>null</code>.
     * @return A {@link ResourceProvider} according to the <code>authenticationInfo</code>.
     * @throws LoginException
     *             If an error occurrs creating the new <code>ResourceProvider</code> with the
     *             provided credential data.
     */
    ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException;

    /**
     * Returns a new {@link ResourceProvider} instance with administrative
     * privileges with further configuration taken from the given <code>authenticationInfo</code>
     * map.
     * <p>
     * Note, that if the <code>authenticationInfo</code> map contains the
     * {@link ResourceResolverFactory#USER_IMPERSONATION} attribute the <code>ResourceProvider</code> returned will only
     * have administrative privileges if the user identified by the property has administrative
     * privileges.
     * <p>
     * <b><i>NOTE: This method is intended for use by infrastructure bundles to access the
     * resource tree and provide general services. This method MUST not be used to handle client
     * requests of whatever kinds. To handle client requests a regular authenticated resource
     * provider retrieved through {@link #getResourceProvider(Map)} must be used.</i></b>
     *
     * @param authenticationInfo
     *            A map of further credential information which may be used by
     *            the implementation to parametrize how the resource provider is
     *            created. This may be <code>null</code>.
     * @return A {@link ResourceProvider} with administrative privileges unless
     *         the {@link ResourceResolverFactory#USER_IMPERSONATION} was set in the <code>authenticationInfo</code>.
     * @throws LoginException
     *             If an error occurrs creating the new <code>ResourceResolverFactory</code> with the
     *             provided credential data.
     */
    ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException;
}
