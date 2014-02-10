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

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>AbstractSlingRepository2</code> is an abstract implementation of
 * the {@link SlingRepository} version 2.2 interface (phasing
 * {@link #loginAdministrative(String)} out in favor of
 * {@link #loginService(String, String)}) which provides default support for
 * attached repositories as well as namespace mapping support by wrapping
 * sessions with namespace support (see
 * {@link #getNamespaceAwareSession(Session)}).
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
 * @since API version 2.3 (bundle version 2.3)
 */
@ProviderType
public abstract class AbstractSlingRepository2 implements SlingRepository {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // the repository manager
    private final AbstractSlingRepositoryManager manager;

    // the bundle using this repository instance
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
    public final String getDefaultWorkspace() {
        return this.getSlingRepositoryManager().getDefaultWorkspace();
    }

    /**
     * Wraps the given session with support for name spaces defined by bundles
     * deployed in the OSGi framework. See {@link NamespaceMappingSupport} for
     * details about namespace support in Sling.
     * <p>
     * To fully support namespaces, this method must be called from each
     * implementation of any of the {@code login} methods implemented by this
     * class or its extensions.
     *
     * @param session The {@code Session} to wrap. This must not be {@code null}
     * @return The wrapped session
     * @throws RepositoryException If an error occurrs wrapping the session
     * @throws NullPointerException If {@code session} is {@code null}
     */
    protected final Session getNamespaceAwareSession(Session session) throws RepositoryException {
        return this.getSlingRepositoryManager().getNamespaceAwareSession(session);
    }

    /**
     * Creates an administrative session to access the indicated workspace.
     * <p>
     * This method is called by the {@link #loginAdministrative(String)} and
     * {@link #createServiceSession(String, String)} methods.
     * <p>
     * Implementations of this method must not call
     * {@link #getNamespaceAwareSession(Session)} as this is handled by the
     * calling method.
     *
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return An administrative session
     * @throws RepositoryException If a general error occurs during login
     * @see #createServiceSession(String, String)
     * @see #loginAdministrative(String)
     */
    protected abstract Session createAdministrativeSession(String workspace) throws RepositoryException;

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
     * <p>
     * This method must not call {@link #getNamespaceAwareSession(Session)} as
     * this is handled by the calling method.
     *
     * @param serviceUserName The name of the user to create the session for
     * @param workspace The workspace to access or {@code null} to access the
     *            {@link #getDefaultWorkspace() default workspace}
     * @return A session for the given user
     * @throws RepositoryException If a general error occurs while creating the
     *             session
     */
    protected Session createServiceSession(String serviceUserName, String workspace) throws RepositoryException {
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

    // login implementations (may be overwritten)

    /**
     * Same as calling {@code login(null, null)}.
     * <p>
     * This method may be overwritten. Care must be taken to call
     * {@link #getNamespaceAwareSession(Session)} before return to the caller.
     *
     * @return the result of calling {@link #login(Credentials, String)
     *         login(null, null)}.
     * @throws LoginException If login is not possible
     * @throws RepositoryException If another error occurrs during login
     * @see #login(Credentials, String)
     * @see #getNamespaceAwareSession(Session)
     */
    public Session login() throws LoginException, RepositoryException {
        return this.login(null, null);
    }

    /**
     * Same as calling {@code login(credentials, null)}.
     * <p>
     * This method may be overwritten. Care must be taken to call
     * {@link #getNamespaceAwareSession(Session)} before return to the caller.
     *
     * @param credentials The {@code Credentials} to use to login.
     * @return the result of calling {@link #login(Credentials, String)
     *         login(credentials, null)}.
     * @throws LoginException If login is not possible
     * @throws RepositoryException If another error occurrs during login
     * @see #login(Credentials, String)
     * @see #getNamespaceAwareSession(Session)
     */
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return this.login(credentials, null);
    }

    /**
     * Same as calling {@code login(null, workspace)}.
     * <p>
     * This method may be overwritten. Care must be taken to call
     * {@link #getNamespaceAwareSession(Session)} before return to the caller.
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
    public Session login(String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return this.login(null, workspace);
    }

    /**
     * Logs into the repository at the given {@code workspace} with the given
     * {@code credentials} and returns a namespace aware session by calling
     * {@link #getNamespaceAwareSession(Session)} with the session returned from
     * the repository..
     * <p>
     * This method logs in as a guest if {@code null} credentials are provided.
     * The method may be overwritten to implement a different behaviour as
     * indicated by the JCR specification for this method to use external
     * mechanisms to login instead of leveraging provided credentials. An
     * implementation overwriting this method must also call
     * {@link #getNamespaceAwareSession(Session)} before returning the session
     * to the caller.
     * <p>
     * This method may be overwritten. Care must be taken to call
     * {@link #getNamespaceAwareSession(Session)} before return to the caller.
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
    public Session login(Credentials credentials, String workspace) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        if (credentials == null) {
            credentials = new GuestCredentials();
        }

        // check the workspace
        if (workspace == null) {
            workspace = this.getDefaultWorkspace();
        }

        try {
            log.debug("login: Logging in to workspace '" + workspace + "'");
            final Repository repository = this.getRepository();
            if (this.getRepository() == null) {
                throw new RepositoryException("Sling Repository not ready");
            }

            Session session = repository.login(credentials, workspace);
            return this.getNamespaceAwareSession(session);

        } catch (RuntimeException re) {
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
    public final Session loginService(String subServiceName, String workspace) throws LoginException,
            RepositoryException {
        final ServiceUserMapper serviceUserMapper = this.getSlingRepositoryManager().getServiceUserMapper();
        final String userName = (serviceUserMapper != null) ? serviceUserMapper.getServiceUserID(this.usingBundle,
            subServiceName) : null;
        if (userName == null) {
            throw new LoginException("Cannot derive user name for bundle " + usingBundle + " and sub service "
                + subServiceName);
        }
        return getNamespaceAwareSession(createServiceSession(userName, workspace));
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
    public final Session loginAdministrative(String workspace) throws RepositoryException {
        if (this.getSlingRepositoryManager().isDisableLoginAdministrative()) {
            log.error("SlingRepository.loginAdministrative is disabled. Please use SlingRepository.loginService.");
            throw new LoginException();
        }

        log.debug("SlingRepository.loginAdministrative is deprecated. Please use SlingRepository.loginService.");
        return getNamespaceAwareSession(createAdministrativeSession(workspace));
    }

    // Remaining Repository service methods all backed by the actual
    // repository instance. They may be overwritten, but generally are not.

    public String getDescriptor(String name) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptor(name);
        }

        log.error("getDescriptor: Repository not available");
        return null;
    }

    public String[] getDescriptorKeys() {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorKeys();
        }

        log.error("getDescriptorKeys: Repository not available");
        return new String[0];
    }

    public Value getDescriptorValue(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorValue(key);
        }

        log.error("getDescriptorValue: Repository not available");
        return null;
    }

    public Value[] getDescriptorValues(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.getDescriptorValues(key);
        }

        log.error("getDescriptorValues: Repository not available");
        return null;
    }

    public boolean isSingleValueDescriptor(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.isSingleValueDescriptor(key);
        }

        log.error("isSingleValueDescriptor: Repository not available");
        return false;
    }

    public boolean isStandardDescriptor(String key) {
        Repository repo = getRepository();
        if (repo != null) {
            return repo.isStandardDescriptor(key);
        }

        log.error("isStandardDescriptor: Repository not available");
        return false;
    }

}
