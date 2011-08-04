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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The repository provider listens for javax.jcr.Repository services and
 * registers a web console plugin
 */
@Component(specVersion="1.1")
@Reference(name="repository",
           referenceInterface=Repository.class,
            policy=ReferencePolicy.DYNAMIC,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class RepositoryPrinterProvider {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Long, ServiceRegistration> registrations = new HashMap<Long, ServiceRegistration>();

    private BundleContext bundleContext;

    /** List of services which are bound before activate is called. */
    private final List<PendingService> pendingServices = new ArrayList<PendingService>();

    /**
     * Activate this component.
     */
    protected void activate(final BundleContext ctx) {
        final List<PendingService> copyList;
        synchronized ( pendingServices ) {
            this.bundleContext = ctx;
            copyList = new ArrayList<PendingService>(this.pendingServices);
            this.pendingServices.clear();
        }
        for(final PendingService reg : copyList) {
            this.registerPrinter(this.bundleContext, reg.repository, reg.properties);
        }
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate() {
        synchronized ( pendingServices ) {
            this.bundleContext = null;
        }
    }

    private void registerPrinter(final BundleContext processContext,
            final Repository repo,
            final Map<String, Object> props) {
        logger.info("Providing new configuration printer for {} : {}", repo, props);
        final Long key = (Long)props.get(Constants.SERVICE_ID);
        final RepositoryPrinter printer = new RepositoryPrinter(repo, props);
        final ServiceRegistration reg = processContext.registerService(RepositoryPrinter.class.getName(),
                printer, printer.getProperties());
        synchronized ( this.registrations ) {
            this.registrations.put(key, reg);
        }
    }

    /**
     * Bind a new repository.
     */
    protected void bindRepository(final Repository repo, final Map<String, Object> props) {
        final BundleContext processContext;
        synchronized ( pendingServices ) {
            processContext = this.bundleContext;
            if ( processContext == null ) {
                this.pendingServices.add(new PendingService(repo, props));
            }
        }
        if ( processContext != null ) {
            this.registerPrinter(processContext, repo, props);
        }
    }

    /**
     * Unind a new repository.
     */
    protected void unbindRepository(final Repository repo, final Map<String, Object> props) {
        synchronized ( pendingServices ) {
            this.pendingServices.remove(new PendingService(repo, props));
        }
        final Long key = (Long)props.get(Constants.SERVICE_ID);
        final ServiceRegistration reg;
        synchronized ( this.registrations ) {
            reg = this.registrations.remove(key);
        }
        if ( reg != null ) {
            reg.unregister();
        }
    }

    /**
     * Data class for a pending service.
     */
    private static final class PendingService {
        public final Repository repository;
        public final Map<String, Object> properties;

        private final long key;

        public PendingService(final Repository r, final Map<String, Object> p) {
            this.repository = r;
            this.properties = p;
            this.key = (Long) this.properties.get(Constants.SERVICE_ID);
        }

        @Override
        public int hashCode() {
            return this.repository.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) {
                return true;
            }
            if ( ! (obj instanceof PendingService ) ) {
                return false;
            }
            return this.key == ((PendingService)obj).key;
        }
    }
}
