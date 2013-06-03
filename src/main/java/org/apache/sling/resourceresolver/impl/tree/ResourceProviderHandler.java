/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.resourceresolver.impl.tree;

import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;

/**
 * The resource provider handler handles access to
 * resource providers.
 */
public class ResourceProviderHandler extends ProviderHandler {

    /** The handled resource provider.*/
    private final ResourceProvider resourceProvider;

    /**
     * Create a new handler.
     */
    public ResourceProviderHandler(final ResourceProvider resourceProvider,
                                   final Map<String, Object> properties) {
        super(properties);
        this.resourceProvider = resourceProvider;
    }

    /**
     * @see ResourceProvider#getResource(ResourceResolver, String)
     */
    public Resource getResource(final ResourceResolverContext ctx, final ResourceResolver resourceResolver, final String path) {
        return getReadableResource(ctx, this.resourceProvider.getResource(resourceResolver, path) );
    }

    /**
     * @see ResourceProvider#listChildren(Resource)
     */
    public Iterator<Resource> listChildren(final ResourceResolverContext ctx, final Resource parent) {
        return getReadableChildrenIterator( ctx, this.resourceProvider.listChildren(parent) );
    }

    /**
     * Return the resource provider.
     */
    public ResourceProvider getResourceProvider() {
        return this.resourceProvider;
    }

    /**
     * @see org.apache.sling.resourceresolver.impl.tree.ProviderHandler#getResourceProvider(org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext)
     */
    public ResourceProvider getResourceProvider(final ResourceResolverContext ctx) {
        return this.getResourceProvider();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return resourceProvider.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if ( obj instanceof ResourceProviderHandler ) {
            return resourceProvider.equals(((ResourceProviderHandler) obj).resourceProvider);
        } else if ( obj instanceof ResourceProvider) {
            return resourceProvider.equals(obj);
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return resourceProvider.toString();
    }
}
