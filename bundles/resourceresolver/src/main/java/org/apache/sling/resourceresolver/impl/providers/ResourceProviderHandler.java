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
package org.apache.sling.resourceresolver.impl.providers;

import org.apache.sling.resourceresolver.impl.providers.tree.Pathable;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;

/**
 * Holder for a resource provider service.
 */
public class ResourceProviderHandler implements Comparable<ResourceProviderHandler>, Pathable {

    private final ResourceProviderInfo info;

    private final BundleContext bundleContext;

    private volatile ResourceProvider<?> provider;

    private final ProviderContextImpl context = new ProviderContextImpl();

    public ResourceProviderHandler(final BundleContext bc, final ResourceProviderInfo info) {
        this.info = info;
        this.bundleContext = bc;
    }

    public ResourceProviderInfo getInfo() {
        return this.info;
    }

    public boolean activate() {
        this.provider = (ResourceProvider<?>) this.bundleContext.getService(this.info.getServiceReference());
        if ( this.provider != null ) {
            this.provider.start(context);
        }
        return this.provider != null;
    }

    public ResourceProvider<?> getResourceProvider() {
        return this.provider;
    }

    public void deactivate() {
        if ( this.provider != null ) {
            this.provider.stop();
            this.provider = null;
            this.context.update(null, null);
            this.bundleContext.ungetService(this.info.getServiceReference());
        }
    }

    @Override
    public int compareTo(final ResourceProviderHandler o) {
        return this.getInfo().compareTo(o.getInfo());
    }

    @Override
    public String getPath() {
        return this.getInfo().getPath();
    }

    public void update() {
        if ( this.provider != null ) {
            this.provider.update(ProviderContext.EXCLUDED_PATHS_CHANGED + ProviderContext.OBSERVATION_LISTENER_CHANGED);
        }
    }

    public ProviderContextImpl getProviderContext() {
        return this.context;
    }
}
