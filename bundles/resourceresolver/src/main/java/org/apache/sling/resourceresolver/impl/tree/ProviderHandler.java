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
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The provider handler is the common base class for the
 * {@link ResourceProviderHandler} and the
 * {@link ResourceProviderFactoryHandler}.
 * <p>
 * The natural ordering for instances of this class is according to the
 * OSGi Service ranking order:
 * <ol>
 * <li>An instance with a higher service.ranking property is compared less
 *    than an instance with a lower service.ranking property.</li>
 * <li>If service.ranking properties are equal, the service with the
 *    lower service.id is compared less than the service with the higher
 *    service.id</li>
 * </ol>
 */
public class ProviderHandler implements Comparable<ProviderHandler> {

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Service properties. */
    private final ResourceProviderHandler resourceProviderHandler;

    /** Unique service id */
    private final Long serviceId;
    
    private final int ranking;

    private final boolean useResourceAccessSecurity;

    /**
     * Create a new handler
     */
    public ProviderHandler(ResourceProviderHandler resourceProviderHandler) {
        this.serviceId = (Long) resourceProviderHandler.getInfo().getServiceReference().getProperty(Constants.SERVICE_ID);
        this.resourceProviderHandler = resourceProviderHandler;
        this.ranking = PropertiesUtil.toInteger(resourceProviderHandler.getInfo().getServiceReference().getProperty(Constants.SERVICE_RANKING), 0);
        this.useResourceAccessSecurity = resourceProviderHandler.getInfo().getUseResourceAccessSecurity();
    }

    public boolean canCreate(final ResourceResolverContext ctx, final ResourceResolver resolver, final String path) {
        final ResourceAccessSecurityTracker tracker = ctx.getResourceAccessSecurityTracker();
        boolean allowed = true;
        if ( useResourceAccessSecurity ) {
            final ResourceAccessSecurity security = tracker.getProviderResourceAccessSecurity();
            if ( security != null ) {
                allowed = security.canCreate(path, resolver);
            } else {
                allowed = false;
            }
        }

        if ( allowed ) {
            final ResourceAccessSecurity security = tracker.getApplicationResourceAccessSecurity();
            if (security != null) {
                allowed = security.canCreate(path, resolver);
            }
        }
        return allowed;
    }

    public boolean canDelete(final ResourceResolverContext ctx, final Resource resource) {
        final ResourceAccessSecurityTracker tracker = ctx.getResourceAccessSecurityTracker();
        boolean allowed = true;
        if ( useResourceAccessSecurity ) {
            final ResourceAccessSecurity security = tracker.getProviderResourceAccessSecurity();
            if ( security != null ) {
                allowed = security.canDelete(resource);
            } else {
                allowed = false;
            }
        }

        if ( allowed ) {
            final ResourceAccessSecurity security = tracker.getApplicationResourceAccessSecurity();
            if (security != null) {
                allowed = security.canDelete(resource);
            }
        }
        return allowed;
    }

    /**
     * applies resource access security if configured
     */
    protected Resource getReadableResource ( final ResourceResolverContext ctx, Resource resource ) {
        final ResourceAccessSecurityTracker tracker = ctx.getResourceAccessSecurityTracker();

        Resource returnValue = null;

        if (useResourceAccessSecurity && resource != null) {
            final ResourceAccessSecurity resourceAccessSecurity = tracker.getProviderResourceAccessSecurity();
            if (resourceAccessSecurity != null) {
                returnValue = resourceAccessSecurity.getReadableResource(resource);
            }
        } else {
            returnValue = resource;
        }

        if ( returnValue != null ) {
            final ResourceAccessSecurity resourceAccessSecurity = tracker.getApplicationResourceAccessSecurity();
            if (resourceAccessSecurity != null) {
                returnValue = resourceAccessSecurity.getReadableResource(returnValue);
            }
        }

        return returnValue;
    }

    /**
     * applies resource access security if configured
     */
    protected Iterator<Resource> getReadableChildrenIterator ( final ResourceResolverContext ctx, final Iterator<Resource> childrenIterator ) {
        Iterator<Resource> returnValue = null;
        if ( childrenIterator != null ) {
            returnValue = new Iterator<Resource>() {

                private Resource nextResource;

                {
                    seek();
                }

                private void seek() {
                    while( nextResource == null && childrenIterator.hasNext() ) {
                        nextResource = getReadableResource(ctx, childrenIterator.next());
                    }
                }

                public boolean hasNext() {
                    return nextResource != null;
                }

                public Resource next() {
                    if ( nextResource == null ) {
                        throw new NoSuchElementException();
                    }
                    final Resource result = nextResource;
                    nextResource = null;
                    seek();
                    return result;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

            };
        }

        return returnValue;
    }

    /**
     * Return the service id.
     */
    public Long getServiceId() {
        return this.serviceId;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final ProviderHandler other) {
        if (this.serviceId.equals(other.serviceId)) {
            return 0; // same service
        }

        // Sort by rank in ascending order.
        int rankOrder = Integer.valueOf(this.ranking).compareTo(other.ranking);
        if (rankOrder != 0) {
            return (rankOrder > 0) ? -1 : 1;
        }

        // If ranks are equal, then sort by service id in descending order.
        return (this.serviceId.compareTo(other.serviceId) > 0) ? 1 : -1;
    }

    /**
     * Return the resource provider.
     */
    public ResourceProviderHandler getResourceProviderHandler() {
        return resourceProviderHandler;
    }

    /**
     * Return a name of the resource provider/factory.
     */
    public String getName() {
        final StringBuilder snBuilder = new StringBuilder(64);
        snBuilder.append('{');
        snBuilder.append(this.toString());
        snBuilder.append('/');
        snBuilder.append(this.serviceId.toString());
        snBuilder.append('}');
        return snBuilder.toString();
    }
}
