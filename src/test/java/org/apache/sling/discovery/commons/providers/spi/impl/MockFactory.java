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
package org.apache.sling.discovery.commons.providers.spi.impl;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;

public class MockFactory {

    public final Mockery context = new JUnit4Mockery();

    public static ResourceResolverFactory mockResourceResolverFactory()
            throws Exception {
    	return mockResourceResolverFactory(null);
    }

    public static ResourceResolverFactory mockResourceResolverFactory(final SlingRepository repositoryOrNull)
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

    public static void resetRepo() throws Exception {
        Session l = RepositoryProvider.instance().getRepository()
                .loginAdministrative(null);
        try {
            l.removeItem("/var");
            l.save();
            l.logout();
        } catch (Exception e) {
            l.refresh(false);
            l.logout();
        }
    }
    
}
