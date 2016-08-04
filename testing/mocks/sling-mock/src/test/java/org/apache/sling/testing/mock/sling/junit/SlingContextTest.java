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

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class SlingContextTest {

    private final SlingContextCallback contextBeforeSetup = mock(SlingContextCallback.class);
    private final SlingContextCallback contextAfterSetup = mock(SlingContextCallback.class);
    private final SlingContextCallback contextBeforeTeardown = mock(SlingContextCallback.class);
    private final SlingContextCallback contextAfterTeardown = mock(SlingContextCallback.class);

    // Run all unit tests for each resource resolver types listed here
    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .beforeSetUp(contextBeforeSetup)
        .afterSetUp(contextAfterSetup)
        .beforeTearDown(contextBeforeTeardown)
        .afterTearDown(contextAfterTeardown)
        .resourceResolverFactoryActivatorProps(ImmutableMap.<String, Object>of("resource.resolver.searchpath", new String[] {
            "/apps",
            "/libs",
            "/testpath",
        }))
        .build();

    @Before
    public void setUp() throws IOException, PersistenceException {
        verify(contextBeforeSetup).execute(context);
        verify(contextAfterSetup).execute(context);
    }

    @Test
    public void testRequest() {
        assertNotNull(context.request());
    }

    /**
     * Test if custom searchpath /testpath added in this SlingContext is handled correctly.
     */
    @Test
    public void testResourceResolverFactoryActivatorProps() {
      context.create().resource("/apps/node1");

      context.create().resource("/libs/node1");
      context.create().resource("/libs/node2");

      context.create().resource("/testpath/node1");
      context.create().resource("/testpath/node2");
      context.create().resource("/testpath/node3");

      assertEquals("/apps/node1", context.resourceResolver().getResource("node1").getPath());
      assertEquals("/libs/node2", context.resourceResolver().getResource("node2").getPath());
      assertEquals("/testpath/node3", context.resourceResolver().getResource("node3").getPath());
      assertNull(context.resourceResolver().getResource("node4"));
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

    @Test
    public void testRegisterAdapterOverlayStatic() {
        prepareInitialAdapterFactory();
        
        // register overlay adapter with static adaption
        context.registerAdapter(TestAdaptable.class, String.class, "static-adaption");

        // test overlay adapter with static adaption
        assertEquals("static-adaption", new TestAdaptable("testMessage2").adaptTo(String.class));
    }

    @Test
    public void testRegisterAdapterOverlayDynamic() {
        prepareInitialAdapterFactory();
        
        // register overlay adapter with dynamic adaption
        context.registerAdapter(TestAdaptable.class, String.class, new Function<TestAdaptable, String>() {
            @Override
            public String apply(TestAdaptable input) {
                return input.getMessage() + "-dynamic";
            }
        });

        // test overlay adapter with dynamic adaption
        assertEquals("testMessage3-dynamic", new TestAdaptable("testMessage3").adaptTo(String.class));
    }
    
    private void prepareInitialAdapterFactory() {
        // register "traditional" adapter factory without specific service ranking
        AdapterFactory adapterFactory = new AdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
                return (AdapterType)(((TestAdaptable)adaptable).getMessage() + "-initial");
            }
        };
        context.registerService(AdapterFactory.class, adapterFactory, ImmutableMap.<String, Object>builder()
                .put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { TestAdaptable.class.getName() })
                .put(AdapterFactory.ADAPTER_CLASSES, new String[] { String.class.getName() })
                .build());
        
        // test initial adapter factory
        assertEquals("testMessage1-initial", new TestAdaptable("testMessage1").adaptTo(String.class));        
    }


    private static class TestAdaptable extends SlingAdaptable {
        
        private final String message;

        public TestAdaptable(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
        
    }

}
