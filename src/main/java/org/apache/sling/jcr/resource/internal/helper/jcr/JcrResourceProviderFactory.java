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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.JcrResourceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPOSITORY_REFERNENCE_NAME = "repository";

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference(name = REPOSITORY_REFERNENCE_NAME, referenceInterface = SlingRepository.class)
    private ServiceReference repositoryReference;

    private SlingRepository repository;

    /** The jcr resource listner. */
    private JcrResourceListener listener;

    @Activate
    protected void activate(final ComponentContext context) throws RepositoryException {

        SlingRepository repository = (SlingRepository) context.locateService(REPOSITORY_REFERNENCE_NAME,
            this.repositoryReference);
        if (repository == null) {
            // concurrent unregistration of SlingRepository service
            // don't care, this component is going to be deactivated
            // so we just stop working
            log.warn("activate: Activation failed because SlingRepository may have been unregistered concurrently");
            return;
        }

        final String root = PropertiesUtil.toString(context.getProperties().get(ResourceProvider.ROOTS), "/");

        this.repository = repository;
        this.listener = new JcrResourceListener(root, null, this.repository, context.getBundleContext());
    }

    @Deactivate
    protected void deactivate() {
        if ( this.listener != null ) {
            this.listener.deactivate();
            this.listener = null;
        }
    }

    /** Get the dynamic class loader if available */
    ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if (dclm != null) {
            return dclm.getDynamicClassLoader();
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void bindRepository(final ServiceReference ref) {
        this.repositoryReference = ref;
        this.repository = null; // make sure ...
    }

    @SuppressWarnings("unused")
    private void unbindRepository(final ServiceReference ref) {
        if (this.repositoryReference == ref) {
            this.repositoryReference = null;
            this.repository = null; // make sure ...
        }
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
            Map<String, Object> authenticationInfo, final boolean isAdmin)
            throws LoginException {

        // Make sure authenticationInfo is not null
        if (authenticationInfo == null) {
            authenticationInfo = Collections.emptyMap();
        }

        // by default any session used by the resource resolver returned is
        // closed when the resource resolver is closed
        boolean logoutSession = true;
        RepositoryHolder holder = new RepositoryHolder();

        // derive the session to be used
        Session session;
        try {
            final String workspace = getWorkspace(authenticationInfo);
            if (isAdmin) {

                /*
                 * This implements the deprecated getAdministrativeResourceResolver
                 * method which is implemented in terms of the deprecated
                 * SlingRepository.loginAdministrative(String) method. Either
                 * one can fail, so it is ok to use the deprecated method
                 * here instead of a properly configured call to
                 * SlingRepository.loginService(String, String)
                 */

                // requested admin session to any workspace (or default)
                session = repository.loginAdministrative(workspace);

            } else {

                session = getSession(authenticationInfo);
                if (session == null) {

                    final Object serviceBundleObject = authenticationInfo.get(SERVICE_BUNDLE);
                    if (serviceBundleObject instanceof Bundle) {

                        final String subServiceName = (authenticationInfo.get(ResourceResolverFactory.SUBSERVICE) instanceof String)
                                ? (String) authenticationInfo.get(ResourceResolverFactory.SUBSERVICE)
                                : null;

                        final BundleContext bc = ((Bundle) serviceBundleObject).getBundleContext();

                        final SlingRepository repo = (SlingRepository) bc.getService(repositoryReference);
                        if (repo == null) {
                            log.warn(
                                "getResourceProviderInternal: Cannot login service because cannot get SlingRepository on behalf of bundle {} ({})",
                                bc.getBundle().getSymbolicName(), bc.getBundle().getBundleId());
                            throw new LoginException(); // TODO: correct ??
                        }

                        try {
                            session = repo.loginService(subServiceName, workspace);
                            holder.setRepositoryReference(bc, repositoryReference);
                            holder.setSession(session);
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
                         * method, this bundle could be configured with an appropriate
                         * user for service authentication and do:
                         *     tmpSession = repository.loginService(null, workspace);
                         * For now, we keep loginAdministrative
                         */

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

        if (logoutSession) {
            holder.setSession(session);
        }

        return new JcrResourceProvider(session, this.getDynamicClassLoader(), holder);
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
     * Return the sudo user information.
     * If the sudo user info is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Authentication info (not {@code null}).
     * @return The configured sudo user information or <code>null</code>
     */
    private String getSudoUser(final Map<String, Object> authenticationInfo) {
        final Object sudoObject = authenticationInfo.get(ResourceResolverFactory.USER_IMPERSONATION);
        if (sudoObject instanceof String) {
            return (String) sudoObject;
        }
        return null;
    }

    /**
     * Return the workspace name.
     * If the workspace name is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Authentication info (not {@code null}).
     * @return The configured workspace name or <code>null</code>
     */
    private String getWorkspace(final Map<String, Object> authenticationInfo) {
        final Object workspaceObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE);
        if (workspaceObject instanceof String) {
            return (String) workspaceObject;
        }
        return null;
    }

    /**
     * Returns the session provided as the user.jcr.session property of the
     * <code>authenticationInfo</code> map or <code>null</code> if the
     * property is not contained in the map or is not a <code>javax.jcr.Session</code>.
     * @param authenticationInfo Authentication info (not {@code null}).
     * @return The user.jcr.session property or <code>null</code>
     */
    private Session getSession(final Map<String, Object> authenticationInfo) {
        final Object sessionObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_SESSION);
        if (sessionObject instanceof Session) {
            return (Session) sessionObject;
        }
        return null;
    }
}
