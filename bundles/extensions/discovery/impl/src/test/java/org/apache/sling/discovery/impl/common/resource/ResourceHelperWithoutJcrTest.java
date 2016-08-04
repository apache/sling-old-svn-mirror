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
package org.apache.sling.discovery.impl.common.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ResourceHelperWithoutJcrTest {
    
    private static final Map<String,Object> VALUEMAP_1 = ImmutableMap.<String, Object>builder()
            .put("prop1", "value1")
            .put("prop2", 25)
            .build();
    private static final Map<String,Object> VALUEMAP_2 = ImmutableMap.<String, Object>builder()
            .put("prop1", "value2")
            .put("prop5", true)
            .build();
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    
    /**
     * Test moveResource method that normally relies to a JCR move operation without JCR = fallback to resource API.
     * @throws PersistenceException
     */
    @Test
    public void testMoveResource() throws PersistenceException {
        
        // prepare some test content
        Resource source = context.create().resource("/content/path1", VALUEMAP_1);
        context.create().resource("/content/path1/child1", VALUEMAP_1);
        context.create().resource("/content/path1/child1/child11", VALUEMAP_1);
        context.create().resource("/content/path1/child1/child12", VALUEMAP_2);
        context.create().resource("/content/path1/child2", VALUEMAP_2);
        
        ResourceHelper.moveResource(source, "/content/path2");
        
        assertNull(context.resourceResolver().getResource("/content/path1"));
        
        Resource target = context.resourceResolver().getResource("/content/path2");
        Resource target1 = context.resourceResolver().getResource("/content/path2/child1");
        Resource target11 = context.resourceResolver().getResource("/content/path2/child1/child11");
        Resource target12 = context.resourceResolver().getResource("/content/path2/child1/child12");
        Resource target2 = context.resourceResolver().getResource("/content/path2/child2");
        
        assertEquals(VALUEMAP_1, ResourceUtil.getValueMap(target));
        assertEquals(VALUEMAP_1, ResourceUtil.getValueMap(target1));
        assertEquals(VALUEMAP_1, ResourceUtil.getValueMap(target11));
        assertEquals(VALUEMAP_2, ResourceUtil.getValueMap(target12));
        assertEquals(VALUEMAP_2, ResourceUtil.getValueMap(target2));
    }

}
