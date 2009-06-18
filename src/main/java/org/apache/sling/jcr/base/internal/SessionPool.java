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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Repository;
import javax.jcr.lock.Lock;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.jcr.api.TooManySessionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>SessionPool</code> class
 * implementins pooling and reusing sessions with the defined limits.
 * See {@link #acquireSession(SimpleCredentials, String)}
 * and {@link #acquireSession(Session, Credentials)} for details.
 *
 */
public class SessionPool {

    /**
     * The default upper limit of simultaneously active sessions created by this
     * instance (value is Integer.MAX_VALUE).
     *
     * @see #setMaxActiveSessions(int)
     * @see #getMaxActiveSessions()
     */
    public static final int DEFAULT_MAX_ACTIVE_SESSIONS = Integer.MAX_VALUE;

    /**
     * The default maximum time in seconds to wait for the number of active
     * sessions to drop below the maximum.
     *
     * @see #getMaxActiveSessionsWait()
     * @see #setMaxActiveSessionsWait(int)
     */
    public static final int DEFAULT_MAX_ACTIVE_SESSIONS_WAIT = 10;

    /**
     * The default upper limit for the number of idle sessions to keep in the
     * pool (valie is "10").
     *
     * @see #setMaxIdleSessions(int)
     * @see #getMaxIdleSessions()
     */
    public static final int DEFAULT_MAX_IDLE_SESSIONS = 10;

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SessionPool.class);

    private SessionPoolManager poolManager;

    /**
     * The name of the user for which this pool has been created.
     */
    private String userName;

    private int[] passData;

    /**
     * The session pool. Access to this object must be synchronized to prevent
     * corruption of the data structures.
     */
    private final LinkedList<Session> idleSessions;

    /**
     * Active sessions issued by this session pool.
     */
    protected final IdentityHashMap<PooledSession, Session> activeSessions;

    /**
     * The maximum number of active sessions for this mapping.
     */
    private int maxActiveSessions;

    /** The maximum number of idle sessions stored in the idleSessions */
    private int maxIdleSessions;

    /** The singal object used for maxActiveSession wait/notify */
    private final Object activeSessionLock;

    /**
     * The number of milliseconds to wait for the number of currently active
     * sessions from this pool to drop below the configured number of maximum
     * active sessions.
     *
     * @see #PROPERTY_MAX_ACTIVE_SESSIONS_WAIT
     * @see #DEFAULT_MAX_ACTIVE_SESSIONS_WAIT
     * @see #getMaxActiveSessionsWait()
     * @see #setMaxActiveSessionsWait(long)
     * @see #checkActiveSessions()
     */
    private long maxActiveWait;

    /**
     * The counter for the number of sessions which could be served from the
     * pool.
     */
    private int poolHitCounter;

    /**
     * The counter for the number of sessions which have to be acquired through
     * repository login (login or impersonation).
     */
    private int poolMissCounter;

    /**
     * The counter for the number of sessions which cannot be returned to the
     * pool. See the {@link #getPoolDropCounter()} method for reasons why
     * sessions might be dropped.
     */
    private int poolDropCounter;

    /**
     * Flag indicating whether this pool has already been disposed off. If this
     * is <code>true</code>, no sessions will be provided by this pool anymore.
     */
    private boolean disposed;

    /**
     * Creates a new instance of this class presetting internal counters
     * and data structures.
     *
     */
    public SessionPool(SessionPoolManager poolManager, SimpleCredentials credentials) {
        this.poolManager = poolManager;
        this.userName = credentials.getUserID();
        this.passData = this.getPassData(credentials);
        this.idleSessions = new LinkedList<Session>();
        this.activeSessions = new IdentityHashMap<PooledSession, Session>();
        this.activeSessionLock = new Object();
        this.clearCounters();

        // explicitly set the default value here, as the setConfig
        // will not set the default if the configured value is <= 0
        this.setMaxActiveSessionsWait(DEFAULT_MAX_ACTIVE_SESSIONS_WAIT);
    }

    /**
     * Disposes off this pool and logs out all idle sessions in the pool. As
     * the pool does not have any registry of non-idle sessions, those will
     * only be logged out if the sessions are requested to be logged out.
     */
    void dispose() {
        // stop providing sessions and force logging out released sessions
        this.disposed = true;

        synchronized (this.idleSessions) {
            // logout all sessions in the pool
            this.logoutSessions(this.idleSessions.iterator());
            this.idleSessions.clear();
        }

        synchronized (this.activeSessions) {
            // logout all sessions in the pool
            this.logoutSessions(this.activeSessions.values().iterator());
            this.activeSessions.clear();
        }
    }

    /**
     * Returns <code>true</code> if this pool has been disposed off by the
     * {@link #dispose()} method.
     */
    boolean isDisposed() {
        return this.disposed;
    }

    SessionPoolManager getPoolManager() {
        return this.poolManager;
    }

    /**
     * Returns a session providing access to the given ContentBus user to the
     * given <code>workSpace</code> of the given <code>repository</code>. The
     * user ID and password are mapped to the repository user ID and password
     * as configured for this instance.
     * <p>
     * In addition to just converting the credentials and returning the session,
     * this implementation has two additional features:
     * <ul>
     * <li>If the number of active session acquired through this instance has
     *  reached the maximum number of active sessions, the methods wait at most
     *  {@link #getMaxActiveSessionsWait()} seconds for sessions to be released.
     *  If no session becomes available a <code>RepositoryException</code> is
     *  thrown.
     * <li>Before really creating a session by calling the base class
     *  implementation of this method, the session pool is checked for an idle
     *  session to the desired <code>workspace</code>. If available, the idle
     *  session is returned.
     * </ul>
     * <p>
     * Only when the number of active sessions is below the maximum number of
     * active sessions and no session is available from the pool, the base class
     * implementation is called to actually login to the repository.
     * <p>
     * This method does not check, whether the user name from the
     * <code>cbCredentials</code> actually matches this instances pattern. The
     * method assumes, this has been done beforehand.
     *
     * @param credentials The <code>javax.jcr.Credentials</code> to authenticate
     *      with.
     * @param workSpace The name of the workspace to connect to.
     *
     * @return A session to the desired workspace to be used as the basis for
     *      the ticket of the given ContentBus user. The session returned is
     *      a pooled session, such that the session is put into the pool instead
     *      of being logged out, when not used anymore.
     *
     * @throws javax.jcr.LoginException If the login fails.
     * @throws TooManySessionsException If the maximum number of active
     *             sessions has been reached and no session was released while
     *             waiting for it.
     * @throws NoSuchWorkspaceException If the specified <code>workSpace</code>
     *      is not recognized.
     * @throws RepositoryException if another error occurs.
     * @throws IllegalStateException If this pool has already been disposed off.
     *
     * @see PooledSession
     */
    Session acquireSession(SimpleCredentials credentials, String workSpace)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {

        this.checkActiveSessions();

        // only try to get from the pool, if the password matches
        if (this.passDataMatch(credentials, this.passData)) {
            Session session = this.getFromPool(workSpace);
            if (session != null) {
                return session;
            }
        }

        // login new session (fails if password has changed but is not valid)
        Session session = this.poolManager.getRepository().login(credentials, workSpace);
        return this.createPooledSession(session);
    }

    /**
     * Returns a session providing access to the given ContentBus user to the
     * same workspace as the given <code>baseSession</code> by impersonating the
     * session as the new user. The <code>cqUserId</code> is mapped to the
     * repository user ID and password as configured for this instance.
     * <p>
     * In addition to just converting the user ID and returning the session,
     * this implementation has two additional features:
     * <ul>
     * <li>If the number of active session acquired through this instance has
     *  reached the maximum number of active sessions, the methods wait at most
     *  {@link #getMaxActiveSessionsWait()} seconds for sessions to be released.
     *  If no session becomes available a <code>RepositoryException</code> is
     *  thrown.
     * <li>Before really creating a session by calling the base class
     *  implementation of this method, the session pool is checked for an idle
     *  session to the desired <code>workspace</code>. If available, the idle
     *  session is returned.
     * </ul>
     * <p>
     * Only when the number of active sessions is below the maximum number of
     * active sessions and no session is available from the pool, the base class
     * implementation is called to actually login to the repository.
     * <p>
     * This method does not check, whether the <code>cqUserId</code> actually
     * matches this instances pattern. The method assumes, this has been done
     * beforehand.
     *
     * @param baseSession The repository session used for impersonation as the
     *      new user.
     * @param credentials The <code>javax.jcr.Credentials</code> to authenticate
     *      with.
     *
     * @return A session to the desired workspace to be used as the basis for
     *      the ticket of the given ContentBus user. The session returned is
     *      a pooled session, such that the session is put into the pool instead
     *      of being logged out, when not used anymore.
     *
     * @throws javax.jcr.LoginException if the <code>baseSession</code> does not
     *          have sufficient rights to impersonate.
     * @throws TooManySessionsException If the maximum number of active
     *             sessions has been reached and no session was released while
     *             waiting for it.
     * @throws RepositoryException If another error occurs.
     * @throws IllegalStateException If this pool has already been disposed off.
     *
     * @see PooledSession
     */
    Session acquireSession(Session baseSession, Credentials credentials)
            throws LoginException, RepositoryException {

        this.checkActiveSessions();

        Session session = this.getFromPool(baseSession.getWorkspace().getName());
        if (session != null) {
            return session;
        }

        session = baseSession.impersonate(credentials);
        return this.createPooledSession(session);
    }

    //---------- JMX ----------------------------------------------------------

    /**
     * Returns the name of user for which this pool has been created. This may
     * be <code>null</code> if the owner name of the pool is not known.
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Returns the maximum number of simultaneously active sessions.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     *
     * @see #DEFAULT_MAX_ACTIVE_SESSIONS
     * @see #setMaxActiveSessions(int)
     */
    public int getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    /**
     * Sets the maximum number of simultaneously active sessions.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     *
     * @param maxActiveSessions The new maximum number of active sessions. If
     *          less than or equal to zero, the default value (Integer.MAX_VALUE)
     *          is assumed.
     *
     * @see #DEFAULT_MAX_ACTIVE_SESSIONS
     * @see #getMaxActiveSessions()
     */
    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = (maxActiveSessions <= 0)
                ? DEFAULT_MAX_ACTIVE_SESSIONS
                : maxActiveSessions;
    }

    /**
     * Returns the maximum number of idle sessions to store in the pool.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     *
     * @see #DEFAULT_MAX_IDLE_SESSIONS
     * @see #getMaxIdleSessions()
     */
    public int getMaxIdleSessions() {
        return this.maxIdleSessions;
    }

    /**
     * Sets the maximum number of idle sessions to store in the pool.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     *
     * @param maxIdleSessions The new maximum number of idle sessions. If less
     *          than zero, the default value of 10 is assumed. If zero, session
     *          pooling will actually be disabled.
     *
     * @see #DEFAULT_MAX_IDLE_SESSIONS
     * @see #getMaxIdleSessions()
     */
    public void setMaxIdleSessions(int maxIdleSessions) {
        this.maxIdleSessions = (maxIdleSessions < 0)
                ? DEFAULT_MAX_IDLE_SESSIONS
                : maxIdleSessions;
    }

    /**
     * The number of seconds to wait for the number of active sessions to drop
     * below the configured maximum number of active sessions.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getMaxActiveSessionsWait() {
        return (int) (this.maxActiveWait / 1000L);
    }

    /**
     * Sets the number of seconds to wait for the number of active sessions to
     * drop below the configured maximum number of active sessions.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     *
     * @param maxActiveWait The number of seconds to wait. This method has no
     *          effect if this value is less than or equal to zero.
     *
     */
    public void setMaxActiveSessionsWait(int maxActiveWait) {
        if (maxActiveWait > 0) {
            this.maxActiveWait = 1000L * maxActiveWait;
        }
    }

    /**
     * Returns the number of currently active sessions.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getNumActiveSessions() {
        synchronized (this.activeSessions) {
            return this.activeSessions.size();
        }
    }

    /**
     * Returns the number of idle sessions stored in this pool.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getIdleSessions() {
        return this.idleSessions.size();
    }

    /**
     * Returns the number session acquisitions served from the pool.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getPoolHitCounter() {
        return this.poolHitCounter;
    }

    /**
     * Returns the number session acquisitions not served from the pool.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getPoolMissCounter() {
        return this.poolMissCounter;
    }

    /**
     * Returns the number session not added to the pool after release or dropped
     * while trying to retrieve a session from the pool. Dropping sessions may
     * have a number of reasons:
     * <ul>
     * <li>The session is not alive anymore
     * <li>The session should be taken from the pool but is attached to the
     *  wrong workspace.
     * <li>The maximum number of idle sessions in the pool has been reached.
     * </ul>
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public int getPoolDropCounter() {
        return this.poolDropCounter;
    }

    /**
     * Clears the session pool statistics counters.
     * <p>
     * This method is not part of the public API of this class and is present
     * solely for the purposes of JMX support.
     */
    public void clearCounters() {
        this.poolHitCounter = 0;
        this.poolMissCounter = 0;
        this.poolDropCounter = 0;
    }

    //---------- Support for PooledSession

    /**
     * Releases the repository session encapsulated by the {@link PooledSession}
     * to the pool or logs the session out if it is not live any more or the
     * maximum number of idle sessions in this pool has been reached.
     * <p>
     * This method also notifies any threads waiting for the number active
     * sessions to drop below the maximum number of sessions.
     * <p>
     * This method is called by the {@link PooledSession#logout()} method to
     * release the session.
     *
     * @param pooledSession The {@link PooledSession} to release.
     */
    void release(PooledSession pooledSession) {
        // remove the pooled session. If it is not in the activeSessions
        // map, it has been removed by one of the acquireSession methods
        // due to changed passwords. In this case, the delegate session
        // is not idled but immediately logged out
        boolean forcedLogout;
        synchronized (this.activeSessions) {
            forcedLogout = this.activeSessions.remove(pooledSession) == null;
        }

        synchronized (this.activeSessionLock) {
            this.activeSessionLock.notifyAll();
        }

        // unwrap the repository session
        Session session = pooledSession.getSession();

        // cache the session owner for messages and more
        String userId = session.getUserID();

        // if the session is to be logged out forcibly
        if (forcedLogout) {
            log.debug("Logging out session {}; password has changed", userId);
            session.logout();
            return;
        }

        // if the pool has been disposed off, the session will be logged out
        if (this.isDisposed()) {
            log.debug("Logging out session {}; pool has been disposed off",
                userId);
            session.logout();
            return;
        }

        // if the pool is full or the session is already dead, logout and return
        // this results in the session not being added to the pool
        if (this.idleSessions.size() >= this.getMaxIdleSessions() || !session.isLive()) {
            log.debug("Logging out session {}; pool is full or session is not alive",
                userId);
            this.poolDropCounter++;
            session.logout();
            return;
        }

        if (isSupported(session.getRepository(), Repository.OPTION_LOCKING_SUPPORTED)) {
            // if the session has locks, we logout the session and drop it,
            // as there is no easy way of finding the temporary locks and
            // unlocking them, we could use the search, however ???
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                // FIXME - this search searches for all locks for the user of the session
                //         so if the user has more than one session, locks from other
                //         sessions will be delivered as well.
                Query q = qm.createQuery(
                    "/jcr:root//element(*,mix:lockable)[@jcr:lockOwner='"
                        + session.getUserID() + "']", Query.XPATH);
                NodeIterator ni = q.execute().getNodes();
                while (ni.hasNext()) {
                    Node node = ni.nextNode();
                    String path = node.getPath();
                    try {
                        final Lock lock = node.getLock();
                        if (lock.getLockToken() == null) {
                            log.debug("Ignoring lock on {} held by {}, not held by this session",
                                path, userId);
                        } else if (lock.isSessionScoped()) {
                            log.info("Unlocking session-scoped lock on {} held by {}",
                                path, userId);
                            node.unlock();
                        } else {
                            log.warn("Dropping lock token of permanent lock on {} held by {}",
                                path, userId);
                            session.removeLockToken(lock.getLockToken());
                        }
                    } catch (RepositoryException re) {
                        log.debug("Ignoring lock on {} held by {}, not held by this session",
                                path, userId);
                    }
                }

                String[] lockTokens = session.getLockTokens();
                if (lockTokens != null && lockTokens.length > 0) {
                    log.warn("Session still has lock tokens !");
                    for (int i=0; i < lockTokens.length; i++) {
                        log.warn("Dropping lock token {} held by {}",
                            lockTokens[i], userId);
                        session.removeLockToken(lockTokens[i]);
                    }
                }
            } catch (RepositoryException re) {
                if ( log.isDebugEnabled() ) {
                    log.debug("Cannot cleanup lockes of session " + userId + ", logging out", re);
                } else {
                    log.info("Cannot cleanup lockes of session {}, logging out", userId);
                }
                this.poolDropCounter++;
                session.logout();
                return;
            }
        }


        if (isSupported(session.getRepository(), Repository.OPTION_OBSERVATION_SUPPORTED)) {
            // make sure the session has no more registered event listeners which
            // may be notified on changes, and - worse - prevent the objects from
            // being collected
            try {
                ObservationManager om = session.getWorkspace().getObservationManager();
                EventListenerIterator eli = om.getRegisteredEventListeners();
                if (eli.hasNext()) {
                    log.debug("Unregistering remaining EventListeners of {}", userId);
                    while (eli.hasNext()) {
                        EventListener el = (EventListener) eli.next();
                        om.removeEventListener(el);
                    }
                }
            } catch (RepositoryException re) {
                log.info("Cannot check or unregister event listeners of session " +
                    "{}, logging out", userId);
                this.poolDropCounter++;
                session.logout();
                return;
            }
        }

        // Otherwise clean up the session if there are any pending changes.
        // Those changes are not persisted, but dropped. If this fails, the
        // session is logged out and not returned/added to the pool
        try {
            if (session.hasPendingChanges()) {
                session.refresh(false);
            }
        } catch (RepositoryException re) {
            log.info("Cannot check or drop pending changes of session " +
                "{}, logging out", userId);
            this.poolDropCounter++;
            session.logout();
            return;
        }

        // now the session is "clean" and may be added to the pool
        log.debug("Returning session {} to the pool, now with {} entries",
            userId, new Integer(this.idleSessions.size()));

        // add to the pool and notify waiting session acquiry
        //    ==> checkActiveSessions()
        synchronized (this.idleSessions) {
            this.idleSessions.add(session);
        }
    }

    //---------- internal -----------------------------------------------------

    /**
     * Returns <code>true</code> if the given <code>repository</code> supports
     * the feature as indicated by the repository <code>descriptor</code>.
     *
     * @param repository the repository.
     * @param descriptor the name of a repository descriptor.
     * @return <code>true</code> if the repository supports the feature,
     *          <code>false</code> otherwise.
     */
    protected final boolean isSupported(Repository repository, String descriptor) {
        return "true".equals(repository.getDescriptor(descriptor));
    }

    /**
     * Wraps the repository session as a pooled session and increments the
     * counter of active sessions before returning the {@link PooledSession}.
     * <p>
     * This method knows about the JCR standard <code>Session</code>
     * interfaces and wraps the <code>delegatee</code> in a
     * {@link PooledSession}.
     *
     * @param delegatee The <code>Session</code> to wrap as a pooled session.
     * @see PooledSession
     */
    protected PooledSession createPooledSession(Session delegatee)
            throws RepositoryException {
        PooledSession pooledSession;
        if (delegatee instanceof JackrabbitSession) {
            pooledSession = new PooledJackrabbitSession(this,
                (JackrabbitSession) delegatee);
        } else {
            pooledSession = new PooledSession(this, delegatee);
        }

        // keep the pooled session
        synchronized (this.activeSessions) {
            this.activeSessions.put(pooledSession, delegatee);
        }

        return pooledSession;
    }

    /**
     * Checks the number of currently active sessions - sessions which have been
     * acquired but not yet released - against the maximum number of session
     * allowed as per the configuration. If the number is not reached yet, the
     * method silently returns.
     * <p>
     * If the maximum number of active sessions has been reached, this method
     * waits for at most 10 seconds for a session to be released. If after that
     * time or after a notification or an interrupt the number of sessions is
     * not below the maximum number of sessions, a
     * <code>RepositoryException</code> is thrown.
     *
     * @throws TooManySessionsException If the maximum number of active
     *             sessions has been reached and no session was released while
     *             waiting for it.
     * @throws IllegalStateException If this pool has already been disposed off.
     */
    private void checkActiveSessions() throws TooManySessionsException {
        // fail if diposed already
        if (this.isDisposed()) {
            throw new IllegalStateException("Pool has already been disposed off");
        }

        // check whether maxActiveSession is exhausted
        int maxActiveSessions = this.getMaxActiveSessions();
        if (this.getNumActiveSessions() < maxActiveSessions) {
            return;
        }

        // sessions exhausted, wait for sessions to be released
        try {
            synchronized (this.activeSessionLock) {
                this.activeSessionLock.wait(this.maxActiveWait);
            }
        } catch (InterruptedException ie) {
            log.debug("Interrupted while waiting for session to " +
                "become available");
        }

        // if the number of active sessions has still not dropped, fail
        if (this.getNumActiveSessions() >= maxActiveSessions) {
            throw new TooManySessionsException(this.getUserName());
        }
    }

    /**
     * Returns a session for the given workspace from the pool of sessions or
     * <code>null</code> if the pool is empty or has no sessions for the
     * given workspace.
     * <p>
     * Any sessions checked while looking for a valid - live session for the
     * correct workspace - session, which is not live or is attached to another
     * than the requested workspace is logged out and removed from the pool.
     * <p>
     * This is of course only a poor man's clean up mechanism which has at least
     * two flaws: (1) Sessions may remain in the pool for ever and (2) sessions
     * are removed from the pool which just do not qulaify for the desired
     * workspace but would qualify later. The reason for the second issue being
     * no problem at the moment, is that the JCR adapter is attached to a single
     * workspace and only during startup will the workspace name be different.
     *
     * @param workSpaceName The name of the workspace to which the sesion
     *          retrieved from the pool must be attached.
     *
     * @return A live session for the given workspace from the pool or
     *          <code>null</code> if no session for the workspace is available
     *          from the pool.
     */
    private Session getFromPool(String workSpaceName) throws RepositoryException {
        // check with pool
        for (;;) {

            // get a session from the, return if empty
            Session session;
            synchronized (this.idleSessions) {
                if (this.idleSessions.isEmpty()) {
                    log.debug("getFromPool: No idle session in pool");
                    this.poolMissCounter++;
                    return null;
                }

                // get the first entry from the pool
                session = this.idleSessions.removeFirst();
            }

            // check the session and the session's workspace
            if (session.isLive()
                    && session.getWorkspace().getName().equals(workSpaceName)) {
                this.poolHitCounter++;
                return this.createPooledSession(session);
            }

            // session is not alive anymore or has the wrong workspace name,
            // logout and try next from pool
            this.poolDropCounter++;
            session.logout();
        }
    }

    //---------- internal helper ----------------------------------------------

    private void logoutSessions(Iterator<Session> sessions) {
        // logout all sessions in the pool
        while (sessions.hasNext()) {
            Session session = sessions.next();
            try {
                if (session.isLive()) {
                    session.logout();
                }
            } catch (Exception e) {
                log.info("Unexpected problem logging out session " + session, e);
            }
        }
    }

    private int[] getPassData(SimpleCredentials credentials) {
        char[] passwd = credentials.getPassword();
        if (passwd == null) {
            return null;
        }

        int[] passdt = new int[passwd.length];
        for (int i=0; i < passdt.length; i++) {
            passdt[i] = passwd[i];
        }

        return passdt;
    }

    private boolean passDataMatch(SimpleCredentials credentials, int[] passdt) {
        char[] passwd = credentials.getPassword();
        if (passwd == null) {
            return passdt == null;
        } else if (passdt == null) {
            return false;
        }

        if (passwd.length != passdt.length) {
            return false;
        }

        for (int i=0; i < passwd.length; i++) {
            if (passwd[i] != passdt[i]) {
                return false;
            }
        }

        return true;
    }
}
