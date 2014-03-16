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
package org.apache.sling.resourceaccesssecurity.impl;


import junit.framework.TestCase;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertTrue;

public class ResourceAccessSecurityImplTests {

    ServiceReference serviceReference;
    ResourceAccessSecurity resourceAccessSecurity;
    ResourceAccessGate resourceAccessGate;

    @Before
    public void setUp() {
        resourceAccessSecurity = new ProviderResourceAccessSecurityImpl();
    }


    @Test
    public void testCanUpdate(){
        initMocks("/content", new String[] { "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        assertTrue(resourceAccessSecurity.canUpdate(resource));
    }

    @Test
    public void testCannotUpdate(){
        initMocks("/content", new String[] { "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.DENIED);
        assertFalse(resourceAccessSecurity.canUpdate(resource));
    }

    @Test
    public void testCannotUpdateWrongPath(){
        initMocks("/content", new String[] { "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/wrongcontent");
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        assertFalse(resourceAccessSecurity.canUpdate(resource));
    }

    @Test
    public void testCanUpdateUsingReadableResource(){
        // one needs to have also read rights to obtain the resource

        initMocks("/content", new String[] { "read", "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");

        ModifiableValueMap valueMap = mock(ModifiableValueMap.class);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);

        when(resourceAccessGate.canRead(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        Resource readableResource = resourceAccessSecurity.getReadableResource(resource);

        ModifiableValueMap resultValueMap = readableResource.adaptTo(ModifiableValueMap.class);


        resultValueMap.put("modified", "value");

        verify(valueMap, times(1)).put("modified", "value");
    }


    @Test
    public void testCannotUpdateUsingReadableResourceIfCannotRead(){
        initMocks("/content", new String[] { "read", "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");

        ModifiableValueMap valueMap = mock(ModifiableValueMap.class);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);

        when(resourceAccessGate.canRead(resource)).thenReturn(ResourceAccessGate.GateResult.DENIED);
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        Resource readableResource = resourceAccessSecurity.getReadableResource(resource);


        assertNull(readableResource);
    }


    @Test
    public void testCannotUpdateUsingReadableResourceIfCannotUpdate(){
        initMocks("/content", new String[] { "read", "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");

        ModifiableValueMap valueMap = mock(ModifiableValueMap.class);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);

        when(resourceAccessGate.canRead(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.DENIED);
        Resource readableResource = resourceAccessSecurity.getReadableResource(resource);

        ModifiableValueMap resultValueMap = readableResource.adaptTo(ModifiableValueMap.class);

        assertNull(resultValueMap);
    }


    @Test
    public void testCannotUpdateAccidentallyUsingReadableResourceIfCannotUpdate(){
        initMocks("/content", new String[] { "read", "update"} );

        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/content");

        ModifiableValueMap valueMap = mock(ModifiableValueMap.class);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);

        when(resourceAccessGate.canRead(resource)).thenReturn(ResourceAccessGate.GateResult.GRANTED);
        when(resourceAccessGate.canUpdate(resource)).thenReturn(ResourceAccessGate.GateResult.DENIED);
        Resource readableResource = resourceAccessSecurity.getReadableResource(resource);

        ValueMap resultValueMap = readableResource.adaptTo(ValueMap.class);

        resultValueMap.put("modified", "value");

        verify(valueMap, times(0)).put("modified", "value");
    }

    private void initMocks(String path, String[] operations){
        serviceReference = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext bundleContext = mock(BundleContext.class);
        resourceAccessGate = mock(ResourceAccessGate.class);

        when(serviceReference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(resourceAccessGate);

        when(serviceReference.getProperty(ResourceAccessGate.PATH)).thenReturn(path);
        when(serviceReference.getProperty(ResourceAccessGate.OPERATIONS)).thenReturn(operations);

        ((ProviderResourceAccessSecurityImpl) resourceAccessSecurity).bindResourceAccessGate(serviceReference);
    }



}
