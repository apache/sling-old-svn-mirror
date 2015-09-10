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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.BasicResolveContext;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * This class keeps track of the used resource providers for a
 * resource resolver.
 * Like a resource resolver itself, this class is not thread safe.
 */
public class ResourceResolverContext {

    /**
     * Context objects for all used resource providers.
     */
    private final Map<ResourceProvider<?>, Object> providerStates = new IdentityHashMap<ResourceProvider<?>, Object>();

    /**
     * All ResourceProviders to be used.
     */
    private final List<ResourceProviderHandler> providerHandlers;

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

    /** Flag for handling multiple calls to close. */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * Create a new resource resolver context.
     */
    public ResourceResolverContext(final boolean isAdmin, final Map<String, Object> originalAuthInfo,
            final ResourceAccessSecurityTracker resourceAccessSecurityTracker,
            final Collection<ResourceProviderHandler> providerHandlers) {
        this.isAdmin = isAdmin;
        this.originalAuthInfo = originalAuthInfo;
        this.resourceAccessSecurityTracker = resourceAccessSecurityTracker;
        this.providerHandlers = new ArrayList<ResourceProviderHandler>(providerHandlers);
    }

    /**
     * Is this an admin resource resolver.
     */
    public boolean isAdmin() {
        return this.isAdmin;
    }

    /**
     * Return the authentication info.
     */
    public Map<String, Object> getAuthenticationInfo() {
        return this.originalAuthInfo;
    }

    /**
     * Check all active dynamic resource providers.
     */
    public boolean isLive(ResourceResolver resolver) {
        boolean result = true;
        for (final ResourceProviderHandler handler : this.providerHandlers) {
            if (!handler.getResourceProvider().isLive((ResolveContext) getResolveContext(resolver, handler))) {
                result = false;
                break;
            }
        }
        return result;
    }

    public ResolveContext<?> getResolveContext(ResourceResolver resolver, ResourceProviderHandler handler) {
        return getResolveContext(resolver, handler, Collections.<String,String>emptyMap());
    }

    public ResolveContext<?> getResolveContext(ResourceResolver resolver, ResourceProviderHandler handler, Map<String, String> parameters) {
        try {
            final Object state = authenticate(handler);
            return new BasicResolveContext(resolver, parameters, state);
        } catch (LoginException e) {
            throw new SlingException("Can't create resolve context", e);
        }
    }

    private Object authenticate(ResourceProviderHandler handler) throws LoginException {
        if (handler.getInfo().getAuthType() == AuthType.no) {
            return null;
        }

        ResourceProvider<?> resourceProvider = handler.getResourceProvider();
        if (providerStates.containsKey(resourceProvider)) {
            return providerStates.get(resourceProvider);
        } else {
            Object state = resourceProvider.authenticate(originalAuthInfo);
            providerStates.put(resourceProvider, state);
            return state;
        }
    }

    /**
     * Close all dynamic resource providers.
     */
    public void close() {
        if ( this.isClosed.compareAndSet(false, true)) {
            for (final Entry<ResourceProvider<?>, Object> e : providerStates.entrySet()) {
                try {
                    if (e.getValue() != null) {
                        ((ResourceProvider) e.getKey()).logout(e.getValue());
                    }
                } catch ( final Throwable t) {
                    // the provider might already be terminated (bundle stopped etc.)
                    // so we ignore anything from here
                }
            }
            this.providerHandlers.clear();
            this.providerStates.clear();
            if ( this.resourceTypeResourceResolver != null ) {
                try {
                    this.resourceTypeResourceResolver.close();
                } catch ( final Throwable t) {
                    // the resolver (or the underlying provider) might already be terminated (bundle stopped etc.)
                    // so we ignore anything from here
                }
                this.resourceTypeResourceResolver = null;
            }
        }
    }

    /**
     * Revert all transient changes.
     */
    public void revert(ResourceResolver resolver) {
        for (final ResourceProviderHandler handler : this.providerHandlers) {
            if (handler.getInfo().getModifiable()) {
                handler.getResourceProvider().revert((ResolveContext) getResolveContext(resolver, handler));
            }
        }
    }

    /**
     * Commit all transient changes
     */
    public void commit(ResourceResolver resolver) throws PersistenceException {
        for (final ResourceProviderHandler handler : this.providerHandlers) {
            if (handler.getInfo().getModifiable()) {
                handler.getResourceProvider().commit((ResolveContext) getResolveContext(resolver, handler));
            }
        }
    }

    /**
     * Do we have changes?
     */
    public boolean hasChanges(ResourceResolver resolver) {
        for (final ResourceProviderHandler handler : this.providerHandlers) {
            if (handler.getInfo().getModifiable()) {
                if (handler.getResourceProvider().hasChanges((ResolveContext) getResolveContext(resolver, handler))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Refresh
     */
    public void refresh(ResourceResolver resolver) {
        for (final ResourceProviderHandler handler : this.providerHandlers) {
            handler.getResourceProvider().refresh((ResolveContext) getResolveContext(resolver, handler));
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
     * Returns {@link #getProperty(Resource, String, Class) getProperty(res,
     * propName, String.class)}
     *
     * @param res The resource to access the property from
     * @param propName The name of the property to access
     * @return The property as a {@code String} or {@code null} if the property
     *         does not exist or cannot be converted into a {@code String}
     */
    public static String getProperty(final Resource res, final String propName) {
        return getProperty(res, propName, String.class);
    }

    /**
     * Returns the value of the name property of the resource converted to the
     * requested {@code type}.
     * <p>
     * If the resource itself does not have the property, the property is looked
     * up in the {@code jcr:content} child node. This access is done through the
     * same {@code ValueMap} as is used to access the property directly. This
     * generally only works for JCR based {@code ValueMap} instances which
     * provide access to relative path property names. This may not work in non
     * JCR {@code ValueMap}, however in non JCR envs there is usually no
     * "jcr:content" child node anyway
     *
     * @param res The resource to access the property from
     * @param propName The name of the property to access
     * @param type The type into which to convert the property
     * @return The property converted to the requested {@code type} or
     *         {@code null} if the property does not exist or cannot be
     *         converted into the requested {@code type}
     */
    public static <Type> Type getProperty(final Resource res, final String propName, final Class<Type> type) {

        // check the property in the resource itself
        final ValueMap props = res.adaptTo(ValueMap.class);
        if (props != null) {
            Type prop = props.get(propName, type);
            if (prop != null) {
                return prop;
            }
            // otherwise, check it in the jcr:content child resource
            // This is a special case checking for JCR based resources
            // we directly use the deep resolution of properties of the
            // JCR value map implementation - this does not work
            // in non JCR environments, however in non JCR envs there
            // is usually no "jcr:content" child node anyway
            prop = props.get("jcr:content/" + propName, type);
            return prop;
        }

        return null;
    }

    public ResourceResolverContext clone(Map<String, Object> authenticationInfo) {
        // create the merged map
        final Map<String, Object> newAuthenticationInfo = new HashMap<String, Object>();
        if (originalAuthInfo != null) {
            newAuthenticationInfo.putAll(originalAuthInfo);
        }
        if (authenticationInfo != null) {
            newAuthenticationInfo.putAll(authenticationInfo);
        }

        return new ResourceResolverContext(isAdmin, newAuthenticationInfo, resourceAccessSecurityTracker, providerHandlers);
    }

    public List<ResourceProviderHandler> getProviders() {
        return providerHandlers;
    }
}
