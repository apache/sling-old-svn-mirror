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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.junit.Before;
import org.junit.Test;


public class IsResourceTypeTest {

    private ResourceResolver resolver;

    @Before
    public final void setUp() throws LoginException {
        resolver = new MockResourceResolverFactory().getResourceResolver(null);
    }
    
    @Test
    public void testIsResourceType() {
        /**
         * prepare resource type hierarchy
         * /types/1
         *  +- /types/2
         *    +- /types/3
         */
        add("/types/1", "/types/component", "/types/2");
        add("/types/2", "/types/component", "/types/3");
        add("/types/3", "/types/component");

        Resource resourceT1 = add("/resourceT1", "/types/1");
        Resource resourceT2 = add("/resourceT2", "/types/2");
        Resource resourceT3 = add("/resourceT3", "/types/3");

        assertTrue(resolver.isResourceType(resourceT1, "/types/1"));
        assertTrue(resolver.isResourceType(resourceT1, "/types/2"));
        assertTrue(resolver.isResourceType(resourceT1, "/types/3"));
        assertFalse(resolver.isResourceType(resourceT1, "/types/component"));
        assertFalse(resolver.isResourceType(resourceT1, "/types/unknown"));

        assertFalse(resolver.isResourceType(resourceT2, "/types/1"));
        assertTrue(resolver.isResourceType(resourceT2, "/types/2"));
        assertTrue(resolver.isResourceType(resourceT2, "/types/3"));
        assertFalse(resolver.isResourceType(resourceT2, "/types/component"));
        assertFalse(resolver.isResourceType(resourceT2, "/types/unknown"));

        assertFalse(resolver.isResourceType(resourceT3, "/types/1"));
        assertFalse(resolver.isResourceType(resourceT3, "/types/2"));
        assertTrue(resolver.isResourceType(resourceT3, "/types/3"));
        assertFalse(resolver.isResourceType(resourceT3, "/types/component"));
        assertFalse(resolver.isResourceType(resourceT3, "/types/unknown"));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/SLING-6327">SLING-6327</a>
     */
    @Test
    public void testIsResourceTypeWithMixedAbsoluteAndRelativePaths() {
        Resource resourceT1 = add("/resourceT1", "types/1");
        Resource resourceT2 = add("/resourceT2", "/apps/types/2");
        Resource resourceT3 = add("/resourceT3", "/libs/types/3");

        assertTrue(resolver.isResourceType(resourceT1, "/libs/types/1"));
        assertTrue(resolver.isResourceType(resourceT1, "/apps/types/1"));
        assertTrue(resolver.isResourceType(resourceT1, "types/1"));

        assertTrue(resolver.isResourceType(resourceT2, "/apps/types/2"));
        assertTrue(resolver.isResourceType(resourceT2, "types/2"));
        assertTrue(resolver.isResourceType(resourceT2, "/libs/types/2"));

        assertTrue(resolver.isResourceType(resourceT3, "/apps/types/3"));
        assertTrue(resolver.isResourceType(resourceT3, "types/3"));
        assertTrue(resolver.isResourceType(resourceT3, "/libs/types/3"));
    }

    @Test(expected=SlingException.class)
    public void testIsResourceCyclicHierarchyDirect() {
        /**
         * prepare resource type hierarchy
         * /types/1  <---+
         *  +- /types/2 -+
         */
        add("/types/1", "/types/component", "/types/2");
        add("/types/2", "/types/component", "/types/1");

        Resource resource = add("/resourceT1", "/types/1");

        assertTrue(resolver.isResourceType(resource, "/types/1"));
        assertTrue(resolver.isResourceType(resource, "/types/2"));

        // this should throw a SlingException when detecting the cyclic hierarchy
        resolver.isResourceType(resource, "/types/unknown");
    }

    @Test(expected=SlingException.class)
    public void testIsResourceCyclicHierarchyIndirect() {
        /**
         * prepare resource type hierarchy
         * /types/1   <----+
         *  +- /types/2    |
         *    +- /types/3 -+
         */
        add("/types/1", "/types/component", "/types/2");
        add("/types/2", "/types/component", "/types/3");
        add("/types/3", "/types/component", "/types/1");

        Resource resource = add("/resourceT1", "/types/1");

        assertTrue(resolver.isResourceType(resource, "/types/1"));
        assertTrue(resolver.isResourceType(resource, "/types/2"));
        assertTrue(resolver.isResourceType(resource, "/types/3"));

        // this should throw a SlingException when detecting the cyclic hierarchy
        resolver.isResourceType(resource, "/types/unknown");
    }

    private Resource add(String path, String resourceType) {
        return add(path, resourceType, null);
    }

    private Resource add(String path, String resourceType, String resourceSuperType) {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("sling:resourceType", resourceType);
            if (resourceSuperType != null) {
                props.put("sling:resourceSuperType", resourceSuperType);
            }
            return ResourceUtil.getOrCreateResource(resolver, path, props, null, true);
        }
        catch (PersistenceException ex) {
            throw new RuntimeException(ex);
        }
    }

}
