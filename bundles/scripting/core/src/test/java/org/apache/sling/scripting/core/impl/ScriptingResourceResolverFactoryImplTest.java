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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ScriptingResourceResolverFactoryImplTest {

    private static final int MAX_CONCURRENT_RESOLVERS = 100;

    @Test
    public void testGetRequestScopedResourceResolver() throws Exception {
        ResourceResolverFactory rrf = mock(ResourceResolverFactory.class);
        ResourceResolver delegate = mock(ResourceResolver.class);
        when(delegate.getUserID()).thenReturn("sling-scripting");
        when(rrf.getServiceResourceResolver(null)).thenReturn(delegate);
        ScriptingResourceResolverFactoryImpl scriptingResourceResolverFactory = new ScriptingResourceResolverFactoryImpl();
        Whitebox.setInternalState(scriptingResourceResolverFactory, "rrf", rrf);
        assertNull(scriptingResourceResolverFactory.getRequestScopedResourceResolver());
        SlingRequestEvent sre1 = new SlingRequestEvent(mock(ServletContext.class), mock(ServletRequest.class), SlingRequestEvent.EventType
                .EVENT_INIT);
        scriptingResourceResolverFactory.onEvent(sre1);
        ResourceResolver resourceResolver = scriptingResourceResolverFactory.getRequestScopedResourceResolver();
        assertEquals("sling-scripting", resourceResolver.getUserID());
        SlingRequestEvent sre2 = new SlingRequestEvent(mock(ServletContext.class), mock(ServletRequest.class), SlingRequestEvent.EventType
                .EVENT_DESTROY);
        scriptingResourceResolverFactory.onEvent(sre2);
        assertNull(scriptingResourceResolverFactory.getRequestScopedResourceResolver());
        verify(delegate).close();
    }

    @Test
    public void testGetRequestScopedResourceResolverWithThreads() throws Exception {
        ResourceResolverFactory rrf = mock(ResourceResolverFactory.class);
        final Set<ResourceResolver> delegates = new HashSet<>();
        when(rrf.getServiceResourceResolver(null)).thenAnswer(new Answer<ResourceResolver>() {
            @Override
            public ResourceResolver answer(InvocationOnMock invocation) throws Throwable {
                ResourceResolver delegate = getMockedRR();
                delegates.add(delegate);
                return delegate;
            }
        });
        final ScriptingResourceResolverFactoryImpl scriptingResourceResolverFactory = new ScriptingResourceResolverFactoryImpl();
        Whitebox.setInternalState(scriptingResourceResolverFactory, "rrf", rrf);
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
        assertEquals(MAX_CONCURRENT_RESOLVERS, resolvers.size());
        assertEquals(MAX_CONCURRENT_RESOLVERS, delegates.size());
        for (ResourceResolver delegate : delegates) {
            verify(delegate).close();
        }

    }

    private Callable<ResourceResolver> createCallable(final ScriptingResourceResolverFactoryImpl scriptingResourceResolverFactory) {
        return new Callable<ResourceResolver>() {
            @Override
            public ResourceResolver call() throws Exception {
                try {
                    scriptingResourceResolverFactory.onEvent(new SlingRequestEvent(mock(ServletContext.class), mock(ServletRequest.class),
                            SlingRequestEvent.EventType.EVENT_INIT));
                    return scriptingResourceResolverFactory.getRequestScopedResourceResolver();
                } finally {
                    scriptingResourceResolverFactory.onEvent(new SlingRequestEvent(mock(ServletContext.class), mock(ServletRequest.class),
                            SlingRequestEvent.EventType.EVENT_DESTROY));
                    assertNull(scriptingResourceResolverFactory.getRequestScopedResourceResolver());
                }
            }
        };
    }

    private ResourceResolver getMockedRR() {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getUserID()).thenReturn("sling-scripting");
        return resolver;
    }

}
