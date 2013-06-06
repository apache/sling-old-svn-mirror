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
package org.apache.sling.discovery.impl.setup;

import java.util.Dictionary;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.settings.SlingSettingsService;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.VoidAction;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class MockFactory {

    public final Mockery context = new JUnit4Mockery();

    public static ResourceResolverFactory mockResourceResolverFactory()
            throws Exception {
    	return mockResourceResolverFactory(null);
    }
    
    public static ResourceResolverFactory mockResourceResolverFactory(final Repository repositoryOrNull)
            throws Exception {
        Mockery context = new JUnit4Mockery();

        final ResourceResolverFactory resourceResolverFactory = context
                .mock(ResourceResolverFactory.class);
        // final ResourceResolver resourceResolver = new MockResourceResolver();
        // final ResourceResolver resourceResolver = new
        // MockedResourceResolver();

        context.checking(new Expectations() {
            {
                allowing(resourceResolverFactory)
                        .getAdministrativeResourceResolver(null);
                will(new Action() {

                    public Object invoke(Invocation invocation)
                            throws Throwable {
                    	return new MockedResourceResolver(repositoryOrNull);
                    }

                    public void describeTo(Description arg0) {
                        arg0.appendText("whateva - im going to create a new mockedresourceresolver");
                    }
                });
            }
        });
        return resourceResolverFactory;
    }

    public static SlingSettingsService mockSlingSettingsService(
            final String slingId) {
        Mockery context = new JUnit4Mockery();

        final SlingSettingsService settingsService = context
                .mock(SlingSettingsService.class);
        context.checking(new Expectations() {
            {
                allowing(settingsService).getSlingId();
                will(returnValue(slingId));
            }
        });
        return settingsService;
    }

    public static ComponentContext mockComponentContext() {
        Mockery context = new JUnit4Mockery();
        final BundleContext bc = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                allowing(bc).registerService(with(any(String.class)),
                        with(any(Object.class)), with(any(Dictionary.class)));
                will(VoidAction.INSTANCE);
            }
        });

        final ComponentContext cc = context.mock(ComponentContext.class);
        context.checking(new Expectations() {
            {
                allowing(cc).getProperties();
                will(returnValue(new Properties()));

                allowing(cc).getBundleContext();
                will(returnValue(bc));
            }
        });

        return cc;
    }
}
