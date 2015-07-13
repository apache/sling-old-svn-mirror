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

import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;

public class ResourceProviderHandler implements Comparable<ResourceProviderHandler> {

    private final ResourceProviderInfo info;

    private final BundleContext bundleContext;

    private volatile ResourceProvider<?> provider;

    public ResourceProviderHandler(final BundleContext bc, final ResourceProviderInfo info) {
        this.info = info;
        this.bundleContext = bc;
    }

    public ResourceProviderInfo getInfo() {
        return this.info;
    }

    public ResourceProvider<?> getResourceProvider() {
        ResourceProvider<?> rp = this.provider;
        if ( rp == null ) {
            synchronized ( this ) {
                if ( this.provider == null ) {
                    this.provider = (ResourceProvider<?>) this.bundleContext.getService(this.info.getServiceReference());
                }
                rp = this.provider;
            }
        }
        return rp;
    }

    public void deactivate() {
        if ( this.provider != null ) {
            this.provider = null;
            this.bundleContext.ungetService(this.info.getServiceReference());
        }
    }

    @Override
    public int compareTo(final ResourceProviderHandler o) {
        return this.info.compareTo(o.info);
    }
}
