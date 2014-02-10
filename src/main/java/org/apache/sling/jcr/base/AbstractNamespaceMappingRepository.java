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

import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>AbstractNamespaceMappingRepository</code> is an abstract
 * implementation of the {@link SlingRepository} interface which provides
 * default support for namespace mapping.
 *
 * @deprecated as of API version 2.3 (bundle version 2.3). Use
 *             {@link NamespaceMappingSupport} or
 *             {@link AbstractSlingRepositoryManager} and
 *             {@link AbstractSlingRepository2} instead.
 */
@Deprecated
@ProviderType
public abstract class AbstractNamespaceMappingRepository extends NamespaceMappingSupport implements SlingRepository {

    private ServiceTracker namespaceMapperTracker;

    protected final NamespaceMapper[] getNamespaceMapperServices() {
        if (namespaceMapperTracker != null) {
            // call namespace mappers
            final Object[] nsMappers = namespaceMapperTracker.getServices();
            if (nsMappers != null) {
                NamespaceMapper[] mappers = new NamespaceMapper[nsMappers.length];
                System.arraycopy(nsMappers, 0, mappers, 0, nsMappers.length);
                return mappers;
            }
        }
        return null;
    }

    protected void setup(final BundleContext bundleContext) {
        super.setup(bundleContext, this);
        this.namespaceMapperTracker = new ServiceTracker(bundleContext, NamespaceMapper.class.getName(), null);
        this.namespaceMapperTracker.open();
    }

    protected void tearDown() {
        if ( this.namespaceMapperTracker != null ) {
            this.namespaceMapperTracker.close();
            this.namespaceMapperTracker = null;
        }
        super.tearDown();
    }

}
