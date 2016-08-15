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
package org.apache.sling.auth.core.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.BundleAuthenticationRequirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = BundleAuthenticationRequirement.class,
           scope = ServiceScope.BUNDLE,
           property = Constants.SERVICE_VENDOR + "=The Apache Software Foundation")
public class BundleAuthenticationRequirementImpl implements BundleAuthenticationRequirement {

    static final String PREFIX = "Apache Sling Authentication Requirements for Bundle ";

    /** The client bundle id. */
    private long bundleId;

    /** Provider string for info */
    private String provider;

    private PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache;

    @Reference
    private Authenticator slingAuthenticator;

    @Activate
    private void activate(final ComponentContext componentCtx) {
        final Bundle bundle = componentCtx.getUsingBundle();
        this.bundleId = bundle.getBundleId();
        this.provider = PREFIX +
                bundle.getSymbolicName() +
                ":" +
                bundle.getVersion() +
                " (" +
                String.valueOf(bundle.getBundleId()) +
                ")";
        this.authRequiredCache = ((SlingAuthenticator)slingAuthenticator).authRequiredCache;
    }

    @Deactivate
    private void deactivate() {
        clearRequirements();
    }

    @Override
    public void setRequirements(@Nonnull final Map<String, Boolean> requirements) {
        final Collection<AuthenticationRequirementHolder> reqHolders = createHolders(requirements);

        // remove existing entries
        clearRequirements();

        // register the new entries
        register(reqHolders);
    }

    @Override
    public void appendRequirements(@Nonnull final Map<String, Boolean> requirements) {
        final Collection<AuthenticationRequirementHolder> reqHolders = createHolders(requirements);
        register(reqHolders);
    }

    @Override
    public void removeRequirements(@Nonnull final Map<String, Boolean> requirements) {
        final Collection<AuthenticationRequirementHolder> reqHolders = createHolders(requirements);
        for (AuthenticationRequirementHolder authReq : reqHolders) {
            authRequiredCache.removeHolder(authReq);
        }
    }

    @Override
    public void clearRequirements() {
        authRequiredCache.removeAllMatchingHolders(new AuthenticationRequirementHolder("", false, null) {

            @Override
            public boolean equals(final Object other) {
                if ( other instanceof BundleAuthenticationRequirementHolder
                     && ((BundleAuthenticationRequirementHolder)other).getBundleId() == bundleId ) {
                    return true;
                }
                return false;
            }
        });

    }

    private Set<AuthenticationRequirementHolder> createHolders(@Nonnull Map<String,Boolean> requirements) {
        final Set<AuthenticationRequirementHolder> holders = new HashSet<AuthenticationRequirementHolder>(requirements.size());
        for (final Map.Entry<String, Boolean> entry : requirements.entrySet()) {
            holders.add(new BundleAuthenticationRequirementHolder(entry.getKey(), entry.getValue()));
        }
        return holders;
    }

    private void register(@Nonnull final Collection<AuthenticationRequirementHolder> authReqs) {
        for (AuthenticationRequirementHolder authReq : authReqs) {
            authRequiredCache.addHolder(authReq);
        }
    }

    private final class BundleAuthenticationRequirementHolder extends AuthenticationRequirementHolder {

        public BundleAuthenticationRequirementHolder(final String fullPath,
                final boolean requiresAuthentication) {
            super(fullPath, requiresAuthentication, null);
        }

        @Override
        String getProvider() {
            return provider;
        }

        long getBundleId() {
            return bundleId;
        }
    }
}
