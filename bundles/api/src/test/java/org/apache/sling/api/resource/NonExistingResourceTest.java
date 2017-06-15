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
package org.apache.sling.api.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonExistingResourceTest {

    @Mock
    private ResourceResolver resolver;

    @Test
    public void testGetParentWithNonExistingParent() {
        final NonExistingResource nonExistingResource = new NonExistingResource(resolver, "/nonExistingParent/nonExistingResource");
        
        when(resolver.getParent(nonExistingResource)).thenReturn(null);
        
        Resource parentResource = nonExistingResource.getParent();
        Assert.assertNotNull("Non existing parent of NonExistingResource must not return null!", parentResource);
        Assert.assertEquals("/nonExistingParent", parentResource.getPath());
        Assert.assertTrue(ResourceUtil.isNonExistingResource(parentResource));
    }

    @Test
    public void testGetParentWithExistingParent() throws PersistenceException {
        final NonExistingResource nonExistingResource = new NonExistingResource(resolver, "/existingParent/nonExistingResource");
        
        final Resource mockParentResource = mock(Resource.class);
        when(resolver.getParent(nonExistingResource)).thenReturn(mockParentResource);
        when(mockParentResource.getPath()).thenReturn("/existingParent");
        when(mockParentResource.getResourceType()).thenReturn("anyResourceType");
        
        Resource parentResource = nonExistingResource.getParent();
        Assert.assertNotNull("Existing parent of NonExistingResource must not return null!", parentResource);
        Assert.assertEquals("/existingParent", parentResource.getPath());
        Assert.assertFalse(ResourceUtil.isNonExistingResource(parentResource));
    }

}
