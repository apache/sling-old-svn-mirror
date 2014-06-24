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
package org.apache.sling.jcr.api;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>SlingRepository</code> extends the standard JCR repository
 * interface with two methods: {@link #getDefaultWorkspace()} and
 * {@link #loginAdministrative(String)}. This method ease the use of a JCR
 * repository in a Sling application in that the default (or standard) workspace
 * to use by the application may be configured and application bundles may use a
 * simple method to get an administrative session instead of being required to
 * provide their own configuration of administrative session details.
 * <p>
 * Implementations of this interface will generally provide configurability of
 * the default workspace name as well as the access details for the
 * administrative session.
 * <p>
 * Implementations of SlingRepository are expected to invoke any available
 * implementations of the {@link NamespaceMapper} interface <b>before</b>
 * returning <b>any</b> {@link Session} to callers. This includes the methods
 * defined in the {@link Repository} interface.
 */
@ProviderType
public interface SlingRepository extends Repository {

    /**
     * Returns the default workspace to use on login.
     *
     * @return null if the configured default workspace name is empty, SLING-256
     */
    String getDefaultWorkspace();

    /**
     * Returns a session to the given workspace which has administrative powers.
     * <p>
     * <b><i>NOTE: This method is intended for use by infrastructure bundles to
     * access the repository and provide general services. This method MUST not
     * be used to handle client requests of whatever kinds. To handle client
     * requests a regular authenticated session retrieved through
     * {@link #login(javax.jcr.Credentials, String)} or
     * {@link Session#impersonate(javax.jcr.Credentials)} must be used.</i></b>
     * <p>
     * This method is deprecated. Services running in the Sling system should
     * use the {@link #loginService(String serviceInfo, String workspace)}
     * method instead. Implementations of this method must throw
     * {@code javax.jcr.LoginException} if they don't support it.
     *
     * @param workspace The name of the workspace to which to get an
     *            administrative session. If <code>null</code> the
     *            {@link #getDefaultWorkspace()} default workspace is assumed.
     * @return The administrative Session
     * @throws LoginException If this method is not supported or is disabled by
     *             the implementation.
     * @throws RepositoryException If an error occurs creating the
     *             administrative session
     * @deprecated as of 2.2 (bundle version 2.2.0) because of inherent security
     *             issues. Services requiring specific permissions should use
     *             the {@link #loginService(String, String)} instead.
     */
    @Deprecated
    Session loginAdministrative(String workspace) throws LoginException, RepositoryException;

    /**
     * Returns a session to the given workspace with privileges assigned to the
     * service provided by the calling bundle. The {@code subServiceName}
     * argument can be used to further specialize the service account to be
     * used.
     *
     * @param subServiceName Optional Subservice Name to specialize account
     *            selection for the service. This may be {@code null}.
     * @param workspace The name of the workspace to which to get an
     *            administrative session. If <code>null</code> the
     *            {@link #getDefaultWorkspace()} default workspace is assumed.
     * @return A Session with appropriate permissions to execute the service.
     * @throws LoginException If there is no service account defined for the
     *             calling bundle or the defined service account does not exist.
     * @throws RepositoryException if an error occurs.
     * @since 2.2 (bundle version 2.2.0) to replace
     *        {@link #loginAdministrative(String)}
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/service-authentication.html">Service
     *      Authentication</a>
     */
    Session loginService(String subServiceName, String workspace) throws LoginException, RepositoryException;
}
