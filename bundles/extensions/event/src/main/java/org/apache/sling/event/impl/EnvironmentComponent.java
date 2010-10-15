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
package org.apache.sling.event.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.event.EventAdmin;

/**
 * Environment component. This component provides "global settings"
 * to all services, like the application id and the thread pool.
 * @since 3.0
 */
@Component()
@Service(value=EnvironmentComponent.class)
public class EnvironmentComponent {

    @Reference
    private SlingRepository repository;

    @Reference
    private EventAdmin eventAdmin;

    @Reference(policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private DynamicClassLoaderManager classLoaderManager;

    /**
     * Our thread pool.
     */
    @Reference(referenceInterface=EventingThreadPool.class)
    private ThreadPool threadPool;

    /** Sling settings service. */
    @Reference
    private SlingSettingsService settingsService;

    /**
     * Activate this component.
     */
    @Activate
    protected void activate() {
        // Set the application id and the thread pool
        Environment.APPLICATION_ID = this.settingsService.getSlingId();
        Environment.THREAD_POOL = this.threadPool;
    }

    /**
     * Dectivate this component.
     */
    @Deactivate
    protected void deactivate() {
        // Unset the thread pool
        if ( Environment.THREAD_POOL == this.threadPool ) {
            Environment.THREAD_POOL = null;
        }
    }

    /**
     * Return the dynamic classloader for loading events from the repository.
     */
    public ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.classLoaderManager;
        if ( dclm != null ) {
            return dclm.getDynamicClassLoader();
        }
        // if we don't have a dynamic classloader, we return our classloader
        return this.getClass().getClassLoader();
    }

    /**
     * Create a new admin session.
     * @return A new admin session.
     * @throws RepositoryException
     */
    public Session createAdminSession()
    throws RepositoryException {
        final SlingRepository repo = this.repository;
        if ( repo == null ) {
            // as the repo is a hard dependency for this service, the repo
            // is always available, but we check for null anyway!
            throw new RepositoryException("Repository is currently not available.");
        }
        return repo.loginAdministrative(null);
    }

    /**
     * Return the event admin.
     */
    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    protected void bindThreadPool(final EventingThreadPool etp) {
        this.threadPool = etp;
    }

    protected void unbindThreadPool(final EventingThreadPool etp) {
        if ( this.threadPool == etp ) {
            this.threadPool = null;
        }
    }
}
