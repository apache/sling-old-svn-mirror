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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.DynamicResourceProvider;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.RefreshableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Features;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;

/**
 * This class keeps track of the used resource providers for a
 * resource resolver.
 * Like a resource resolver itself, this class is not thread safe.
 */
public class ResourceResolverContext {

    /**
     * The name of the property listing feature requirements for a resource.
     *
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/featureflags.html">Feature
     *      Flags</a>
     */
    private static final String RESOURCE_PROPERTY = "sling:features";

    /** A map of all used providers created by a factory. */
    private final Map<Long, ResourceProvider> providers = new HashMap<Long, ResourceProvider>();

    /** A set of all dynamic providers (for closing them later on) */
    private final Set<DynamicResourceProvider> dynamicProviders = new HashSet<DynamicResourceProvider>();

    /** A set of all refreshable providers */
    private final Set<RefreshableResourceProvider> refreshableProviders = new HashSet<RefreshableResourceProvider>();

    /** A set of all modifying providers */
    private final Set<ModifyingResourceProvider> modifyingProviders = new HashSet<ModifyingResourceProvider>();

    /** Is this a resource resolver for an admin? */
    private final boolean isAdmin;

    /**
     * The original authentication information - this is used for cloning and lazy logins.
     */
    private final Map<String, Object> originalAuthInfo;

    /** service tracker for ResourceAccessSecurity service */
    private final ResourceAccessSecurityTracker resourceAccessSecurityTracker;

    /** Resource type resource resolver (admin resolver) */
    private ResourceResolver resourceTypeResourceResolver;

    /** Features ServiceTracker */
    private final FeaturesHolder featuresService;

    /**
     * Create a new resource resolver context.
     */
    public ResourceResolverContext(final boolean isAdmin, final Map<String, Object> originalAuthInfo, final ResourceAccessSecurityTracker resourceAccessSecurityTracker, final FeaturesHolder featuresService) {
        this.isAdmin = isAdmin;
        this.originalAuthInfo = originalAuthInfo;
        this.resourceAccessSecurityTracker = resourceAccessSecurityTracker;
        this.featuresService = featuresService;
    }

    /**
     * Is this an admin resource resolver.
     */
    public boolean isAdmin() {
        return this.isAdmin;
    }

    /**
     * @return the Features service tracker used by this context
     */
    public FeaturesHolder getFeaturesHolder() {
        return this.featuresService;
    }

    /**
     * Return the authentication info.
     */
    public Map<String, Object> getAuthenticationInfo() {
        return this.originalAuthInfo;
    }

    /**
     * Add a new resource provider
     * @param key      The unique key of the provider
     * @param provider The provider.
     */
    public void addFactoryResourceProvider(final Long key, final ResourceProvider provider) {
        this.providers.put(key, provider);
        if (provider instanceof DynamicResourceProvider) {
            this.dynamicProviders.add((DynamicResourceProvider) provider);
        }
        if (provider instanceof ModifyingResourceProvider) {
            this.modifyingProviders.add((ModifyingResourceProvider) provider);
        }
        if (provider instanceof RefreshableResourceProvider) {
            this.refreshableProviders.add((RefreshableResourceProvider)provider);
        }
    }

    /**
     * Return a resource provider for a given key
     * @param key The unique key of a provider
     * @return The resource provider or <code>null</code>
     */
    public ResourceProvider getFactoryResourceProvider(final Long key) {
        return this.providers.get(key);
    }

