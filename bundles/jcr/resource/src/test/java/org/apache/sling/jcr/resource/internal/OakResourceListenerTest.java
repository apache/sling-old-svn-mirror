/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal;

import static org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardUtils.registerObserver;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jcr.Repository;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.sling.api.resource.util.PathSet;
import org.apache.sling.commons.testing.jcr.RepositoryUtil.RepositoryWrapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.junit.After;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Test of OakResourceListener.
 */
public class OakResourceListenerTest extends AbstractListenerTest {

    private OakResourceListener listener;
    private ExecutorService executor;
    private Whiteboard whiteboard;
    private SlingRepository slingRepository;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        final Oak oak = new Oak(new SegmentNodeStore());
        this.whiteboard = oak.getWhiteboard();
        final Repository repository = new Jcr(oak).createRepository();
        this.slingRepository = new RepositoryWrapper(repository);

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.registerService(any(String.class), any(Object.class), any(Dictionary.class)))
                .thenAnswer(new Answer<ServiceRegistration>() {
                    public ServiceRegistration answer(InvocationOnMock invocation) throws Throwable {
                        Object[] arguments = invocation.getArguments();
                        if (arguments[1] instanceof Observer) {
                            registerObserver(whiteboard, (Observer) arguments[1]);
                        }
                        return mock(ServiceRegistration.class);
                    }
                });
        listener = new OakResourceListener("/", new ProviderContext() {
            @Override
            public ObservationReporter getObservationReporter() {
                return OakResourceListenerTest.this.getObservationReporter();
            }

            @Override
            public PathSet getExcludedPaths() {
                return PathSet.fromPaths();
            }
        }, bundleContext, executor, new PathMapperImpl(), 1000, slingRepository); 
    }

    @After
    public void tearDown() throws Exception {
        listener.close();
        executor.shutdown();
    }

    @Override
    public SlingRepository getRepository() {
        return slingRepository;
    }
}
