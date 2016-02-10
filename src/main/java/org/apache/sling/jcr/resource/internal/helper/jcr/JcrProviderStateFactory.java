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

import static org.apache.sling.api.resource.ResourceResolverFactory.NEW_PASSWORD;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrProviderStateFactory {

    private final Logger logger = LoggerFactory.getLogger(JcrProviderStateFactory.class);

    private final ServiceReference repositoryReference;

    private final SlingRepository repository;

    private final DynamicClassLoaderManager dynamicClassLoaderManager;

    private final PathMapper pathMapper;

    public JcrProviderStateFactory(final ServiceReference repositoryReference,
            final SlingRepository repository,
            final DynamicClassLoaderManager dynamicClassLoaderManager,
            final PathMapper pathMapper) {
        this.repository = repository;
        this.repositoryReference = repositoryReference;
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
        this.pathMapper = pathMapper;
    }

    @SuppressWarnings("deprecation")
    JcrProviderState createProviderState(final @Nonnull Map<String, Object> authenticationInfo) throws LoginException {
        // by default any session used by the resource resolver returned is
        // closed when the resource resolver is closed
        boolean logoutSession = true;

        // derive the session to be used
        Session session;
        BundleContext bc = null;
        try {
            final String workspace = getWorkspace(authenticationInfo);

            if (Boolean.TRUE.equals(authenticationInfo.get(ResourceProvider.AUTH_ADMIN))) {
                session = repository.loginAdministrative(workspace);
            } else {
                session = getSession(authenticationInfo);
                if (session == null) {

                    final Object serviceBundleObject = authenticationInfo.get(ResourceProvider.AUTH_SERVICE_BUNDLE);
                    if (serviceBundleObject instanceof Bundle) {

                        final String subServiceName = (authenticationInfo
                                .get(ResourceResolverFactory.SUBSERVICE) instanceof String)
                                        ? (String) authenticationInfo.get(ResourceResolverFactory.SUBSERVICE) : null;

                        bc = ((Bundle) serviceBundleObject).getBundleContext();

                        final SlingRepository repo = (SlingRepository) bc.getService(repositoryReference);
                        if (repo == null) {
                            logger.warn(
                                    "getResourceProviderInternal: Cannot login service because cannot get SlingRepository on behalf of bundle {} ({})",
                                    bc.getBundle().getSymbolicName(), bc.getBundle().getBundleId());
                            throw new LoginException(); // TODO: correct ??
                        }

                        try {
                            session = repo.loginService(subServiceName, workspace);
                        } finally {
                            // unget the repository if the service cannot
                            // login to it, otherwise the repository service
                            // is let go off when the resource resolver is
                            // closed and the session logged out
                            if (session == null) {
                                bc.ungetService(repositoryReference);
                            }
                        }

                    } else {

                        // requested non-admin session to any workspace (or
                        // default)
                        final Credentials credentials = getCredentials(authenticationInfo);
                        session = repository.login(credentials, workspace);

                    }

                } else if (workspace != null) {

                    // session provided by map; but requested a different
                    // workspace impersonate can only change the user not switch
                    // the workspace as a workaround we login to the requested
                    // workspace with admin and then switch to the provided
                    // session's user (if required)
                    Session tmpSession = null;
                    try {

                        /*
                         * TODO: Instead of using the deprecated loginAdministrative
                         * method, this bundle could be configured with an
                         * appropriate user for service authentication and do:
                         * tmpSession = repository.loginService(null, workspace);
                         * For now, we keep loginAdministrative
                         */

                        tmpSession = repository.loginAdministrative(workspace);
                        if (tmpSession.getUserID().equals(session.getUserID())) {
                            session = tmpSession;
                            tmpSession = null;
                        } else {
                            session = tmpSession.impersonate(new SimpleCredentials(session.getUserID(), new char[0]));
                        }
                    } finally {
                        if (tmpSession != null) {
                            tmpSession.logout();
                        }
                    }

                } else {

                    // session provided; no special workspace; just make sure
                    // the session is not logged out when the resolver is closed
                    logoutSession = false;
                }
            }
        } catch (final RepositoryException re) {
            throw getLoginException(re);
        }

        session = handleImpersonation(session, authenticationInfo, logoutSession);

        final HelperData data = new HelperData(this.dynamicClassLoaderManager.getDynamicClassLoader(), this.pathMapper);
        if (bc == null) {
            return new JcrProviderState(session, data, logoutSession);
        } else {
            return new JcrProviderState(session, data, logoutSession, bc, repositoryReference);
        }
    }

    /**
     * Handle the sudo if configured. If the authentication info does not
     * contain a sudo info, this method simply returns the passed in session. If
     * a sudo user info is available, the session is tried to be impersonated.
     * The new impersonated session is returned. The original session is closed.
     * The session is also closed if the impersonation fails.
     *
     * @param session
     *            The session.
     * @param authenticationInfo
     *            The optional authentication info.
     * @param logoutSession
     *            whether to logout the <code>session</code> after impersonation
     *            or not.
     * @return The original session or impersonated session.
     * @throws LoginException
     *             If something goes wrong.
     */
    private static Session handleImpersonation(final Session session, final Map<String, Object> authenticationInfo,
            final boolean logoutSession) throws LoginException {
        final String sudoUser = getSudoUser(authenticationInfo);
        if (sudoUser != null && !session.getUserID().equals(sudoUser)) {
            try {
                final SimpleCredentials creds = new SimpleCredentials(sudoUser, new char[0]);
                copyAttributes(creds, authenticationInfo);
                creds.setAttribute(ResourceResolver.USER_IMPERSONATOR, session.getUserID());
                return session.impersonate(creds);
            } catch (final RepositoryException re) {
                throw getLoginException(re);
            } finally {
                if (logoutSession) {
                    session.logout();
                }
            }
        }
        return session;
    }

    /**
     * Create a login exception from a repository exception. If the repository
     * exception is a {@link javax.jcr.LoginException} a {@link LoginException}
     * is created with the same information. Otherwise a {@link LoginException}
     * is created which wraps the repository exception.
     *
     * @param re
     *            The repository exception.
     * @return The login exception.
     */
    private static LoginException getLoginException(final RepositoryException re) {
        if (re instanceof javax.jcr.LoginException) {
            return new LoginException(re.getMessage(), re.getCause());
        }
        return new LoginException("Unable to login " + re.getMessage(), re);
    }

    /**
     * Create a credentials object from the provided authentication info. If no
     * map is provided, <code>null</code> is returned. If a map is provided and
     * contains a credentials object, this object is returned. If a map is
     * provided but does not contain a credentials object nor a user,
     * <code>null</code> is returned. if a map is provided with a user name but
     * without a credentials object a new credentials object is created and all
     * values from the authentication info are added as attributes.
     *
     * @param authenticationInfo
     *            Optional authentication info
     * @return A credentials object or <code>null</code>
     */
    private static Credentials getCredentials(final Map<String, Object> authenticationInfo) {

        Credentials creds = null;
        if (authenticationInfo != null) {

            final Object credentialsObject = authenticationInfo
                    .get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);

            if (credentialsObject instanceof Credentials) {
                creds = (Credentials) credentialsObject;
            } else {
                // otherwise try to create SimpleCredentials if the userId is
                // set
                final Object userId = authenticationInfo.get(ResourceResolverFactory.USER);
                if (userId instanceof String) {
                    final Object password = authenticationInfo.get(ResourceResolverFactory.PASSWORD);
                    final SimpleCredentials credentials = new SimpleCredentials((String) userId,
                            ((password instanceof char[]) ? (char[]) password : new char[0]));

                    // add attributes
                    copyAttributes(credentials, authenticationInfo);

                    creds = credentials;
                }
            }
        }

        if (creds instanceof SimpleCredentials && authenticationInfo.get(NEW_PASSWORD) instanceof String) {
            ((SimpleCredentials) creds).setAttribute(NEW_PASSWORD, authenticationInfo.get(NEW_PASSWORD));
        }

        return creds;
    }

    /**
     * Copies the contents of the source map as attributes into the target
     * <code>SimpleCredentials</code> object with the exception of the
     * <code>user.jcr.credentials</code> and <code>user.password</code>
     * attributes to prevent leaking passwords into the JCR Session attributes
     * which might be used for break-in attempts.
     *
     * @param target
     *            The <code>SimpleCredentials</code> object whose attributes are
     *            to be augmented.
     * @param source
     *            The map whose entries (except the ones listed above) are
     *            copied as credentials attributes.
     */
    private static void copyAttributes(final SimpleCredentials target, final Map<String, Object> source) {
        final Iterator<Map.Entry<String, Object>> i = source.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Object> current = i.next();
            if (isAttributeVisible(current.getKey())) {
                target.setAttribute(current.getKey(), current.getValue());
            }
        }
    }

    /**
     * Returns <code>true</code> unless the name is
     * <code>user.jcr.credentials</code> (
     * {@link JcrResourceConstants#AUTHENTICATION_INFO_CREDENTIALS}) or contains
     * the string <code>password</code> as in <code>user.password</code> (
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#PASSWORD})
     *
     * @param name
     *            The name to check whether it is visible or not
     * @return <code>true</code> if the name is assumed visible
     * @throws NullPointerException
     *             if <code>name</code> is <code>null</code>
     */
    private static boolean isAttributeVisible(final String name) {
        return !name.equals(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS) && !name.contains("password");
    }

    /**
     * Return the sudo user information. If the sudo user info is provided, it
     * is returned, otherwise <code>null</code> is returned.
     *
     * @param authenticationInfo
     *            Authentication info (not {@code null}).
     * @return The configured sudo user information or <code>null</code>
     */
    private static String getSudoUser(final Map<String, Object> authenticationInfo) {
        final Object sudoObject = authenticationInfo.get(ResourceResolverFactory.USER_IMPERSONATION);
        if (sudoObject instanceof String) {
            return (String) sudoObject;
        }
        return null;
    }

    /**
     * Return the workspace name. If the workspace name is provided, it is
     * returned, otherwise <code>null</code> is returned.
     *
     * @param authenticationInfo
     *            Authentication info (not {@code null}).
     * @return The configured workspace name or <code>null</code>
     */
    private static String getWorkspace(final Map<String, Object> authenticationInfo) {
        final Object workspaceObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE);
        if (workspaceObject instanceof String) {
            return (String) workspaceObject;
        }
        return null;
    }

    /**
     * Returns the session provided as the user.jcr.session property of the
     * <code>authenticationInfo</code> map or <code>null</code> if the property
     * is not contained in the map or is not a <code>javax.jcr.Session</code>.
     *
     * @param authenticationInfo
     *            Authentication info (not {@code null}).
     * @return The user.jcr.session property or <code>null</code>
     */
    private static Session getSession(final Map<String, Object> authenticationInfo) {
        final Object sessionObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_SESSION);
        if (sessionObject instanceof Session) {
            return (Session) sessionObject;
        }
        return null;
    }

}
