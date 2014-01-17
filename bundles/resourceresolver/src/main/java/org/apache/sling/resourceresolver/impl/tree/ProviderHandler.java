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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.osgi.framework.Constants;

/**
 * The provider handler is the common base class for the
 * {@link ResourceProviderHandler} and the
 * {@link ResourceProviderFactoryHandler}.
 */
public abstract class ProviderHandler implements Comparable<ProviderHandler> {

    /** Service properties. */
    private final Map<String, Object> properties;

    /** Unique service id */
    private final Long serviceId;

    /** Configured roots. */
    private final String[] roots;

    /** Configured languages for queries. */
    private final Set<String> queryLanguages;

    /** Owns roots? */
    private final boolean ownsRoots;

    /** use ResourceAccessSecurity? */
    private final boolean useResourceAccessSecurity;

    /**
     * Create a new handler
     */
    public ProviderHandler(final Map<String, Object> properties) {
        this.properties = properties;
        this.serviceId = (Long) properties.get(Constants.SERVICE_ID);
        // calculate roots
        final List<String> configuredRoots = new ArrayList<String>();
        final String[] paths = PropertiesUtil.toStringArray(properties.get(ResourceProvider.ROOTS));
        if ( paths != null) {
            for(final String r : paths) {
                if (r != null) {
                    String path = r.trim();
                    // cut off trailing slash
                    if (path.endsWith("/") && path.length() > 1) {
                        path = path.substring(0, path.length() - 1);
                    }
                    if ( path.length() > 0 && !configuredRoots.contains(path)) {
                        configuredRoots.add(path);
                    }
                }
            }
        }
        if ( configuredRoots.size() == 0 ) {
            this.roots = null;
        } else {
            Collections.sort(configuredRoots);
            this.roots = configuredRoots.toArray(new String[configuredRoots.size()]);
        }
        this.ownsRoots = PropertiesUtil.toBoolean(properties.get(ResourceProvider.OWNS_ROOTS), false);
        this.useResourceAccessSecurity = PropertiesUtil.toBoolean(properties.get(ResourceProvider.USE_RESOURCE_ACCESS_SECURITY), false);
        final Set<String> configuredLanguages = new HashSet<String>();
        final String[] languages = PropertiesUtil.toStringArray(properties.get(QueriableResourceProvider.LANGUAGES));
        if ( languages != null) {
            for(final String l : languages) {
                if (l != null) {
                    final String language = l.trim();
                    if ( language.length() > 0 ) {
                        configuredLanguages.add(language);
                    }
                }
            }
        }
        if ( configuredLanguages.size() == 0 ) {
            this.queryLanguages = null;
        } else {
            this.queryLanguages = configuredLanguages;
        }
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
                returnValue = resourceAccessSecurity.getReadableResource(resource);
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
     * Return the service properties.
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Return the service id.
     */
    public Long getServiceId() {
        return this.serviceId;
    }

    /**
     * Does this provider own the roots?
     */
    public boolean ownsRoots() {
        return this.ownsRoots;
    }

    /**
     * Return a sorted array of roots for this provider. If no roots are configured,
     * this will return <code>null</code>
     * @return The array of roots or <code>null</code>
     */
    public String[] getRoots() {
        return this.roots;
    }

    /**
     * Check if the resource provider supports the language.
     */
    public boolean supportsQueryLanguages(final String language) {
        return this.queryLanguages != null && this.queryLanguages.contains(language);
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final ProviderHandler other) {
        if (this.serviceId.equals(other.serviceId)) {
            return 0; // same service
        }

        Object rankObj = this.getProperties().get(Constants.SERVICE_RANKING);
        Object otherRankObj = other.getProperties().get(Constants.SERVICE_RANKING);

        // If no rank, then spec says it defaults to zero.
        rankObj = (rankObj == null) ? new Integer(0) : rankObj;
        otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

        // If rank is not Integer, then spec says it defaults to zero.
        Integer rank = (rankObj instanceof Integer)
            ? (Integer) rankObj : new Integer(0);
        Integer otherRank = (otherRankObj instanceof Integer)
            ? (Integer) otherRankObj : new Integer(0);

        // Sort by rank in ascending order.
        if (rank.compareTo(otherRank) < 0) {
            return -1; // lower rank
        } else if (rank.compareTo(otherRank) > 0) {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by service id in descending order.
        return (this.serviceId.compareTo(other.serviceId) < 0) ? 1 : -1;
    }

    /**
     * @see ResourceProvider#getResource(ResourceResolver, String)
     */
    public abstract Resource getResource(final ResourceResolverContext ctx, final ResourceResolver resourceResolver, final String path);

    /**
     * @see ResourceProvider#listChildren(Resource)
     */
    public abstract Iterator<Resource> listChildren(final ResourceResolverContext ctx, final Resource parent);

    /**
     * Return the resource provider.
     */
    public abstract ResourceProvider getResourceProvider(final ResourceResolverContext ctx);

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
