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
package org.apache.sling.testing.mock.sling.resource;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.junit.Rule;
import org.junit.Test;

/**
 * Validates correct registering and mapping of JCR namespaces, esp. the sling namespace. 
 */
public abstract class AbstractJcrNamespaceTest {
    
    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();
    
    @Test
    public void testSling4362() throws RepositoryException {
        ResourceResolver resolver = MockSling.newResourceResolver(getResourceResolverType());
        
        ContentLoader contentLoader = new ContentLoader(resolver);
        contentLoader.json("/json-import-samples/SLING-4362.json", context.uniqueRoot().content() + "/foo");

        Resource resource = resolver.getResource(context.uniqueRoot().content() + "/foo");
        
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals("fooType", props.get(SLING_RESOURCE_TYPE_PROPERTY));
        assertEquals("fooType", resource.getResourceType());
    }

    @Test
    public void testSling4362_ViaContextRule() throws RepositoryException {
        ResourceResolver resolver = context.resourceResolver();
        
        ContentLoader contentLoader = new ContentLoader(resolver);
        contentLoader.json("/json-import-samples/SLING-4362.json", context.uniqueRoot().content() + "/foo");

        Resource resource = resolver.getResource(context.uniqueRoot().content() + "/foo");
        
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals("fooType", props.get(SLING_RESOURCE_TYPE_PROPERTY));
        assertEquals("fooType", resource.getResourceType());
    }

}
