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

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource provider factory handler handles access to
 * resource provider factories.
 */
public class ResourceProviderFactoryHandler extends ProviderHandler {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * A do nothing resource provider.
     */
    private static final ResourceProvider NULL_PROVIDER = new ResourceProvider() {

        public Iterator<Resource> listChildren(final Resource parent) {
            return null;
        }

        public Resource getResource(final ResourceResolver resourceResolver, final String path) {
            return null;
        }

        public Resource getResource(final ResourceResolver resourceResolver, final HttpServletRequest request, final String path) {
            return null;
        }
    };

    /**
     * A resource provider fractory
     */
    private final ResourceProviderFactory resourceProviderFactory;

    /**
     * Create a new handler
     */
    public ResourceProviderFactoryHandler(final ResourceProviderFactory factory,
                                          final Map<String, Object> properties) {
        super(properties);
        this.resourceProviderFactory = factory;
    }

    /**
     * Get a resource provider
     * If the user can't be authenticated <code>null</code> is returned.
     * @param ctx The user context.
     * @return A resource provider or <code>null</code>
     */
    public ResourceProvider getResourceProvider(final ResourceResolverContext ctx) {
        ResourceProvider rp = ctx.getFactoryResourceProvider(this.getServiceId());
        if ( rp == null ) {
            try {
                rp = this.login(ctx);
            } catch (final LoginException le ) {
                // for now we log to debug, as a failed login for not required resource provider
                // is expected
                // TODO - we could introduce a service property controlling this?
                logger.debug("Unable to login to " + this.getName(), le);
            }
            if ( rp == null ) {
                ctx.addFactoryResourceProvider(this.getServiceId(), NULL_PROVIDER);
            }
        } else if ( rp == NULL_PROVIDER ) {
            rp = null;
        }
        return rp;
    }

    /**
     * @see ResourceProvider#getResource(ResourceResolver, String)
     */
    public Resource getResource(final ResourceResolverContext ctx, final ResourceResolver resourceResolver, final String path) {
        final ResourceProvider rp = this.getResourceProvider(ctx);
        if ( rp != null ) {
            return getReadableResource(ctx, rp.getResource(resourceResolver, path) );
        }
        return null;
    }

    /**
     * @see ResourceProvider#listChildren(Resource)
     */
    public Iterator<Resource> listChildren(final ResourceResolverContext ctx, final Resource parent) {
        final ResourceProvider rp = this.getResourceProvider(ctx);
        if ( rp != null ) {
            return getReadableChildrenIterator( ctx, rp.listChildren(parent) );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return resourceProviderFactory.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if ( obj instanceof ResourceProviderFactoryHandler ) {
            return resourceProviderFactory.equals(((ResourceProviderFactoryHandler) obj).resourceProviderFactory);
        } else if ( obj instanceof ResourceProviderFactory) {
            return resourceProviderFactory.equals(obj);
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
        return resourceProviderFactory.toString();
    }

    /**
     * Login to a resource resolver factory.
     */
    public ResourceProvider login(final ResourceResolverContext ctx) throws LoginException {
        final ResourceProvider rp;
        if (ctx.isAdmin()) {
            rp = this.resourceProviderFactory.getAdministrativeResourceProvider(ctx.getAuthenticationInfo());
        } else {
            rp = this.resourceProviderFactory.getResourceProvider(ctx.getAuthenticationInfo());
        }
        ctx.addFactoryResourceProvider(this.getServiceId(), rp);
        return rp;
    }
}
