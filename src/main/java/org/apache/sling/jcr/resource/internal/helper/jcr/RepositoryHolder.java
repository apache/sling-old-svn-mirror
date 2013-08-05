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

import javax.jcr.Session;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

class RepositoryHolder {

    private Session session;

    private BundleContext bundleContext;

    private ServiceReference repositoryReference;

    RepositoryHolder() {
    }

    public void setSession(final Session session) {
        this.session = session;
    }

    public void setRepositoryReference(final BundleContext bundleContext, final ServiceReference repositoryReference) {
        this.bundleContext = bundleContext;
        this.repositoryReference = repositoryReference;
    }

    void release() {
        if (this.session != null) {
            this.session.logout();
            this.session = null;
        }

        if (this.repositoryReference != null) {
            this.bundleContext.ungetService(this.repositoryReference);
        }
    }
}
