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
package org.apache.sling.jcr;

import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.sling.jcr.internal.SessionPoolManager;
import org.apache.sling.jcr.internal.loader.Loader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;


/**
 * The <code>AbstractSlingRepository</code> is an abstract implementation of the
 * {@link SlingRepository} interface which provides core support for session
 * pooling. Implementations of the <code>SlingRepository</code> interface may
 * wish to extend this class to benefit from a default implementation.
 * <p>
 * Extensions of this class will have to declare the following
 * <code>scr.property</code> tags to have them declared automatically in the
 * respective component and metatype definitions by the maven-sling-plugin:
 *
 * <pre>
 *  scr.property value=&quot;default&quot; name=&quot;defaultWorkspace&quot;
 *  scr.property value=&quot;anonymous&quot; name=&quot;anonymous.name&quot;
 *  scr.property value=&quot;anonymous&quot; name=&quot;anonymous.password&quot;
 *  scr.property value=&quot;admin&quot; name=&quot;admin.name&quot;
 *  scr.property value=&quot;admin&quot; name=&quot;admin.password&quot;
 *  scr.property value=&quot;10&quot; type=&quot;Integer&quot; name=&quot;pool.maxActive&quot;
 *  scr.property value=&quot;10&quot; type=&quot;Integer&quot; name=&quot;pool.maxIdle&quot;
 *  scr.property value=&quot;256&quot; type=&quot;Integer&quot; name=&quot;pool.maxActiveWait&quot;
 * </pre>
 */
