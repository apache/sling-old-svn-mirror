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

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.framework.Constants;

/**
 * The <code>JcrResourceProviderFactory</code> creates
 * resource providers based on JCR.
 */
@Component
@Service(value = ResourceProviderFactory.class )
@Properties({
    @Property(name=ResourceProviderFactory.PROPERTY_REQUIRED, boolValue=true),
    @Property(name=ResourceProvider.ROOTS, value="/"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling JCR Resource Provider Factory"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = QueriableResourceProvider.LANGUAGES, value = {Query.XPATH, Query.SQL, Query.JCR_SQL2})
})
public class JcrResourceProviderFactory implements ResourceProviderFactory {

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private SlingRepository repository;

    /** Get the dynamic class loader if available */
    ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if (dclm != null) {
            return dclm.getDynamicClassLoader();
        }
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProviderFactory#getResourceProvider(java.util.Map)
     */
    public ResourceProvider getResourceProvider(final Map<String, Object> authenticationInfo)
    throws LoginException {
        return getResourceProviderInternal(authenticationInfo, false);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProviderFactory#getAdministrativeResourceProvider(java.util.Map)
     */
    public ResourceProvider getAdministrativeResourceProvider(final Map<String, Object> authenticationInfo)
    throws LoginException {
        return getResourceProviderInternal(authenticationInfo, true);
    }

    /**
     * Create a new ResourceResolver wrapping a Session object. Carries map of
     * authentication info in order to create a new resolver as needed.
     */
    private ResourceProvider getResourceProviderInternal(
            final Map<String, Object> authenticationInfo, final boolean isAdmin)
            throws LoginException {

        // by default any session used by the resource resolver returned is
        // closed when the resource resolver is closed
        boolean logoutSession = true;

        // derive the session to be used
        Session session;
        try {
            final String workspace = getWorkspace(authenticationInfo);
            if (isAdmin) {
                // requested admin session to any workspace (or default)
                session = repository.loginAdministrative(workspace);

            } else {

                session = getSession(authenticationInfo);
                if (session == null) {
                    // requested non-admin session to any workspace (or default)
                    final Credentials credentials = getCredentials(authenticationInfo);
                    session = repository.login(credentials, workspace);

                } else if (workspace != null) {
                    // session provided by map; but requested a different
                    // workspace impersonate can only change the user not switch
                    // the workspace as a workaround we login to the requested
                    // workspace with admin and then switch to the provided
                    // session's user (if required)
                    Session tmpSession = null;
                    try {
                        tmpSession = repository.loginAdministrative(workspace);
                        if (tmpSession.getUserID().equals(session.getUserID())) {
                            session = tmpSession;
                            tmpSession = null;
                        } else {
                            session = tmpSession.impersonate(new SimpleCredentials(
                                session.getUserID(), new char[0]));
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

        return new JcrResourceProvider(session, this.getDynamicClassLoader(), logoutSession);
    }

    /**
     * Return the sudo user information.
     * If the sudo user info is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured sudo user information or <code>null</code>
     */
    private String getSudoUser(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object sudoObject = authenticationInfo.get(ResourceResolverFactory.USER_IMPERSONATION);
            if (sudoObject instanceof String) {
                return (String) sudoObject;
            }
        }
        return null;
    }

    /**
     * Return the workspace name.
     * If the workspace name is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured workspace name or <code>null</code>
     */
    private String getWorkspace(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object workspaceObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE);
            if (workspaceObject instanceof String) {
                return (String) workspaceObject;
            }
        }
        return null;
    }

    /**
     * Handle the sudo if configured. If the authentication info does not
     * contain a sudo info, this method simply returns the passed in session. If
     * a sudo user info is available, the session is tried to be impersonated.
     * The new impersonated session is returned. The original session is closed.
     * The session is also closed if the impersonation fails.
     *
     * @param session The session.
     * @param authenticationInfo The optional authentication info.
     * @param logoutSession whether to logout the <code>session</code> after
     *            impersonation or not.
     * @return The original session or impersonated session.
     * @throws LoginException If something goes wrong.
     */
    private Session handleImpersonation(final Session session,
            final Map<String, Object> authenticationInfo,
            final boolean logoutSession)
    throws LoginException {
        final String sudoUser = getSudoUser(authenticationInfo);
        if (sudoUser != null && !session.getUserID().equals(sudoUser)) {
            try {
                final SimpleCredentials creds = new SimpleCredentials(sudoUser,
                    new char[0]);
                copyAttributes(creds, authenticationInfo);
                creds.setAttribute(ResourceResolver.USER_IMPERSONATOR,
                    session.getUserID());
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
     * Create a login exception from a repository exception.
     * If the repository exception is a  {@link javax.jcr.LoginException}
     * a {@link LoginException} is created with the same information.
     * Otherwise a {@link LoginException} is created which wraps the
     * repository exception.
     * @param re The repository exception.
     * @return The login exception.
     */
    private LoginException getLoginException(final RepositoryException re) {
        if ( re instanceof javax.jcr.LoginException ) {
            return new LoginException(re.getMessage(), re.getCause());
        }
        return new LoginException("Unable to login " + re.getMessage(), re);
    }

    /**
     * Create a credentials object from the provided authentication info.
     * If no map is provided, <code>null</code> is returned.
     * If a map is provided and contains a credentials object, this object is
     * returned.
     * If a map is provided but does not contain a credentials object nor a
     * user, <code>null</code> is returned.
     * if a map is provided with a user name but without a credentials object
     * a new credentials object is created and all values from the authentication
     * info are added as attributes.
     * @param authenticationInfo Optional authentication info
     * @return A credentials object or <code>null</code>
     */
    private Credentials getCredentials(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo == null) {
            return null;
        }

        final Object credentialsObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);
        if (credentialsObject instanceof Credentials) {
            return (Credentials) credentialsObject;
        }

        // otherwise try to create SimpleCredentials if the userId is set
        final Object userId = authenticationInfo.get(ResourceResolverFactory.USER);
        if (userId instanceof String) {
            final Object password = authenticationInfo.get(ResourceResolverFactory.PASSWORD);
            final SimpleCredentials credentials = new SimpleCredentials(
                (String) userId, ((password instanceof char[])
                        ? (char[]) password
                        : new char[0]));

            // add attributes
            copyAttributes(credentials, authenticationInfo);

            return credentials;
        }

        // no user id (or not a String)
        return null;
    }

    /**
     * Copies the contents of the source map as attributes into the target
     * <code>SimpleCredentials</code> object with the exception of the
     * <code>user.jcr.credentials</code> and <code>user.password</code>
     * attributes to prevent leaking passwords into the JCR Session attributes
     * which might be used for break-in attempts.
     *
     * @param target The <code>SimpleCredentials</code> object whose attributes
     *            are to be augmented.
     * @param source The map whose entries (except the ones listed above) are
     *            copied as credentials attributes.
     */
    private void copyAttributes(final SimpleCredentials target,
            final Map<String, Object> source) {
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
     * @param name The name to check whether it is visible or not
     * @return <code>true</code> if the name is assumed visible
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    public static boolean isAttributeVisible(final String name) {
        return !name.equals(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS)
            && !name.contains("password");
    }

    /**
     * Returns the session provided as the user.jcr.session property of the
     * <code>authenticationInfo</code> map or <code>null</code> if the
     * property is not contained in the map or is not a <code>javax.jcr.Session</code>.
     * @param authenticationInfo Optional authentication info.
     * @return The user.jcr.session property or <code>null</code>
     */
    private Session getSession(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object sessionObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_SESSION);
            if (sessionObject instanceof Session) {
                return (Session) sessionObject;
            }
        }
        return null;
    }
}