    /**
     * Check all active dynamic resource providers.
     */
    public boolean isLive() {
        boolean result = true;
        for (final DynamicResourceProvider provider : this.dynamicProviders) {
            if (!provider.isLive()) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Close all dynamic resource providers.
     */
    public void close() {
        for (final DynamicResourceProvider provider : this.dynamicProviders) {
            provider.close();
        }
        this.dynamicProviders.clear();
        this.providers.clear();
        this.refreshableProviders.clear();
        if ( this.resourceTypeResourceResolver != null ) {
            this.resourceTypeResourceResolver.close();
            this.resourceTypeResourceResolver = null;
        }
    }

    /**
     * Revert all transient changes.
     */
    public void revert(final ResourceResolver resolver) {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            provider.revert(resolver);
        }
    }

    /**
     * Commit all transient changes
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            provider.commit(resolver);
        }
    }

    /**
     * Do we have changes?
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            if ( provider.hasChanges(resolver) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refresh
     */
    public void refresh() {
        for(final RefreshableResourceProvider provider : this.refreshableProviders) {
            provider.refresh();
        }
    }

    /**
     * get's the ServiceTracker of the ResourceAccessSecurity service
     */
    public ResourceAccessSecurityTracker getResourceAccessSecurityTracker () {
        return resourceAccessSecurityTracker;
    }

    private ResourceResolver getResourceTypeResourceResolver(
            final ResourceResolverFactory factory,
            final ResourceResolver resolver) {
        if ( this.isAdmin ) {
            return resolver;
        } else {
            if ( this.resourceTypeResourceResolver == null ) {
                try {
                    this.resourceTypeResourceResolver = factory.getAdministrativeResourceResolver(null);
                } catch (final LoginException e) {
                    // we simply ignore this and return null
                }
            }
            return this.resourceTypeResourceResolver;
        }
    }

    /**
     * Get the parent resource type
     *
     * @see org.apache.sling.api.resource.ResourceResolver#getParentResourceType(java.lang.String)
     */
    public String getParentResourceType(
            final ResourceResolverFactory factory,
            final ResourceResolver resolver,
            final String resourceType) {
        // normalize resource type to a path string
        final String rtPath = (resourceType == null ? null : ResourceUtil.resourceTypeToPath(resourceType));
        // get the resource type resource and check its super type
        String resourceSuperType = null;

        if ( rtPath != null ) {
            ResourceResolver adminResolver = this.getResourceTypeResourceResolver(factory, resolver);
            if ( adminResolver != null ) {
                final Resource rtResource = adminResolver.getResource(rtPath);
                if (rtResource != null) {
                    resourceSuperType = rtResource.getResourceSuperType();
                }
            }
        }
        return resourceSuperType;
    }

    /**
     * Checks whether the resource has a sling:features property. If so the
     * Features service is consulted to indicate whether any of the named
     * features is enabled and thus the resource is visible at all.
     * <p>
     * If none of the listed features is enabled, the resource is not visible
     * and null is returned. Otherwise the list of features configured on the
     * Resource is stored in the ResourceMetadata and the resource is returned.
     * <p>
     * If the Features service is not available, the features property is not
     * consulted and not added to the ResourceMetadata. The resource is returned
     * unmodified in this case.
     *
     * @param resource The resource to check for features
     * @return The unmodified resource if the Features service is not available
     *         or no features are set on the property. Otherwise the features
     *         are added to the ResourceMetadata and the resource is returned if
     *         one of the listed features is enabled. Otherwise null is
     *         returned.
     */
    public Resource applyFeatures(final Resource resource) {
        if (resource != null) {
            Features featuresService = this.featuresService.getFeatures();
            if (featuresService != null) {
                ValueMap props = resource.adaptTo(ValueMap.class);
                if (props != null) {
                    ClientContext featureContext = featuresService.getCurrentClientContext();
                    String[] features = props.get(RESOURCE_PROPERTY, String[].class);
                    if (features != null && features.length > 0) {
                        for (String feature : features) {

                            // check whether the feature must be disabled
                            boolean negative = false;
                            if (feature.charAt(0) == '-') {
                                feature = feature.substring(1);
                                negative = false;
                            }

                            if (featureContext.isEnabled(feature) ^ negative) {
                                resource.getResourceMetadata().put(RESOURCE_PROPERTY, features);
                                return resource;
                            }
                        }

                        // invariant: none of the named features enabled
                        return null;
                    }
                }
            }
        }

        // invariant: null resource or no feature check on resource
        return resource;
    }
}
