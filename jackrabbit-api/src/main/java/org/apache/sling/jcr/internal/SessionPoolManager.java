/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.internal;

import java.util.Comparator;
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

    private Repository repository;

    private Map sessionPools;

    /**
     * The maximum number of active sessions per session pool, that is per user.
     */
    private int poolMaxActiveSessions;

    /** The maximum number of idle sessions stored in the sessionPool */
    private int poolMaxIdleSessions;

    /**
     * The number of seconds to wait for the number of currently active sessions
     * from this pool to drop below the configured number of maximum active
     * sessions.
     */
    private int poolMaxActiveSessionsWait;

    public SessionPoolManager(Repository repository, int maxActiveSessions,
            int maxActiveSessionsWait, int maxIdleSessions) {

        this.repository = repository;
        this.sessionPools = new HashMap();

        // default session pool configuration (actual values will be checked
        // for validity by the SessionPool instances themselves when
        // configuring)
        poolMaxActiveSessions = maxActiveSessions;
        poolMaxActiveSessionsWait = maxActiveSessionsWait;
        poolMaxIdleSessions = maxIdleSessions;
    }

    public void dispose() {
        if (sessionPools != null) {
            for (Iterator si = sessionPools.values().iterator(); si.hasNext();) {
                SessionPool pool = (SessionPool) si.next();
                pool.dispose();
            }
            sessionPools.clear();
        }
    }

    Repository getRepository() {
        return repository;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.jcr.SlingRepository#login(javax.jcr.Credentials,
     *      java.lang.String)
     */
    public Session login(Credentials credentials, String workspace)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        // get the session pool for the credentials
        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials simple = (SimpleCredentials) credentials;
            SessionPool pool = getPool(simple);
            if (pool != null) {
                return pool.acquireSession(simple, workspace);
            }
        }

        // direct session, if no pool is available for the credentials
        return getRepository().login(credentials, workspace);
    }

    Session impersonate(Session baseSession, Credentials credentials)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        // assert base session is live
        if (!baseSession.isLive()) {
            throw new RepositoryException(
                "Base Session is not alive, cannot impersonate");
        }

        if (credentials instanceof SimpleCredentials) {
            SessionPool pool = getPool((SimpleCredentials) credentials);
            if (pool != null) {
                return pool.acquireSession(baseSession, credentials);
            }
        }

        // no pool available for the credentials, use direct session
        return baseSession.impersonate(credentials);
    }

    // ---------- Session Pooling ----------------------------------------------

    private SessionPool getPool(SimpleCredentials credentials) {
        String userName = credentials.getUserID();
        SessionPool pool = (SessionPool) sessionPools.get(userName);
        if (pool == null) {
            // create and configure the new pool
            pool = new SessionPool(this, credentials);
            pool.setMaxActiveSessions(poolMaxActiveSessions);
            pool.setMaxActiveSessionsWait(poolMaxActiveSessionsWait);
            pool.setMaxIdleSessions(poolMaxIdleSessions);
            sessionPools.put(userName, pool);
        }

        return pool;
    }
    
    private class XYZ implements Comparator {
        
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Comparable) {
                return ((Comparable) o1).compareTo(o2);
            }
            
            if (o1 instanceof SimpleCredentials && o2 instanceof SimpleCredentials) {
                SimpleCredentials sc1 = (SimpleCredentials) o1;
                SimpleCredentials sc2 = (SimpleCredentials) o2;
                
                int res = sc1.getUserID().compareTo(sc2.getUserID());
                if (res != 0) {
                    return res;
                }
                
                return new String(sc1.getPassword()).compareTo(new String(sc2.getPassword()));
            }
            
            return o1.equals(o2) ? 0 : o1.hashCode() - o2.hashCode();
        }
    }
}