public abstract class AbstractSlingRepository
    implements SlingRepository, SynchronousBundleListener {

    public static final String PROPERTY_DEFAULT_WORKSPACE = "defaultWorkspace";

    public static final String PROPERTY_ANONYMOUS_USER = "anonymous.name";

    public static final String PROPERTY_ANONYMOUS_PASS = "anonymous.password";

    public static final String PROPERTY_ADMIN_USER = "admin.name";

    public static final String PROPERTY_ADMIN_PASS = "admin.password";

    public static final String DEFAULT_WORKSPACE = "default";

    public static final String DEFAULT_ANONYMOUS_USER = "anonymous";

    public static final String DEFAULT_ANONYMOUS_PASS = "anonymous";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASS = "admin";

    /**
     * The name of the configuration parameter containing the maximum number of
     * seconds to wait for the number of currently active sessions to drop be
     * low the upper limit before giving up (value is "pool.maxActiveWait").
     */
    public static final String PARAM_MAX_ACTIVE_SESSIONS_WAIT = "pool.maxActiveWait";

    /**
     * The name of the configuration parameter containing the upper limit of the
     * simultaneously active sessions (value is "pool.maxActive").
     */
    public static final String PARAM_MAX_ACTIVE_SESSIONS = "pool.maxActive";

    /**
     * The name of the configuration parameter containing the upper limit of the
     * currently idle sessions to keep in the pool (value is "pool.maxIdle").
     */
    public static final String PARAM_MAX_IDLE_SESSIONS = "pool.maxIdle";

    private ComponentContext componentContext;

    private String defaultWorkspace;

    private String anonUser;

    private char[] anonPass;

    private String adminUser;

    private char[] adminPass;

    private SessionPoolManager poolManager;

    private Loader loader;

    protected AbstractSlingRepository() {
    }

    protected abstract Repository getDelegatee() throws RepositoryException;

    protected final SessionPoolManager getPoolManager()
            throws RepositoryException {
        if (this.poolManager == null) {
            Dictionary properties = this.componentContext.getProperties();
            int maxActiveSessions = this.getIntProperty(properties,
                PARAM_MAX_ACTIVE_SESSIONS);
            int maxIdleSessions = this.getIntProperty(properties,
                PARAM_MAX_IDLE_SESSIONS);
            int maxActiveSessionsWait = this.getIntProperty(properties,
                PARAM_MAX_ACTIVE_SESSIONS_WAIT);

            this.poolManager = new SessionPoolManager(this.getDelegatee(), this.loader,
                maxActiveSessions, maxActiveSessionsWait, maxIdleSessions);
        }

        return this.poolManager;
    }

    /**
     * @see org.apache.sling.jcr.SlingRepository#getDefaultWorkspace()
     */
    public String getDefaultWorkspace() {
        return this.defaultWorkspace;
    }

    /**
     * Logs in as an anonymous user. This implementation simply returns the
     * result of calling {@link #login(Credentials, String)}
     */
    public Session login() throws LoginException, RepositoryException {
        return this.login(null, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.jcr.SessionProvider#getAdministrationSession()
     */
    public Session loginAdministrative(String workspace)
            throws RepositoryException {
        SimpleCredentials sc = new SimpleCredentials(this.adminUser, this.adminPass);
        return this.login(sc, workspace);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public Session login(Credentials credentials) throws LoginException,
            RepositoryException {
        return this.login(credentials, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public Session login(String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return this.login(null, workspace);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.jcr.SlingRepository#login(javax.jcr.Credentials,
     *      java.lang.String)
     */
    public Session login(Credentials credentials, String workspace)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        // if already stopped, don't retrieve a session
        if (this.componentContext == null || this.getDelegatee() == null) {
            throw new RepositoryException("Sling Repository not ready");
        }

        if (credentials == null) {
            credentials = new SimpleCredentials(this.anonUser, this.anonPass);
        }

        // check the workspace
        if (workspace == null) {
            workspace = this.getDefaultWorkspace();
        }

        try {
            return this.getPoolManager().login(credentials, workspace);
        } catch (NoSuchWorkspaceException nswe) {
            // if the desired workspace is the default workspace, try to create
            if (workspace.equals(this.getDefaultWorkspace())
                && this.createWorkspace(workspace)) {
                return this.getPoolManager().login(credentials, workspace);
            }

            // otherwise (any workspace) or if workspace creation fails
            // just forward the original exception
            throw nswe;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor(String name) {
        try {
            return this.getDelegatee().getDescriptor(name);
        } catch (RepositoryException re) {
            this.log(LogService.LOG_ERROR, "Repository not available", re);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        try {
            return this.getDelegatee().getDescriptorKeys();
        } catch (RepositoryException re) {
            this.log(LogService.LOG_ERROR, "Repository not available", re);
        }

        return new String[0];
    }

    // ---------- logging ------------------------------------------------------

    protected abstract LogService getLog();

    protected void log(int level, String message) {
        this.log(level, message, null);
    }

    protected void log(int level, String message, Throwable t) {
        LogService log = this.getLog();
        if (log != null) {
            log.log(this.componentContext.getServiceReference(), level, message, t);
        }
    }

    // --------- SCR integration -----------------------------------------------

    protected ComponentContext getComponentContext() {
        return this.componentContext;
    }

    /**
     * This method must be called if overwritten by implementations !!
     * @throws nothing, but allow derived classes to throw any Exception
     */
    protected void activate(ComponentContext componentContext) throws Exception {
        this.componentContext = componentContext;

        Dictionary properties = componentContext.getProperties();
        this.defaultWorkspace = this.getProperty(properties, PROPERTY_DEFAULT_WORKSPACE,
            DEFAULT_WORKSPACE);

        this.anonUser = this.getProperty(properties, PROPERTY_ANONYMOUS_USER,
            DEFAULT_ANONYMOUS_USER);
        this.anonPass = this.getProperty(properties, PROPERTY_ANONYMOUS_PASS,
            DEFAULT_ANONYMOUS_PASS).toCharArray();

        this.adminUser = this.getProperty(properties, PROPERTY_ADMIN_USER,
            DEFAULT_ADMIN_USER);
        this.adminPass = this.getProperty(properties, PROPERTY_ADMIN_PASS,
            DEFAULT_ADMIN_PASS).toCharArray();

        this.loader = new Loader(this);

        componentContext.getBundleContext().addBundleListener(this);

        // TODO: Consider running this in the background !!
        Bundle[] bundles = componentContext.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                // load content for bundles which are neither INSTALLED nor
                // UNINSTALLED
                this.loader.registerBundle(bundles[i]);
            }
        }
    }

    /**
     * This method must be called if overwritten by implementations !!
     *
     * @param componentContext
     */
    protected void deactivate(ComponentContext componentContext) {

        componentContext.getBundleContext().removeBundleListener(this);

        if (this.poolManager != null) {
            this.poolManager.dispose();
            this.poolManager = null;
        }
        if ( this.loader != null ) {
            this.loader.dispose();
            this.loader = null;
        }

        this.componentContext = null;
    }

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {
        // TODO: This is synchronous - take care to not block the system !!
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                // register content and types when the bundle content is
                // available
                this.loader.registerBundle(event.getBundle());
                break;

            case BundleEvent.UNINSTALLED:
                this.loader.unregisterBundle(event.getBundle());
                break;
            case BundleEvent.UPDATED:
                this.loader.updateBundle(event.getBundle());
        }
    }

    // ---------- internal -----------------------------------------------------

    private String getProperty(Dictionary properties, String name,
            String defaultValue) {
        Object prop = properties.get(name);
        return (prop instanceof String) ? (String) prop : defaultValue;
    }

    private int getIntProperty(Dictionary properties, String name) {
        Object prop = properties.get(name);
        if (prop instanceof Number) {
            return ((Number) prop).intValue();
        } else if (prop != null) {
            try {
                return Integer.decode(String.valueOf(prop)).intValue();
            } catch (NumberFormatException nfe) {
                // don't really care
            }
        }

        return -1;
    }

    private boolean createWorkspace(String workspace) {
        this.log(LogService.LOG_INFO, "login: Requested workspace " + workspace
            + " does not exist, trying to create");

        Session tmpSession = null;
        try {
            SimpleCredentials sc = new SimpleCredentials(this.adminUser, this.adminPass);
            tmpSession = this.getDelegatee().login(sc);
            Workspace defaultWs = tmpSession.getWorkspace();
            if (defaultWs instanceof JackrabbitWorkspace) {
                ((JackrabbitWorkspace) defaultWs).createWorkspace(workspace);
                return true;
            }

            // not Jackrabbit
            this.log(LogService.LOG_ERROR, "login: Cannot create requested workspace "
                + workspace + ": Jackrabbit is required");
        } catch (Throwable t) {
            this.log(LogService.LOG_ERROR, "login: Cannot create requested workspace "
                + workspace, t);
        } finally {
            if (tmpSession != null) {
                tmpSession.logout();
            }
        }

        // fall back to failure
        return false;
    }
}
