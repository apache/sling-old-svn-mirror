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
package org.apache.sling.testing.resourceresolver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.EventAdmin;

public class MockResourceResolverFactory implements ResourceResolverFactory {

    private final Map<String, Map<String, Object>> resources = new LinkedHashMap<String, Map<String, Object>>();

    private final EventAdmin eventAdmin;

    public MockResourceResolverFactory(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
        resources.put("/", new HashMap<String, Object>());
    }

    public MockResourceResolverFactory() {
        this(null);
    }

    @Override
    public ResourceResolver getResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return new MockResourceResolver(this.eventAdmin, resources);
    }

    @Override
    public ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return new MockResourceResolver(this.eventAdmin, resources);
    }

    @Override
    public ResourceResolver getServiceResourceResolver(
            Map<String, Object> authenticationInfo) throws LoginException {
        return new MockResourceResolver(this.eventAdmin, resources);
    }
}
