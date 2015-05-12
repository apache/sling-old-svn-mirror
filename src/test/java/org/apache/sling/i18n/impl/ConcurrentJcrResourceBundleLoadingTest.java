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
package org.apache.sling.i18n.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

/**
 * Test case to verify that each bundle is only loaded once, even
 * if concurrent requests for the same bundle are made.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrResourceBundleProvider.class)
public class ConcurrentJcrResourceBundleLoadingTest {

    @Mock JcrResourceBundle english;
    @Mock JcrResourceBundle german;

    @Test
    public void loadBundlesOnlyOncePerLocale() throws Exception {
        JcrResourceBundleProvider provider = spy(new JcrResourceBundleProvider());
        provider.activate(createComponentContext(new Hashtable<String, Object>()));

        doReturn(english).when(provider, "createResourceBundle", eq(null), eq(Locale.ENGLISH));
        doReturn(german).when(provider, "createResourceBundle", eq(null), eq(Locale.GERMAN));

        assertEquals(english, provider.getResourceBundle(Locale.ENGLISH));
        assertEquals(english, provider.getResourceBundle(Locale.ENGLISH));
        assertEquals(german, provider.getResourceBundle(Locale.GERMAN));
        assertEquals(german, provider.getResourceBundle(Locale.GERMAN));

        verifyPrivate(provider, times(2)).invoke("createResourceBundle", eq(null), any(Locale.class));
    }

    @Test
    public void loadBundlesOnlyOnceWithConcurrentRequests() throws Exception {
        final JcrResourceBundleProvider provider = spy(new JcrResourceBundleProvider());
        provider.activate(createComponentContext(new Hashtable<String, Object>()));

        doReturn(english).when(provider, "createResourceBundle", eq(null), eq(Locale.ENGLISH));
        doReturn(german).when(provider, "createResourceBundle", eq(null), eq(Locale.GERMAN));

        final int numberOfThreads = 40;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads / 2);
        for (int i = 0; i < numberOfThreads; i++) {
            final Locale language = i < numberOfThreads / 2 ? Locale.ENGLISH : Locale.GERMAN;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    provider.getResourceBundle(language);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        verifyPrivate(provider, times(1)).invoke("createResourceBundle", eq(null), eq(Locale.ENGLISH));
        verifyPrivate(provider, times(1)).invoke("createResourceBundle", eq(null), eq(Locale.GERMAN));
    }

    private ComponentContext createComponentContext(Hashtable<String, Object> config) {
        final ComponentContext componentContext = PowerMockito.mock(ComponentContext.class);
        Mockito.when(componentContext.getBundleContext()).thenReturn(PowerMockito.mock(BundleContext.class));
        Mockito.when(componentContext.getProperties()).thenReturn(config);
        return componentContext;
    }
}