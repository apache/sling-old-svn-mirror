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
package org.apache.sling.testing.mock.sling.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Function;

@RunWith(MockitoJUnitRunner.class)
public class SlingContextTest {

    private final SlingContextCallback contextSetup = mock(SlingContextCallback.class);
    private final SlingContextCallback contextTeardown = mock(SlingContextCallback.class);

    // Run all unit tests for each resource resolver types listed here
    @Rule
    public SlingContext context = new SlingContext(contextSetup, contextTeardown,
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setUp() throws IOException, PersistenceException {
        verify(contextSetup).execute(context);
    }

    @Test
    public void testRequest() {
        assertNotNull(context.request());
    }

    @Test
    public void testRegisterAdapter() {
        
        // prepare some adapter factories
        context.registerAdapter(ResourceResolver.class, Integer.class, 5);
        context.registerAdapter(ResourceResolver.class, String.class, new Function<ResourceResolver,String>() {
            @Override
            public String apply(ResourceResolver input) {
                return ">" + input.toString();
            }
        });
        
        // test adaption
        assertEquals(Integer.valueOf(5), context.resourceResolver().adaptTo(Integer.class));
        assertEquals(">" + context.resourceResolver().toString(), context.resourceResolver().adaptTo(String.class));
        assertNull(context.resourceResolver().adaptTo(Double.class));
    }

}
