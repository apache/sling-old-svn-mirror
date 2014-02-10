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

import java.util.Dictionary;

import javax.jcr.Repository;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>AbstractSlingRepositoryManager</code> is the basis for controlling
 * the JCR repository instances used by Sling. As a manager it starts and stops
 * the actual repository instance, manages service registration and hands out
 * {@code SlingRepository} instances to be used by the consumers.
 * <p>
 * This base class controls the livecycle of repository instance whereas
 * implementations of this class provide actual integration into the runtime
 * context. The livecycle of the repository instance is defined as follows:
 * <p>
 * To start the repository instance, the implemetation calls the
 * {@link #start(BundleContext, String, boolean)}method which goes through the
 * steps of instantiating the repository, setting things up, and registering the
 * repository as an OSGi service:
 * <ol>
 * <li>{@link #acquireRepository()}</li>
 * <li>{@link #create(Bundle)}</li>
 * <li>{@link #setup(BundleContext, SlingRepository)}</li>
 * <li>{@link #registerService()}</li>
 * </ol>
 * <p>
 * To stop the repository instance, the implementation calls the {@link #stop()}
 * method which goes through the setps of unregistering the OSGi service,
 * tearing all special settings down and finally shutting down the repository:
 * <ol>
 * <li>{@link #unregisterService(ServiceRegistration)}</li>
 * <li>{@link #tearDown()}</li>
 * <li>{@link #destroy(AbstractSlingRepository2)}</li>
 * <li>{@link #disposeRepository(Repository)}</li>
 * </ol>
 * <p>
 * Instances of this class manage a single repository instance backing the OSGi
 * service instances. Each consuming bundle, though, gets its own service
 * instance backed by the single actual repository instance managed by this
 * class.
 *
 * @see AbstractSlingRepository2
 * @since API version 2.3 (bundle version 2.3)
 */
@ProviderType
public abstract class AbstractSlingRepositoryManager extends NamespaceMappingSupport {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private BundleContext bundleContext;

    private Repository repository;

    // the SlingRepository instance used to setup basic stuff
    // see setup and tearDown
    private AbstractSlingRepository2 masterSlingRepository;

    private ServiceRegistration repositoryService;

    private String defaultWorkspace;

    private boolean disableLoginAdministrative;

    /**
     * Returns the default workspace, which may be <code>null</code> meaning to
     * use the repository provided default workspace.
     *
     * @return the default workspace or {@code null} indicating the repository's
     *         default workspace is actually used.
     */
    public final String getDefaultWorkspace() {
        return defaultWorkspace;
    }

    /**
     * Returns whether to disable the
     * {@code SlingRepository.loginAdministrative} method or not.
     *
     * @return {@code true} if {@code SlingRepository.loginAdministrative} is
     *         disabled.
     */
    public final boolean isDisableLoginAdministrative() {
        return disableLoginAdministrative;
    }

    /**
     * Returns the {@code ServiceUserMapper} service to map the service name to
     * a service user name.
     * <p>
     * The {@code ServiceUserMapper} is used to implement the
     * {@link AbstractSlingRepository2#loginService(String, String)} method used
     * to replace the
     * {@link AbstractSlingRepository2#loginAdministrative(String)} method. If
     * this method returns {@code null} and hence the
     * {@code ServiceUserMapperService} is not available, the
     * {@code loginService} method is not able to login.
     *
     * @return The {@code ServiceUserMapper} service or {@code null} if not
     *         available.
     * @see AbstractSlingRepository2#loginService(String, String)
     */
    protected abstract ServiceUserMapper getServiceUserMapper();

    /**
     * Creates the backing JCR repository instances. It is expected for this
     * method to just start the repository.
     * <p>
     * This method does not throw any <code>Throwable</code> but instead just
     * returns <code>null</code> if not repository is available. Any problems
     * trying to acquire the repository must be caught and logged as
     * appropriate.
     *
     * @return The acquired JCR <code>Repository</code> or <code>null</code> if
     *         not repository can be acquired.
     * @see #start(BundleContext, String, boolean)
     */
    protected abstract Repository acquireRepository();

    /**
     * Registers this component as an OSGi service with the types provided by
     * the {@link #getServiceRegistrationInterfaces()} method and properties
     * provided by the {@link #getServiceRegistrationProperties()} method.
     * <p>
     * The repository is actually registered as an OSGi {@code ServiceFactory}
     * where the {@link #create(Bundle)} method is called to create an actual
     * {@link AbstractSlingRepository2} repository instance for a calling
     * (using) bundle. When the bundle is done using the repository instance,
     * the {@link #destroy(AbstractSlingRepository2)} method is called to clean
     * up.
     *
     * @return The OSGi <code>ServiceRegistration</code> object representing the
     *         registered service.
     * @see #start(BundleContext, String, boolean)
     * @see #getServiceRegistrationInterfaces()
     * @see #getServiceRegistrationProperties()
     * @see #create(Bundle)
     * @see #destroy(AbstractSlingRepository2)
     */
    protected final ServiceRegistration registerService() {
        final Dictionary<String, Object> props = getServiceRegistrationProperties();
        final String[] interfaces = getServiceRegistrationInterfaces();

        return bundleContext.registerService(interfaces, new ServiceFactory() {
            public Object getService(Bundle bundle, ServiceRegistration registration) {
                return AbstractSlingRepositoryManager.this.create(bundle);
            }

            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
                AbstractSlingRepositoryManager.this.destroy((AbstractSlingRepository2) service);
            }
        }, props);
    }

    /**
     * Return the service registration properties to be used to register the
     * repository service in {@link #registerService()}.
     *
     * @return The service registration properties to be used to register the
     *         repository service in {@link #registerService()}
     * @see #registerService()
     */
    protected abstract Dictionary<String, Object> getServiceRegistrationProperties();

    /**
     * Returns the service types to be used to register the repository service
     * in {@link #registerService()}. All interfaces returned must be accessible
     * to the class loader of the class of this instance.
     * <p>
     * This method may be overwritten to return additional types but the types
     * returned from this base implementation, {@code SlingRepository} and
     * {@code Repository}, must always be included.
     *
     * @return The service types to be used to register the repository service
     *         in {@link #registerService()}
     * @see #registerService()
     */
    protected String[] getServiceRegistrationInterfaces() {
        return new String[] {
            SlingRepository.class.getName(), Repository.class.getName()
        };
    }

    /**
     * Creates an instance of the {@link AbstractSlingRepository2}
     * implementation for use by the given {@code usingBundle}.
     * <p>
     * This method is called when the repository service is requested from
     * within the using bundle for the first time.
     * <p>
     * This method is expected to return a new instance on every call.
     *
     * @param usingBundle The bundle providing from which the repository is
     *            requested.
     * @return The {@link AbstractSlingRepository2} implementation instance to
     *         be used by the {@code usingBundle}.
     * @see #registerService()
     */
    protected abstract AbstractSlingRepository2 create(Bundle usingBundle);

    /**
     * Cleans up the given {@link AbstractSlingRepository2} instance previously
     * created by the {@link #create(Bundle)} method.
     *
     * @param repositoryServiceInstance The {@link AbstractSlingRepository2}
     *            istance to cleanup.
     * @see #registerService()
     */
    protected abstract void destroy(AbstractSlingRepository2 repositoryServiceInstance);

    /**
     * Returns the repository underlying this instance or <code>null</code> if
     * no repository is currently being available.
     *
     * @return The repository
     */
    protected final Repository getRepository() {
        return repository;
    }

    /**
     * Unregisters the service represented by the
     * <code>serviceRegistration</code>.
     *
     * @param serviceRegistration The service to unregister
     */
    protected final void unregisterService(ServiceRegistration serviceRegistration) {
        serviceRegistration.unregister();
    }

    /**
     * Disposes off the given <code>repository</code>.
     *
     * @param repository The repository to be disposed off which is the same as
     *            the one returned from {@link #acquireRepository()}.
     */
    protected abstract void disposeRepository(Repository repository);

    // --------- SCR integration -----------------------------------------------

    /**
     * This method actually starts the backing repository instannce and
     * registeres the repository service.
     * <p>
     * Multiple subsequent calls to this method without calling {@link #stop()}
     * first have no effect.
     *
     * @param bundleContext The {@code BundleContext} to register the repository
     *            service (and optionally more services required to operate the
     *            repository)
     * @param defaultWorkspace The name of the default workspace to use to
     *            login. This may be {@code null} to have the actual repository
     *            instance define its own default
     * @param disableLoginAdministrative Whether to disable the
     *            {@code SlingRepository.loginAdministrative} method or not.
     * @return {@code true} if the repository has been started and the service
     *         is registered.
     */
    protected final boolean start(final BundleContext bundleContext, final String defaultWorkspace,
            final boolean disableLoginAdministrative) {

        // already setup ?
        if (this.bundleContext != null) {
            log.debug("start: Repository already started and registered");
            return true;
        }

        this.bundleContext = bundleContext;
        this.defaultWorkspace = defaultWorkspace;
        this.disableLoginAdministrative = disableLoginAdministrative;

        try {
            log.debug("start: calling acquireRepository()");
            Repository newRepo = this.acquireRepository();
            if (newRepo != null) {

                // ensure we really have the repository
                log.debug("start: got a Repository");
                this.repository = newRepo;
                this.masterSlingRepository = this.create(this.bundleContext.getBundle());

                log.debug("start: setting up NamespaceMapping support");
                this.setup(this.bundleContext, this.masterSlingRepository);

                log.debug("start: calling registerService()");
                this.repositoryService = registerService();

                log.debug("start: registerService() successful, registration=" + repositoryService);
                return true;
            }
        } catch (Throwable t) {
            // consider an uncaught problem an error
            log.error("start: Uncaught Throwable trying to access Repository, calling stopRepository()", t);

            // repository might be partially started, stop anything left
            stop();
        }

        // fallback to failure to start the repository
        return false;
    }

    /**
     * This method must be called if overwritten by implementations !!
     */
    protected final void stop() {
        // ensure the repository is really disposed off
        if (repository != null || repositoryService != null) {
            log.info("stop: Repository still running, forcing shutdown");

            try {
                if (repositoryService != null) {
                    try {
                        log.debug("stop: Unregistering SlingRepository service, registration=" + repositoryService);
                        unregisterService(repositoryService);
                    } catch (Throwable t) {
                        log.info("stop: Uncaught problem unregistering the repository service", t);
                    }
                    repositoryService = null;
                }

                if (repository != null) {
                    Repository oldRepo = repository;
                    repository = null;

                    // stop namespace support
                    this.tearDown();
                    this.destroy(this.masterSlingRepository);

                    try {
                        disposeRepository(oldRepo);
                    } catch (Throwable t) {
                        log.info("stop: Uncaught problem disposing the repository", t);
                    }
                }
            } catch (Throwable t) {
                log.warn("stop: Unexpected problem stopping repository", t);
            }
        }

        this.repositoryService = null;
        this.repository = null;
        this.defaultWorkspace = null;
        this.bundleContext = null;
    }
}
