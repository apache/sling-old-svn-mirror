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

import javax.jcr.Session;

import org.apache.sling.api.resource.util.PathSet;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.junit.After;
import org.junit.Before;

/**
 * Test of JcrResourceListener.
 */
public class JcrResourceListenerTest extends AbstractListenerTest {

    private JcrResourceListener listener;

    private Session adminSession;

    @After
    public void tearDown() throws Exception {
        if ( adminSession != null ) {
            adminSession.logout();
            adminSession = null;
        }
        RepositoryUtil.stopRepository();
        if ( listener != null ) {
            listener.close();
            listener = null;
        }
    }

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        RepositoryUtil.startRepository();
        this.adminSession = RepositoryUtil.getRepository().loginAdministrative(null);
        RepositoryUtil.registerSlingNodeTypes(adminSession);
        this.listener = new JcrResourceListener(new ProviderContext() {
            @Override
            public ObservationReporter getObservationReporter() {
                return JcrResourceListenerTest.this.getObservationReporter();
            }

            @Override
            public PathSet getExcludedPaths() {
                return PathSet.fromPaths();
            }
        }, "/", new PathMapperImpl(), RepositoryUtil.getRepository());
    }

    @Override
    public SlingRepository getRepository() {
        return RepositoryUtil.getRepository();
    }
}
