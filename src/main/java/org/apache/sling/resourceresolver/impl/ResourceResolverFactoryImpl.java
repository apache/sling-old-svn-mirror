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
package org.apache.sling.resourceresolver.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;

/**
 * The <code>ResourceResolverFactoryImpl</code> is the {@link ResourceResolverFactory} service
 * providing the following
 * functionality:
 * <ul>
 * <li><code>ResourceResolverFactory</code> service
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 */
public class ResourceResolverFactoryImpl implements ResourceResolverFactory {

    private final CommonResourceResolverFactoryImpl commonFactory;

    private final ServiceUserMapper serviceUserMapper;

    private final Bundle usingBundle;

    public ResourceResolverFactoryImpl(
            final CommonResourceResolverFactoryImpl commonFactory,
            final Bundle usingBundle,
            final ServiceUserMapper serviceUserMapper) {
        this.commonFactory = commonFactory;
        this.serviceUserMapper = serviceUserMapper;
        this.usingBundle = usingBundle;
    }

    // ---------- Resource Resolver Factory ------------------------------------

    public ResourceResolver getServiceResourceResolver(final Map<String, Object> passedAuthenticationInfo) throws LoginException {
        // create a copy of the passed authentication info as we modify the map
        final Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        final String subServiceName;
        if ( passedAuthenticationInfo != null ) {
            authenticationInfo.putAll(passedAuthenticationInfo);
            authenticationInfo.remove(PASSWORD);
            final Object info = passedAuthenticationInfo.get(SUBSERVICE);
            subServiceName = (info instanceof String) ? (String) info : null;
        } else {
            subServiceName = null;
        }

        // Ensure a mapped user name: If no user is defined for a bundle
        // acting as a service, the user may be null. We can decide whether
        // this should yield guest access or no access at all. For now
        // no access is granted if there is no service user defined for
        // the bundle.
        final String userName = this.serviceUserMapper.getServiceUserID(this.usingBundle, subServiceName);
        if (userName == null) {
            throw new LoginException("Cannot derive user name for bundle "
                + this.usingBundle + " and sub service " + subServiceName);
        }

        // ensure proper user name and service bundle
        authenticationInfo.put(ResourceResolverFactory.USER, userName);
        authenticationInfo.put(ResourceProviderFactory.SERVICE_BUNDLE, this.usingBundle);

        return commonFactory.getResourceResolverInternal(authenticationInfo, false);
    }

    public ResourceResolver getResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return commonFactory.getResourceResolver(authenticationInfo);
    }

    public ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return commonFactory.getAdministrativeResourceResolver(authenticationInfo);
    }
}