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
package org.apache.sling.resourcebuilder.it;

import java.util.UUID;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.api.ResourceBuilderProvider;
import org.apache.sling.resourcebuilder.test.ResourceAssertions;

class TestEnvironment {
    
    final ResourceBuilder builder;
    final ResourceResolver resolver;
    final String testRootPath;
    final Resource parent;
    final ResourceAssertions A;

    TestEnvironment(TeleporterRule teleporter) throws LoginException, PersistenceException {
        testRootPath = getClass().getSimpleName() + "-" + UUID.randomUUID().toString(); 
        resolver = teleporter.getService(ResourceResolverFactory.class).getAdministrativeResourceResolver(null);
        final Resource root = resolver.getResource("/");
        parent = resolver.create(root, testRootPath, null);
        builder = teleporter.getService(ResourceBuilderProvider.class).getResourceBuilder(parent);
        A = new ResourceAssertions(testRootPath, resolver);
    }
    
    void cleanup() throws PersistenceException {
        if(resolver != null && parent != null) {
            resolver.delete(parent);
            resolver.commit();
        }
    }
}