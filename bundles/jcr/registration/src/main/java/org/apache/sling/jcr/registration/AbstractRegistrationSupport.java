/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.registration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Repository;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * The <code>AbstractRegistrationSupport</code> class is the base class for
 * registration purposes of embedded repositories.
 * <p>
 * This base class cares for synchronization issues of the
 * {@link #activate(ComponentContext)}, {@link #deactivate(ComponentContext)},
 * {@link #bindRepository(ServiceReference)} and
 * {@link #unbindRepository(ServiceReference)} methods. Implementations of the
 * abstract API may safely assume to run thread-safe.
 * <p>
 * To ensure this thread-safeness, said methods should not be overwritten.
 */
public abstract class AbstractRegistrationSupport {

    /**
     * The JCR Repository service registration property used to create
     * the registration name. If this service registration property
     * (assumed to be a single string) does not exist, the repository is
     * not registered.
     */
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    /**
     * The LogService for logging. Extensions of this class must declare the log
     * service as a reference or call the {@link #bindLog(LogService)} to enable
     * logging correctly.
     */
    private volatile LogService log;

    /**
     * The OSGi ComponentContext.
     */
    private ComponentContext componentContext;

    /**
     * The (possibly empty) map of repositories which have been bound to the
     * registry before the registry has been activated.
     */
    private final Map<String, ServiceReference> repositoryRegistrationBacklog = new HashMap<String, ServiceReference>();

    /**
     * The map of repositories which have been bound to the registry component
     * and which are actually registered with the registry.
     */
    private final Map<String, Object> registeredRepositories = new HashMap<String, Object>();

    /**
     * A lock to serialize access to the registry management in this class.
     */
    protected final Object registryLock = new Object();

    // ---------- API to be implemented by extensions --------------------------

    /**
     * Performs additional activation tasks. This method is called by the
     * {@link #activate(ComponentContext)} method and is intended for internal
     * setup, such as acquiring the registry.
     *
     * @return Whether the activation succeeded or not. If <code>true</code>
     *         is returned, activation succeeded and any repositories which have
     *         been bound before the component was activated are now actually
     *         registered. If <code>false</code> is returned, activation
     *         failed and this component is disabled and receives no further
     *         repository bind and unbound events (apart for unbind events for
     *         repositories which have already been bound).
     */
    protected abstract boolean doActivate();

    /**
     * Performs additional deactivation tasks. This method is called by the
     * {@link #deactivate(ComponentContext)} method and is intended for internal
     * cleanup of setup done by the {@link #doActivate()} method.
     * <p>
     * This method is always called, regardless of whether {@link #doActivate()}
     * succeeded or not.
     */
    protected abstract void doDeactivate();

    /**
     * Called to actually register a repository with the registry. This method
     * is called by {@link #activate(ComponentContext)} for any repositories
     * bound before the component was activated and by
     * {@link #bindRepository(ServiceReference)} for any repositories bound
     * after the component was activated.
     * <p>
     * If actual registration fails, this method is expected to return
     * <code>null</code> to indicate this fact. In this case, the
     * {@link #unbindRepository(String, Object)} will NOT be called for the
     * named repository.
     * <p>
     * This method may safely assume that it is only called on or after
     * activation of this component on or before the component deactivation.
     *
     * @param name The name under which the repository is to be registered.
     * @param repository The <code>javax.jcr.Repository</code> to register.
     * @return Returns an object which is later given as the <code>data</code>
     *         parameter to the {@link #unbindRepository(String, Object)} method
     *         to unregister the repository of the given name. This may be
     *         <code>null</code> if actual registration failed.
     */
    protected abstract Object bindRepository(String name, Repository repository);

    /**
     * Called to actually unregister a repository with the registry. This method
     * is called by {@link #unbindRepository(ServiceReference)} for any
     * repositories unbound before the component is deactivated and by
     * {@link #deactivate(ComponentContext)} for any repositories not unbound
     * before the component is deactivated.
     * <p>
     * If the {@link #bindRepository(String, Repository)} returned
     * <code>null</code> for when the named repository was registered, this
     * method is not called.
     * <p>
     * This method may safely assume that it is only called on or after
     * activation of this component on or before the component deactivation.
     *
     * @param name The name under which the repository is to be registered.
     * @param data The data object returned by the
     *            {@link #bindRepositoryInternal(String, ServiceReference)}
     *            method.
     */
    protected abstract void unbindRepository(String name, Object data);

    // ---------- Implementation support methods -------------------------------

    /**
     * Returns the OSGi <code>ComponentContext</code> of this component. This
     * method returns <code>null</code> before the {@link #doActivate()}
     * method is called and after the {@link #doDeactivate()} method has been
     * called. That is, this method does not return <code>null</code> if it is
     * fully operational.
     */
    protected ComponentContext getComponentContext() {
        return this.componentContext;
    }

    /**
     * Logs a message with optional <code>Throwable</code> stack trace to the
     * log service or <code>stderr</code> if no log service is available.
     *
     * @param level The <code>LogService</code> level at which to log the
     *            message.
     * @param message The message to log, this should of course not be
     *            <code>null</code>.
     * @param t The <code>Throwable</code> to log along with the message. This
     *            may be <code>null</code>.
     */
    protected void log(int level, String message, Throwable t) {
        LogService log = this.log;
        if (log != null) {
            log.log(level, message, t);
        } else {
            System.err.print(level + " - " + message);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        }

    }

    /**
     * Returns the <code>name</code> property from the service properties or
     * <code>null</code> if no such property exists or the property is an
     * empty string.
     *
     * @param reference The <code>ServiceReference</code> whose
     *            <code>name</code> property is to be returned.
     * @return The non-empty name property or <code>null</code>.
     */
    protected String getName(ServiceReference reference) {
        String name = (String) reference.getProperty(REPOSITORY_REGISTRATION_NAME);
        if (name == null || name.length() == 0) {
            log(LogService.LOG_DEBUG,
                    "registerRepository: Repository not to be registered", null);
            return null;
        }

        return name;
    }

    // ---------- SCR intergration ---------------------------------------------

    /**
     * Activates this component thread-safely as follows:
     * <ol>
     * <li>Set the OSGi ComponentContext field
     * <li>Call {@link #doActivate()}
     * <li>Register repositores bound before activation calling
     * {@link #bindRepository(String, Repository)} for each such repository.
     * </ol>
     * <p>
     * If {@link #doActivate()} returns <code>false</code>, the repositories
     * already bound are not actually registered, but this component is
     * disabled.
     *
     * @param componentContext The OSGi <code>ComponentContext</code> of this
     *      component.
     */
    protected void activate(ComponentContext componentContext) {
        synchronized (this.registryLock) {
            this.componentContext = componentContext;

            if (this.doActivate()) {
                // register all repositories in the tmp map
                for (Iterator<Map.Entry<String, ServiceReference>> ri = this.repositoryRegistrationBacklog.entrySet().iterator(); ri.hasNext();) {
                    Map.Entry<String, ServiceReference> entry = ri.next();

                    this.bindRepositoryInternal(entry.getKey(),
                        entry.getValue());

                    ri.remove();
                }
            } else {
                // disable this component
                String name = (String) componentContext.getProperties().get(
                    ComponentConstants.COMPONENT_NAME);
                this.getComponentContext().disableComponent(name);
            }
        }
    }

    /**
     * Deactivates this component thread-safely as follows:
     * <ol>
     * <li>Unregister repositores still bound calling
     * {@link #unbindRepository(String, Object)} for each such repository.
     * <li>Call {@link #doDeactivate()}
     * <li>Clear the OSGi ComponentContext field
     * </ol>
     *
     * @param componentContext The OSGi <code>ComponentContext</code> of this
     *            component.
     */
    protected void deactivate(ComponentContext context) {

        synchronized (this.registryLock) {

            // unregister all repositories in the tmp map
            for (Iterator<Map.Entry<String, Object>> ri = this.registeredRepositories.entrySet().iterator(); ri.hasNext();) {
                Map.Entry<String, Object> entry = ri.next();

                this.unbindRepository(entry.getKey(), entry.getValue());

                ri.remove();
            }

            this.doDeactivate();

            this.componentContext = null;
        }
    }

    /**
     * Registers the repository identified by the OSGi service reference under
     * the name set as service property. If the repository service has not
     * name property, the repository is not registered.
     *
     * @param reference The <code>ServiceReference</code> representing the
     *      repository to register.
     */
    protected void bindRepository(ServiceReference reference) {
        String name = this.getName(reference);

        if (name != null) {
            synchronized (this.registryLock) {
                if (this.componentContext == null) {
                    // no context to register with, delay ??
                    this.repositoryRegistrationBacklog.put(name, reference);
                } else {
                    this.bindRepositoryInternal(name, reference);
                }
            }
        } else {
            this.log(LogService.LOG_INFO, "Service "
                + reference.getProperty(Constants.SERVICE_ID)
                + " has no name property, not registering", null);
        }
    }

    /**
     * Unregisters the repository identified by the OSGi service reference under
     * the name set as service property. If the repository service has no
     * name property, the repository is assumed not be registered and nothing
     * needs to be done.
     *
     * @param reference The <code>ServiceReference</code> representing the
     *      repository to unregister.
     */
    protected void unbindRepository(ServiceReference reference) {
        String name = this.getName(reference);
        if (name != null) {

            synchronized (this.registryLock) {
                // unbind the repository
                Object data = this.registeredRepositories.remove(name);
                if (data != null) {
                    this.unbindRepository(name, data);
                }

                // ensure unregistered from internal and temporary map
                this.repositoryRegistrationBacklog.remove(name);

                // make sure we have no reference to the service
                if (this.componentContext != null) {
                    this.componentContext.getBundleContext().ungetService(reference);
                }
            }
        } else {
            this.log(LogService.LOG_DEBUG, "Service "
                + reference.getProperty(Constants.SERVICE_ID)
                + " has no name property, nothing to unregister", null);
        }
    }

    /** Binds the LogService */
    protected void bindLog(LogService log) {
        this.log = log;
    }

    /** Unbinds the LogService */
    protected void unbindLog(LogService log) {
        if ( this.log == log ) {
            this.log = null;
        }
    }

    //---------- internal -----------------------------------------------------

    /**
     * Internal bind method called by {@link #activate(ComponentContext)} and
     * {@link #bindRepository(ServiceReference)} to actually control the
     * registration process by retrieving the repository and calling the
     * {@link #bindRepository(String, Repository)} method.
     */
    private void bindRepositoryInternal(String name, ServiceReference reference) {
        Repository repository = (Repository) this.getComponentContext().getBundleContext().getService(
            reference);
        Object data = this.bindRepository(name, repository);
        if (data != null) {
            this.registeredRepositories.put(name, data);
        }
    }

}
