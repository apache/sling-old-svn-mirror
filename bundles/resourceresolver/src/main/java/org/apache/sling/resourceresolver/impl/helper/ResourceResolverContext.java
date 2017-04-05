/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.resourceresolver.impl.helper;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.stateful.ProviderManager;

/**
 * The resource resolver context.
 *
 * This class is not thread safe (same as the resource resolver).
 */
public class ResourceResolverContext {

    private final ResourceResolver resolver;

    private final ProviderManager providerManager;

    public ResourceResolverContext(@Nonnull final ResourceResolver resolver, @Nonnull final ResourceAccessSecurityTracker tracker) {
        this.resolver = resolver;
        this.providerManager = new ProviderManager(resolver, tracker);
    }

    public ResourceResolver getResourceResolver() {
        return this.resolver;
    }

    public ProviderManager getProviderManager() {
        return this.providerManager;
    }
}
