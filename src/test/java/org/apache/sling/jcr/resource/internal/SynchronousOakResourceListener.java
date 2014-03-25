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

import java.io.IOException;
import java.util.concurrent.Executor;

import javax.jcr.RepositoryException;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class is used to ensure that events are handled during the test.
 *
 * TODO - Ideally, this wouldn't be necessary, but EventHelper doesn't seem
 * to be working 100% of the time.
 *
 */
public class SynchronousOakResourceListener extends OakResourceListener {

    public SynchronousOakResourceListener(
            final SlingRepository repo,
            final BundleContext bundleContext,
            final ResourceResolver resolver,
            final ServiceTracker tracker,
            final Executor executor)
            throws LoginException, RepositoryException, NoSuchFieldException {
        super("/", new ObservationListenerSupport(bundleContext, repo), bundleContext, executor);
        PrivateAccessor.setField(this.support, "resourceResolver", resolver);
        PrivateAccessor.setField(this.support, "eventAdminTracker", tracker);
    }

    public void dispose() {
        try {
            this.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
