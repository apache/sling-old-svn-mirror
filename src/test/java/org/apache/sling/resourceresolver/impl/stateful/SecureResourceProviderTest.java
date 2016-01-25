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
package org.apache.sling.resourceresolver.impl.stateful;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.stateful.SecureResourceProvider;
import org.apache.sling.resourceresolver.impl.providers.stateful.StatefulResourceProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SecureResourceProviderTest {
    
    private ResourceAccessSecurity security;
    private ResourceResolver rr;
    private SecureResourceProvider src;
    private StatefulResourceProvider rp;
    private Resource first;
    private Resource second;

    @Before
    public void prepare() throws PersistenceException {

        rr = mock(ResourceResolver.class);
        
        security = mock(ResourceAccessSecurity.class);
        first = mock(Resource.class);
        second = mock(Resource.class);
        
        when(security.getReadableResource(first)).thenReturn(first);
        when(security.getReadableResource(second)).thenReturn(null);
        
        rp = mock(StatefulResourceProvider.class);
        when(rp.create("/some/path", Collections.<String, Object> emptyMap())).thenReturn(mock(Resource.class));
        when(rp.findResources("FIND ALL", "MockQueryLanguage")).thenReturn(Arrays.asList(first, second).iterator());
        
        when(rp.getResourceResolver()).thenReturn(rr);
        
        ResourceAccessSecurityTracker securityTracker = new ResourceAccessSecurityTracker() {
            @Override
            public ResourceAccessSecurity getApplicationResourceAccessSecurity() {
                return security;
            }
        };
        
        src = new SecureResourceProvider(rp, securityTracker);
                
    }

    @Test
    public void create_success() throws PersistenceException {
        
        when(security.canCreate("/some/path", rr)).thenReturn(true);
        
        assertNotNull("expected resource to be created", src.create("/some/path", Collections.<String, Object> emptyMap()));
    }
    
    @Test
    public void create_failure() throws PersistenceException {
        
        when(security.canCreate("/some/path", rr)).thenReturn(false);
        
        assertNull("expected resource to not be created", src.create("/some/path", Collections.<String, Object> emptyMap()));
    }
    
    @Test
    public void delete_success() throws PersistenceException {
        
        Resource toDelete = mock(Resource.class);
        
        when(security.canDelete(toDelete)).thenReturn(true);
        
        src.delete(toDelete);
        
        verify(rp).delete(toDelete);
    }
    
    @Test
    public void delete_failure() throws PersistenceException {
        
        Resource toDelete = mock(Resource.class);
        
        when(security.canDelete(toDelete)).thenReturn(false);
        
        src.delete(toDelete);
        
        Mockito.verifyZeroInteractions(rp);
    }
    
    @Test
    public void find() {
        
        Iterator<Resource> resources = src.findResources("FIND ALL", "MockQueryLanguage");
        
        assertThat("resources should contain at least one item", resources.hasNext(), equalTo(true));
        
        Resource resource = resources.next();
        
        assertThat("unexpected resource found", resource, equalTo(first));
        
        assertThat("resources should exactly at least one item", resources.hasNext(), equalTo(false));
    }
}
