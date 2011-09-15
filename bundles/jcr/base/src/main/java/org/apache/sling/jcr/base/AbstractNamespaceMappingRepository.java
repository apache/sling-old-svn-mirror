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
package org.apache.sling.jcr.base;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.internal.loader.Loader;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The <code>AbstractNamespaceMappingRepository</code> is an abstract implementation of
 * the {@link SlingRepository} interface which provides default support for
 * namespace mapping.
 */
public abstract class AbstractNamespaceMappingRepository implements SlingRepository {

    /** Namespace handler. */
    private Loader namespaceHandler;

    /** Session proxy handler. */
    private SessionProxyHandler sessionProxyHandler;

    private ServiceTracker namespaceMapperTracker;

    protected void setup(final BundleContext bundleContext) {
        this.namespaceMapperTracker = new ServiceTracker(bundleContext, NamespaceMapper.class.getName(), null);
        this.namespaceMapperTracker.open();
        this.namespaceHandler = new Loader(this, bundleContext);
        this.sessionProxyHandler = new SessionProxyHandler(this);
    }

    protected void tearDown() {
        if ( this.namespaceMapperTracker != null ) {
            this.namespaceMapperTracker.close();
            this.namespaceMapperTracker = null;
        }
        if (this.namespaceHandler != null) {
            this.namespaceHandler.dispose();
            this.namespaceHandler = null;
        }
        this.sessionProxyHandler = null;
    }

    void defineNamespacePrefixes(final Session session) throws RepositoryException {
        final Loader localHandler = this.namespaceHandler;
        if (localHandler != null) {
            // apply namespace mapping
            localHandler.defineNamespacePrefixes(session);
        }

        if (namespaceMapperTracker != null) {
            // call namespace mappers
            final Object[] nsMappers = namespaceMapperTracker.getServices();
            if (nsMappers != null) {
                for (int i = 0; i < nsMappers.length; i++) {
                    ((NamespaceMapper) nsMappers[i]).defineNamespacePrefixes(session);
                }
            }
        }
    }

    /**
     * Return a namespace aware session.
     */
    protected Session getNamespaceAwareSession(final Session session) throws RepositoryException {
        if ( session == null ) {  // sanity check
            return null;
        }
        defineNamespacePrefixes(session);

        // to support namespace prefixes if session.impersonate is called
        // we have to use a proxy
        final SessionProxyHandler localHandler = this.sessionProxyHandler;
        if ( localHandler != null ) {
            return localHandler.createProxy(session);
        }
        return session;
    }
}
