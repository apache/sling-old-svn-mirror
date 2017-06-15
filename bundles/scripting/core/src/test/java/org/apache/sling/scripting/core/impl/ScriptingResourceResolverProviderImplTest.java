/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.servlet.ServletRequestEvent;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ScriptingResourceResolverProviderImplTest {

    private static final int MAX_CONCURRENT_RESOLVERS = 200;
    private static final int RESOLVER_REUSE_FOR_SAME_THREAD = 100;
    private ScriptingResourceResolverProviderImpl scriptingResourceResolverFactory;
    private Set<ResourceResolver> delegates;

    @Before
    public void setUp() throws LoginException {
        delegates = Collections.synchronizedSet(new HashSet<ResourceResolver>());
        ResourceResolverFactory rrf = mock(ResourceResolverFactory.class);
        when(rrf.getServiceResourceResolver(null)).thenAnswer(new Answer<ResourceResolver>() {
            @Override
            public ResourceResolver answer(InvocationOnMock invocation) throws Throwable {
                ResourceResolver delegate = getMockedRR();
                delegates.add(delegate);
                return delegate;
            }
        });
        scriptingResourceResolverFactory = new ScriptingResourceResolverProviderImpl();
        Whitebox.setInternalState(scriptingResourceResolverFactory, "rrf", rrf);
    }

    @After
    public void tearDown() {
        scriptingResourceResolverFactory = null;
        delegates = null;
    }

    @Test
    public void testGetRequestScopedResourceResolver() throws Exception {
        ResourceResolver resourceResolver = scriptingResourceResolverFactory.getRequestScopedResourceResolver();
        assertEquals("sling-scripting", resourceResolver.getUserID());
        scriptingResourceResolverFactory.requestDestroyed(mock(ServletRequestEvent.class));
        assertEquals(1, delegates.size());
        for (ResourceResolver delegate : delegates) {
            verify(delegate).close();
        }
    }

    @Test
    public void testGetRequestScopedResourceResolverWithThreads() throws Exception {
        Collection<Callable<ResourceResolver>> callables = new ArrayList<>(MAX_CONCURRENT_RESOLVERS);
        for (int i = 0; i < MAX_CONCURRENT_RESOLVERS; i++) {
            callables.add(createCallable(scriptingResourceResolverFactory));
        }
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_RESOLVERS);
        List<Future<ResourceResolver>> futures = executor.invokeAll(callables);
        Set<ResourceResolver> resolvers = new HashSet<>();
        for (Future<ResourceResolver> future : futures) {
            resolvers.add(future.get());
        }
        assertEquals("The number of ScriptingResourceResolvers is not what we expected.", MAX_CONCURRENT_RESOLVERS, resolvers.size());
        assertEquals("The number of delegate resource resolvers is not what we expected.", MAX_CONCURRENT_RESOLVERS, delegates.size());
        for (ResourceResolver delegate : delegates) {
            verify(delegate).close();
        }

    }

    private Callable<ResourceResolver> createCallable(final ScriptingResourceResolverProviderImpl scriptingResourceResolverFactory) {
        return new Callable<ResourceResolver>() {
            @Override
            public ResourceResolver call() {
                ResourceResolver resourceResolver = scriptingResourceResolverFactory.getRequestScopedResourceResolver();
                for (int i = 0; i < RESOLVER_REUSE_FOR_SAME_THREAD; i++) {
                    ResourceResolver subsequentResolver = scriptingResourceResolverFactory.getRequestScopedResourceResolver();
                    assertEquals("Expected that subsequent calls to ScriptingResourceResolverProvider#getRequestScopedResourceResolver() " +
                            "from the same thread will not create additional resolvers.", resourceResolver, subsequentResolver);
                }
                scriptingResourceResolverFactory.requestDestroyed(mock(ServletRequestEvent.class));
                return resourceResolver;
            }
        };
    }

    private ResourceResolver getMockedRR() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("sling-scripting");
        return resolver;
    }

}
