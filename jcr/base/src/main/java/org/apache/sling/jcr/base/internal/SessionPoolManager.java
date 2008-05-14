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
package org.apache.sling.jcr.base.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.sling.jcr.api.SlingRepository;

/**
 * The <code>SessionPoolManager</code> is an abstract implementation of the
 * {@link SlingRepository} interface which provides core support for session
 * pooling. Implementations of the <code>SlingRepository</code> interface may
 * wish to extend this class to benefit from a default implementation.
 * <p>
 * Extensions of this class will have to declare the following
 * <code>scr.property</code> tags to have them declared automatically in the
 * respective component and metatype definitions by the maven-sling-plugin:
 */
public class SessionPoolManager {

    private final Repository repository;

    private final Map<String, SessionPool> sessionPools;

    private final NamespaceMapper namespaceMapper;

    private final SessionPoolFactory sessionPoolFactory;

    public SessionPoolManager(Repository repository,
            NamespaceMapper mapper,
            SessionPoolFactory factory) {

        this.repository = repository;
        this.sessionPools = new HashMap<String, SessionPool>();
        this.sessionPoolFactory = factory;

        this.namespaceMapper = mapper;
    }

    public void dispose() {
        if (this.sessionPools != null) {
            for (Iterator<SessionPool> si = this.sessionPools.values().iterator(); si.hasNext();) {
                SessionPool pool = si.next();
                pool.dispose();
            }
            this.sessionPools.clear();
        }
    }

    Repository getRepository() {
        return this.repository;
    }

    /**
     * @param credentials
     * @param workspace
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws RepositoryException
     */
    public Session login(Credentials credentials, String workspace)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        Session result = null;

        // get the session pool for the credentials
        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials simple = (SimpleCredentials) credentials;
            SessionPool pool = this.getPool(simple);
            if (pool != null) {
                result = pool.acquireSession(simple, workspace);
            }
        }

        if ( result == null ) {
            // direct session, if no pool is available for the credentials
            result = this.getRepository().login(credentials, workspace);
        }
        if ( result != null && this.namespaceMapper != null ) {
            this.namespaceMapper.defineNamespacePrefixes(result);
        }
        return result;
    }

    Session impersonate(Session baseSession, Credentials credentials)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        Session result = null;

        // assert base session is live
        if (!baseSession.isLive()) {
            throw new RepositoryException(
                "Base Session is not alive, cannot impersonate");
        }

        if (credentials instanceof SimpleCredentials) {
            SessionPool pool = this.getPool((SimpleCredentials) credentials);
            if (pool != null) {
                result = pool.acquireSession(baseSession, credentials);
            }
        }

        if ( result == null ) {
            // no pool available for the credentials, use direct session
            result = baseSession.impersonate(credentials);
        }

        if ( result != null && this.namespaceMapper != null ) {
            this.namespaceMapper.defineNamespacePrefixes(result);
        }
        return result;
    }

    // ---------- Session Pooling ----------------------------------------------

    private SessionPool getPool(SimpleCredentials credentials) {
        String userName = credentials.getUserID();
        SessionPool pool = this.sessionPools.get(userName);
        if (pool == null) {
            pool = this.sessionPoolFactory.createPool(this, credentials);
            this.sessionPools.put(userName, pool);
        }

        return pool;
    }
}
