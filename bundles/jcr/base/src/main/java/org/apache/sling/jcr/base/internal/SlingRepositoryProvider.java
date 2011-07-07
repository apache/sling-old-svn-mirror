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
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The repository provider listens for javax.jcr.Repository services and wraps
 * them as SlingRepository services (if required)
 */
@Component(specVersion="1.1")
@Reference(name="repository",
           referenceInterface=Repository.class,
            policy=ReferencePolicy.DYNAMIC,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class SlingRepositoryProvider {

    private static final String SLING_REPOSITORY = SlingRepository.class.getName();

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Long, RepositoryRegistration> registrations = new HashMap<Long, RepositoryRegistration>();

    private BundleContext bundleContext;

    private final List<Registration> repositories = new ArrayList<Registration>();

    protected void activate(final BundleContext ctx) {
        final List<Registration> copyList;
        synchronized ( repositories ) {
            this.bundleContext = ctx;
            copyList = new ArrayList<Registration>(this.repositories);
            this.repositories.clear();
        }
        for(final Registration reg : copyList) {
            this.bindRepository(reg.repository, reg.properties);
        }
    }

    protected void deactivate() {
        synchronized ( repositories ) {
            this.bundleContext = null;
        }
    }

    private boolean isSlingRepository(final Map<String, Object> props) {
        final String[] interfaces = (String[]) props.get(Constants.OBJECTCLASS);
        if ( interfaces != null ) { // sanity check
            for(final String name : interfaces) {
                if ( SLING_REPOSITORY.equals(name) ) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void bindRepository(final Repository repo, final Map<String, Object> props) {
        if ( !isSlingRepository(props) ) {
            final BundleContext processContext;
            synchronized ( repositories ) {
                processContext = this.bundleContext;
                if ( processContext == null ) {
                    this.repositories.add(new Registration(repo, props));
                }
            }
            if ( processContext != null ) {
                logger.info("Binding repository!");
                final Long key = (Long)props.get(Constants.SERVICE_ID);
                final RepositoryRegistration reg = new RepositoryRegistration();
                reg.wrapper = new SlingRepositoryWrapper(repo, processContext);
                reg.registration = processContext.registerService(SLING_REPOSITORY, reg.wrapper, null);
                synchronized ( this.registrations ) {
                    this.registrations.put(key, reg);
                }
            }
        }
    }

    protected void unbindRepository(final Repository repo, final Map<String, Object> props) {
        if ( !isSlingRepository(props) ) {
            logger.info("Unbinding repository!");
            synchronized ( repositories ) {
                this.repositories.remove(new Registration(repo, props));
            }
            final Long key = (Long)props.get(Constants.SERVICE_ID);
            final RepositoryRegistration slingRepo;
            synchronized ( this.registrations ) {
                slingRepo = this.registrations.remove(key);
            }
            if ( slingRepo != null ) {
                slingRepo.wrapper.dispose();
                slingRepo.registration.unregister();
            }
        }
    }

    private static final class Registration {
        public final Repository repository;
        public final Map<String, Object> properties;

        private final long key;

        public Registration(final Repository r, final Map<String, Object> p) {
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
            if ( ! (obj instanceof Registration ) ) {
                return false;
            }
            return this.key == ((Registration)obj).key;
        }
    }

    private static final class RepositoryRegistration {
        public ServiceRegistration registration;
        public SlingRepositoryWrapper wrapper;
    }
}
