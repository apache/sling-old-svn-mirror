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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.io.Closeable;
import java.io.IOException;

import javax.jcr.Session;

import org.apache.sling.jcr.resource.internal.HelperData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

class JcrProviderState implements Closeable {

    private final Session session;

    private final BundleContext bundleContext;

    private final ServiceReference repositoryRef;

    private final boolean logout;

    private final JcrItemResourceFactory resourceFactory;

    private final HelperData helperData;

    JcrProviderState(Session session, HelperData helperData, boolean logout) {
        this(session, helperData, logout, null, null);
    }

    JcrProviderState(Session session, HelperData helperData, boolean logout, BundleContext bundleContext, ServiceReference repositoryRef) {
        this.session = session;
        this.bundleContext = bundleContext;
        this.repositoryRef = repositoryRef;
        this.logout = logout;
        this.helperData = helperData;
        this.resourceFactory = new JcrItemResourceFactory(session, helperData);
    }

    Session getSession() {
        return session;
    }

    JcrItemResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    HelperData getHelperData() {
        return helperData;
    }

    @Override
    public void close() throws IOException {
        logout();
    }

    void logout() {
        if (logout) {
            session.logout();
        }
        if (bundleContext != null) {
            try {
                bundleContext.ungetService(repositoryRef);
            } catch ( final IllegalStateException ise ) {
                // this might happen on shutdown / updates (bundle is invalid)
                // we can ignore this.
            }
        }
    }
}
