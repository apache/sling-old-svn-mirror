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
import org.apache.sling.jcr.base.SessionProxyHandler.SessionProxyInvocationHandler;
import org.apache.sling.jcr.base.internal.loader.Loader;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>NamespaceMappingSupport</code> is an abstract base class for
 * implementing support for dynamic namespace mapping in {@code SlingRepository}
 * instances.
 *
 * @see AbstractSlingRepositoryManager
 * @since API version 2.3 (bundle version 2.3)
 */
@ProviderType
public abstract class NamespaceMappingSupport {

    /** Namespace handler. */
    private Loader namespaceHandler;

    /** Session proxy handler. */
    private SessionProxyHandler sessionProxyHandler;

    /**
     * Returns the {@code NamespaceMapper} services used by the
     * {@link #getNamespaceAwareSession(Session)} method to define custom
     * namespaces on sessions.
     *
     * @return the {@code NamespaceMapper} services or {@code null} if there are
     *         none.
     */
    protected abstract NamespaceMapper[] getNamespaceMapperServices();

    private SessionProxyHandler getSessionProxyHandler() {
        return this.sessionProxyHandler;
    }

    private Loader getLoader() {
        return this.namespaceHandler;
    }

    /**
     * Sets up the namespace mapping support. This method is called by
     * implementations of this class after having started (or acquired) the
     * backing JCR repository instance.
     * <p>
     * This method may be overwritten but must be called from overwriting
     * methods.
     *
     * @param bundleContext The OSGi {@code BundleContext} to access the
     *            framework for namespacing setup
     * @param repository The {@code SlingRepository} to register namespaces on
     */
    protected void setup(final BundleContext bundleContext, final SlingRepository repository) {
        this.sessionProxyHandler = new SessionProxyHandler(this);
        this.namespaceHandler = new Loader(repository, bundleContext);
    }

    /**
     * Terminates namespace mapping support. This method is called by the
     * implementations of this class before stopping (or letting go of) the
     * backing JCR repository instance.
     * <p>
     * This method may be overwritten but must be called from overwriting
     * methods.
     */
    protected void tearDown() {
        if (this.namespaceHandler != null) {
            this.namespaceHandler.dispose();
            this.namespaceHandler = null;
        }
        this.sessionProxyHandler = null;
    }

    /**
     * Helper method to dynamically define namespaces on the given session
     * <p>
     * This method is package private to allow to be accessed from the
     * {@link SessionProxyInvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])}
     * method.
     *
     * @param session The JCR {@code Session} to define name spaces on
     * @throws RepositoryException if an error occurrs defining the name spaces
     */
    final void defineNamespacePrefixes(final Session session) throws RepositoryException {
        final Loader localHandler = this.getLoader();
        if (localHandler != null) {
            // apply namespace mapping
            localHandler.defineNamespacePrefixes(session);
        }

        // call namespace mappers
        final NamespaceMapper[] nsMappers = getNamespaceMapperServices();
        if (nsMappers != null) {
            for (int i = 0; i < nsMappers.length; i++) {
                nsMappers[i].defineNamespacePrefixes(session);
            }
        }
    }

    /**
     * Return a namespace aware session.
     * <p>
     * This method must be called for each JCR {@code Session} to be returned
     * from any of the repository {@code login} methods which are expected to
     * support dynamically mapped namespaces.
     *
     * @param session The session convert into a namespace aware session
     * @return The namespace aware session
     * @throws RepositoryException If an error occurrs making the session
     *             namespace aware
     */
    protected final Session getNamespaceAwareSession(final Session session) throws RepositoryException {
        if (session == null) { // sanity check
            return null;
        }
        defineNamespacePrefixes(session);

        // to support namespace prefixes if session.impersonate is called
        // we have to use a proxy
        final SessionProxyHandler localHandler = this.getSessionProxyHandler();
        if (localHandler != null) {
            return localHandler.createProxy(session);
        }
        return session;
    }
}
