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
package org.apache.sling.testing.mock.sling;

import java.util.Map;

import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.osgi.service.component.ComponentContext;

/**
 * Overrides some behavior of {@link ResourceResolverFactoryActivator} to allow
 * usage in mocking.
 */
class MockResourceResolverFactoryActivator extends ResourceResolverFactoryActivator {

    // make public
    @Override
    public void activate(final ComponentContext componentContext) {
        super.activate(componentContext);
    }

    // make public
    @Override
    public void bindResourceProviderFactory(final ResourceProviderFactory provider, final Map<String, Object> props) {
        super.bindResourceProviderFactory(provider, props);
    }

    // make public
    @Override
    public void bindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        super.bindResourceProvider(provider, props);
    }

}
