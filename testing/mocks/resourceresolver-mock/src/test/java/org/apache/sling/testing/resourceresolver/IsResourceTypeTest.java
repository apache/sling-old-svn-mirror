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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class IsResourceTypeTest {

    private ResourceResolver resourceResolver;

    @Before
    public final void setUp() throws IOException, LoginException {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);       
    }

    @Test
    public void testIsResourceResolver() throws PersistenceException {
        Resource root= resourceResolver.getResource("/");
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("jcr:primaryType", "bar");
        properties.put("sling:resourceType", "baz");
        properties.put("sling:resourceSuperType", "qux");
        Resource resource = resourceResolver.create(root, "foo", properties);
        
        Assert.assertTrue(resource.isResourceType("bar"));
        Assert.assertTrue(resource.isResourceType("baz"));
        Assert.assertTrue(resource.isResourceType("qux"));
        Assert.assertFalse(resource.isResourceType("invalid"));
    }

}
