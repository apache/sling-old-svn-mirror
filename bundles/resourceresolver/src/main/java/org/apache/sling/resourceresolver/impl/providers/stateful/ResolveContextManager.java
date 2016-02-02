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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolverContext;

public class ResolveContextManager {

    private final Map<ResourceProviderHandler, ResolverContext<Object>> contextMap;

    public ResolveContextManager(final ResourceResolver resolver) {
        this.contextMap = new IdentityHashMap<ResourceProviderHandler, ResolverContext<Object>>();
    }

    public ResolverContext<Object> getResolveContext(final ResourceProviderHandler handler) {
        return this.contextMap.get(handler);
    }
}
