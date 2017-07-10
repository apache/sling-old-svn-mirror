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

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.security.auth.Subject;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AbstractSlingRepository2</code> is an abstract implementation of
 * the {@link SlingRepository} version 2.3 interface (phasing
 * {@link #loginAdministrative(String)} out in favor of
 * {@link #loginService(String, String)}) which provides default support for
 * attached repositories.
 * <p>
 * Implementations of the <code>SlingRepository</code> interface may wish to
 * extend this class to benefit from default implementations of most methods.
 * <p/>
 * To be able to know the calling bundle to implement the
 * {@link #loginService(String, String)} method the bundle using the repository
 * service has to be provided in the
 * {@link #AbstractSlingRepository2(AbstractSlingRepositoryManager, Bundle)
 * constructor}.
 * <p>
 * The premise of this abstract class is that an instance of an implementation
 * of this abstract class is handed out to each consumer of the
 * {@code SlingRepository} service. Each instance is generally based on the same
 * {@link #getRepository() delegated repository} instance and also makes use of
 * a single system wide {@link #getNamespaceAwareSession(Session) namespace
 * session support}.
 *
 * @see AbstractSlingRepositoryManager
 * @since API version 2.4 (bundle version 2.3)
 */
@ProviderType
public abstract class AbstractSlingRepository2 implements SlingRepository {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** The repository manager. */
    private final AbstractSlingRepositoryManager manager;

    /** The bundle using this repository instance. */
    private final Bundle usingBundle;

    /**
     * Sets up this abstract SlingRepository implementation.
     *
     * @param manager The {@link AbstractSlingRepositoryManager} controlling
     *            this instance as well as the actual JCR repository instance
     *            used by this.
     * @param usingBundle The bundle using this instance. This is used by the
     *            {@link #loginService(String, String)} method, which will not
     *            be able to login if this parameter is {@code null}
     */
    protected AbstractSlingRepository2(final AbstractSlingRepositoryManager manager, final Bundle usingBundle) {
        this.manager = manager;
        this.usingBundle = usingBundle;

        if(usingBundle == null) {
            throw new IllegalArgumentException("usingBundle is null");
        }
    }

    /**
     * @return The {@link AbstractSlingRepositoryManager} controlling this
     *         instances
     */
    protected final AbstractSlingRepositoryManager getSlingRepositoryManager() {
        return this.manager;
    }

    /**
     * Returns the actual repository to which all JCR Repository interface
     * methods implemented by this class are delegated.
     *
     * @return The delegated repository.
     */
    protected final Repository getRepository() {
        return this.getSlingRepositoryManager().getRepository();
    }

    /**
     * Returns the default workspace to login to if any of the {@code login} and
     * {@code createSession} methods is called without an explicit workspace
     * name.
     * <p>
     * This method may return {@code null} in which case the actual default
     * workspace used depends on the underlying JCR Repository implementation.
     */
    @Override
    public final String getDefaultWorkspace() {
        return this.getSlingRepositoryManager().getDefaultWorkspace();
    }

    /**
     * Creates an administrative session to access the indicated workspace.
     * <p>
     * This method is called by the {@link #loginAdministrative(String)} and
     * {@link #createServiceSession(String, String)} methods.
     *
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return An administrative session
     * @throws RepositoryException If a general error occurs during login
     * @see #createServiceSession(String, String)
     * @see #loginAdministrative(String)
     */
    protected abstract Session createAdministrativeSession(final String workspace) throws RepositoryException;


    /**
     * Creates a new service-session. This method is used by {@link #loginService(String, String)}
     * and {@link #impersonateFromService(String, Credentials, String)} to avoid
     * code duplication.
     *
     * @param usingBundle The bundle using a service session.
     * @param subServiceName The optional name of the subservice.
     * @param workspaceName The JCR workspace name or {@code null}.
     * @return A new service session or {@code null} if neither a user-name nor a
     *         principal names mapping has been defined.
     * @throws RepositoryException If an error occurs while creating the service session.
     */
    private Session createServiceSession(Bundle usingBundle, String subServiceName, String workspaceName) throws RepositoryException {
        final ServiceUserMapper serviceUserMapper = this.getSlingRepositoryManager().getServiceUserMapper();
        if (serviceUserMapper != null) {
            final Iterable<String> principalNames = serviceUserMapper.getServicePrincipalNames(usingBundle, subServiceName);
            if (principalNames != null) {
                return createServiceSession(principalNames, workspaceName);
            }

            final String userName = serviceUserMapper.getServiceUserID(usingBundle, subServiceName);
            if (userName != null) {
                return createServiceSession(userName, workspaceName);
            }
        }
        return null;
    }

    /**
     * Creates a service-session for the service's {@code serviceUserName} by
     * impersonating the user from an administrative session.
     * <p>
     * The administrative session is created calling
     * {@link #createAdministrativeSession(String)
     * createAdministrativeSession(workspace)} and is logged out before this
     * method returns.
     * <p>
     * Implementations of this class may overwrite this method with a better
     * implementation, notably one which does not involve a temporary creation
     * of an administrative session.
     *
     * @param serviceUserName The name of the user to create the session for
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return A session for the given user
     * @throws RepositoryException If a general error occurs while creating the
     *             session
     */
    protected Session createServiceSession(final String serviceUserName, final String workspace) throws RepositoryException {
        Session admin = null;
        try {
            admin = this.createAdministrativeSession(workspace);
            return admin.impersonate(new SimpleCredentials(serviceUserName, new char[0]));
        } finally {
            if (admin != null) {
                admin.logout();
            }
        }
    }

    /**
     * Creates a service-session for the service's {@code servicePrincipalNames}
     * using a pre-authenticated {@link Subject}.
     * <p>
     * Implementations of this class may overwrite this method to meet additional
     * needs wrt the the nature of the principals, the {@code Subject} or additional
     * attributes passed to the repository login.
     *
     * @param servicePrincipalNames The names of the service principals to create the session for
     * @param workspace The workspace to access or {@code null} to access the {@link #getDefaultWorkspace() default workspace}
     * @return A new service session
     * @throws RepositoryException If a general error occurs while creating the session.
     */
    protected Session createServiceSession(final Iterable<String> servicePrincipalNames, final String workspaceName) throws RepositoryException {
        Set<Principal> principals = new HashSet<>();
        for (final String pName : servicePrincipalNames) {
            if (pName != null && !pName.isEmpty()) {
                principals.add(new Principal() {
                    @Override
                    public String getName() {
                        return pName;
                    }
                });
            }
        };
        Subject subject = new Subject(true, principals, Collections.emptySet(), Collections.emptySet());
        try {
            return Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {
                @Override
                public Session run() throws Exception {
                    return AbstractSlingRepository2.this.getRepository().login(null, workspaceName);
                }
            }, null);
        } catch (PrivilegedActionException e) {
            throw new RepositoryException("failed to retrieve service session.", e);
        }
    }

    // login implementations (may be overwritten)

    /**
     * Same as calling {@code login(null, null)}.
     * <p>
     * This method may be overwritten.
     *
     * @return the result of calling {@link #login(Credentials, String)
     *         login(null, null)}.
     * @throws LoginException If login is not possible
     * @throws RepositoryException If another error occurrs during login
     * @see #login(Credentials, String)
     * @see #getNamespaceAwareSession(Session)
     */
    @Override
    public Session login() throws LoginException, RepositoryException {
        return this.login(null, null);
    }

    /**
     * Same as calling {@code login(credentials, null)}.
     * <p>
     * This method may be overwritten.
     *
     * @param credentials The {@code Credentials} to use to login.
     * @return the result of calling {@link #login(Credentials, String)
     *         login(credentials, null)}.
     * @throws LoginException If login is not possible
     * @throws RepositoryException If another error occurrs during login
     * @see #login(Credentials, String)
     * @see #getNamespaceAwareSession(Session)
     */
    @Override
    public Session login(final Credentials credentials) throws LoginException, RepositoryException {
        return this.login(credentials, null);
    }

    /**
     * Same as calling {@code login(null, workspace)}.
     * <p>
     * This method may be overwritten.
     *
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return the result of calling {@link #login(Credentials, String)
     *         login(null, workspace)}.
     * @throws LoginException If login is not possible
     * @throws RepositoryException If another error occurrs during login
     * @see #login(Credentials, String)
     * @see #getNamespaceAwareSession(Session)
     */
    @Override
    public Session login(final String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return this.login(null, workspace);
    }

    /**
     * Logs into the repository at the given {@code workspace} with the given
     * {@code credentials} and returns the session returned from
     * the repository.
     * <p>
     * This method logs in as a guest if {@code null} credentials are provided.
     * The method may be overwritten to implement a different behaviour as
     * indicated by the JCR specification for this method to use external
     * mechanisms to login instead of leveraging provided credentials.
     * <p>
     * This method may be overwritten.
     *
     * @param credentials The {@code Credentials} to use to login. If this is
     *            {@code null} JCR {@code GuestCredentials} are used to login.
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @throws LoginException If login is not possible
     * @throws NoSuchWorkspaceException if the desired workspace is not
     *             available
     * @throws RepositoryException If another error occurrs during login
     * @see #getNamespaceAwareSession(Session)
     */
    @Override
    public Session login(Credentials credentials, String workspace)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {

        if (credentials == null) {
            credentials = new GuestCredentials();
        }

        // check the workspace
        if (workspace == null) {
            workspace = this.getDefaultWorkspace();
        }

        try {
            logger.debug("login: Logging in to workspace '" + workspace + "'");
            final Repository repository = this.getRepository();
            if (this.getRepository() == null) {
                throw new RepositoryException("Sling Repository not ready");
            }

            final Session session = repository.login(credentials, workspace);
            return session;

        } catch (final RuntimeException re) {
            // SLING-702: Jackrabbit throws IllegalStateException if the
            // repository has already been shut down ...
            throw new RepositoryException(re.getMessage(), re);
        }
    }

    // service login

    /**
     * Actual implementation of the {@link #loginService(String, String)} method
     * taking into account the bundle calling this method.
     * <p/>
     * This method uses the
     * {@link AbstractSlingRepositoryManager#getServiceUserMapper()
     * ServiceUserMapper} service to map the named service to a user and then
     * calls the {@link #createServiceSession(String, String)} method actually
     * create a session for that user.
     *
     * @param subServiceName An optional subService identifier (may be
     *            {@code null})
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return A session authenticated with the service user
     * @throws LoginException if the service name cannot be derived or if
     *             logging is as the user to which the service name maps is not
     *             allowed
     * @throws RepositoryException If a general error occurs while creating the
     *             session
     */
    @Override
    public final Session loginService(final String subServiceName, final String workspace)
            throws LoginException, RepositoryException {
        Session s = createServiceSession(usingBundle, subServiceName, workspace);
        if (s != null) {
            return s;
        } else {
            throw new LoginException("Can neither derive user name nor principal names for bundle " + usingBundle + " and sub service " + subServiceName);
        }
    }

    /**
     * Default implementation of the {@link #impersonateFromService(Credentials, String, String)}
     * method taking into account the bundle calling this method.
     * <p/>
     * This method uses the
     * {@link AbstractSlingRepositoryManager#getServiceUserMapper()
     * ServiceUserMapper} service to map the named service to a user and then
     * calls the {@link #createServiceSession(String, String)} method actually
     * create a session for that user. This service session is then impersonated
     * to the subject identified by the specified {@code credentials}.
     *
     * @param subServiceName An optional subService identifier (may be {@code null})
     * @param credentials    A valid non-null {@code Credentials} object
     * @param workspaceName  The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return a new {@code Session} object
     * @throws LoginException If the current session does not have sufficient access to perform the operation.
     * @throws RepositoryException If another error occurs.
     * @since 2.4
     */
    @Override
    public Session impersonateFromService(final String subServiceName, final Credentials credentials, final String workspaceName)
            throws LoginException, RepositoryException {
        Session serviceSession = null;
        try {
            serviceSession = createServiceSession(usingBundle, subServiceName, workspaceName);
            if (serviceSession == null) {
                throw new LoginException("Cannot create service session for bundle " + usingBundle + " and sub service " + subServiceName);
            }
            return serviceSession.impersonate(credentials);
        } finally {
            if (serviceSession != null) {
                serviceSession.logout();
            }
        }
    }


    /**
     * Login as an administrative user. This method is deprecated and its use
     * can be completely disabled by setting {@code disableLoginAdministrative}
     * to {@code true}.
     * <p>
     * This implementation cannot be overwritten but, unless disabled, forwards
     * to the {@link #createAdministrativeSession(String)} method.
     *
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return An administrative session
     * @throws RepositoryException If the login fails or has been disabled
     */
    @Override
    public final Session loginAdministrative(final String workspace) throws RepositoryException {
        final boolean whitelisted = getSlingRepositoryManager().allowLoginAdministrativeForBundle(usingBundle);

        if(!whitelisted) {
            final String symbolicName = usingBundle.getSymbolicName();
            logger.error("Bundle {} is NOT whitelisted to use SlingRepository.loginAdministrative", symbolicName);
            throw new LoginException("Bundle " + symbolicName +" is NOT whitelisted");
        } else if (this.getSlingRepositoryManager().isDisableLoginAdministrative()) {
            logger.error("SlingRepository.loginAdministrative is disabled. Please use SlingRepository.loginService.");
            throw new LoginException("SlingRepository.loginAdministrative is disabled.");
        }

        logger.debug("SlingRepository.loginAdministrative is deprecated. Please use SlingRepository.loginService.");
        return createAdministrativeSession(workspace);
    }

    // Remaining Repository service methods all backed by the actual
    // repository instance. They may be overwritten, but generally are not.

    @Override
    public String getDescriptor(String name) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptor(name);
        }

        logger.error("getDescriptor: Repository not available");
        return null;
    }

    @Override
    public String[] getDescriptorKeys() {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorKeys();
        }

        logger.error("getDescriptorKeys: Repository not available");
        return new String[0];
    }

    @Override
    public Value getDescriptorValue(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorValue(key);
        }

        logger.error("getDescriptorValue: Repository not available");
        return null;
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorValues(key);
        }

        logger.error("getDescriptorValues: Repository not available");
        return null;
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.isSingleValueDescriptor(key);
        }

        logger.error("isSingleValueDescriptor: Repository not available");
        return false;
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.isStandardDescriptor(key);
        }

        logger.error("isStandardDescriptor: Repository not available");
        return false;
    }
}
