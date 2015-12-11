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

import java.io.IOException;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Server-side integration test for the 
 *  ResourceBuilder, acquired via the ResourceBuilderProvider
 */
public class ResourceBuilderIT {
    
    @Rule
    public final TeleporterRule teleporter = 
        TeleporterRule
        .forClass(getClass(), "RBIT_Teleporter")
        .withResources("/files/");
    
    private ResourceBuilder builder;
    private ResourceResolver resolver;
    private String testRootPath;
    private Resource parent;
    private ResourceAssertions A;

    @Before
    public void setup() throws LoginException, PersistenceException {
        testRootPath = getClass().getSimpleName() + "-" + UUID.randomUUID().toString(); 
        resolver = teleporter.getService(ResourceResolverFactory.class).getAdministrativeResourceResolver(null);
        final Resource root = resolver.getResource("/");
        parent = resolver.create(root, testRootPath, null);
        builder = teleporter.getService(ResourceBuilderProvider.class).getResourceBuilder(parent);
        A = new ResourceAssertions(testRootPath, resolver);
    }
    
    @After
    public void cleanup() throws PersistenceException {
        if(resolver != null && parent != null) {
            resolver.delete(parent);
            resolver.commit();
        }
    }
    
    @Test
    public void simpleResource() {
        builder
            .resource("foo", "title", testRootPath)
            .commit();
        A.assertProperties("foo", "title", testRootPath);
    }
    
    @Test
    public void smallTreeWithFile() throws IOException {
        builder
            .resource("somefolder")
            .file("the-model.js", getClass().getResourceAsStream("/files/models.js"), "foo", 42L)
            .commit();
        
        A.assertFile("somefolder/the-model.js", "foo", "yes, it worked", 42L);
    }
}