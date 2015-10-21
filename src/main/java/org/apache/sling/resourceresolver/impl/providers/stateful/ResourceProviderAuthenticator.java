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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceProviderAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProviderAuthenticator.class);

    private final Map<ResourceProviderHandler, StatefulResourceProvider> stateful;

    private final List<StatefulResourceProvider> authenticated;

    private final List<StatefulResourceProvider> authenticatedModifiable;

    private final ResourceResolver resolver;

    private final Map<String, Object> authInfo;

    private final ResourceAccessSecurityTracker securityTracker;

    boolean allProvidersAuthenticated;

    public ResourceProviderAuthenticator(ResourceResolver resolver, Map<String, Object> authInfo,
            ResourceAccessSecurityTracker securityTracker) throws LoginException {
        this.stateful = new IdentityHashMap<ResourceProviderHandler, StatefulResourceProvider>();
        this.authenticated = new ArrayList<StatefulResourceProvider>();
        this.authenticatedModifiable = new ArrayList<StatefulResourceProvider>();
        this.resolver = resolver;
        this.authInfo = authInfo;
        this.securityTracker = securityTracker;
    }

    public void authenticateAll(List<ResourceProviderHandler> handlers) throws LoginException {
        for (ResourceProviderHandler h : handlers) {
            authenticate(h);
        }
    }

    private StatefulResourceProvider authenticate(ResourceProviderHandler handler) throws LoginException {
        StatefulResourceProvider rp = stateful.get(handler);
        if (rp == null) {
            rp = createStateful(handler);
            if (rp == null) {
                return null;
            }
            stateful.put(handler, rp);
            if (handler.getInfo().getAuthType() != AuthType.no) {
                authenticated.add(rp);
            }
            if (handler.getInfo().getModifiable()) {
                authenticatedModifiable.add(rp);
            }
        }
        return rp;
    }

    public StatefulResourceProvider getStateful(ResourceProviderHandler handler) {
        try {
            return authenticate(handler);
        } catch (LoginException e) {
            throw new SlingException("Can't authenticate provider", e);
        }
    }

    public Collection<StatefulResourceProvider> getAllUsedAuthenticated() {
        return authenticated;
    }

    public Collection<StatefulResourceProvider> getAllUsedModifiable() {
        return authenticatedModifiable;
    }

    public Collection<StatefulResourceProvider> getAll(List<ResourceProviderHandler> handlers) {
        List<StatefulResourceProvider> result = new ArrayList<StatefulResourceProvider>(handlers.size());
        for (ResourceProviderHandler h : handlers) {
            result.add(getStateful(h));
        }
        return result;
    }

    private StatefulResourceProvider createStateful(ResourceProviderHandler handler) throws LoginException {
        ResourceProvider<?> rp = handler.getResourceProvider();
        if (rp == null) {
            logger.warn("Empty resource provider for {}", handler);
            return null;
        }
        StatefulResourceProvider authenticated;
        authenticated = new AuthenticatedResourceProvider(rp, handler.getInfo(), resolver, authInfo);
        if (handler.getInfo().getUseResourceAccessSecurity()) {
            authenticated = new SecureResourceProvider(authenticated, securityTracker);
        }
        return authenticated;
    }

}