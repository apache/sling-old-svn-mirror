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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The <code>SessionProvider</code> extends the standard JCR Repository
 * interface with two methods: {@link #getDefaultWorkspace()} and
 * {@link #loginAdministrative(String)}. This method ease the use of a JCR
 * repository in a Sling Application in that the default (or standard) workspace
 * to use by the application may be configured and application bundles may use a
 * simple method to get an administrative session instead of being required to
 * provide their own configuration of administrative session details.
 * <p>
 * Implementations of this interface will generally provide configurability of
 * the default workspace name as well as the access details for the
 * administrative session.
 */
public interface SlingRepository extends Repository {

    /**
     * Returns the default workspace to use on login.
     * 
     * @return null if the configured default workspace name is empty, SLING-256
     */
    String getDefaultWorkspace();

    /**
     * Returns a session to the default workspace which has administrative
     * powsers.
     * <p>
     * <b><i>NOTE: This method is intended for use by infrastructure bundles to
     * access the repository and provide general services. This method MUST not
     * be used to handle client requests of whatever kinds. To handle client
     * requests a regular authenticated session retrieved
     * through {@link #login(javax.jcr.Credentials, String)} or
     * {@link Session#impersonate(javax.jcr.Credentials)} must be used.</i></b>
     *
     * @param workspace The name of the workspace to which to get an
     *            administrative session. If <code>null</code> the
     *            {@link #getDefaultWorkspace()} default workspace is assumed.
     */
    Session loginAdministrative(String workspace) throws RepositoryException;
}
