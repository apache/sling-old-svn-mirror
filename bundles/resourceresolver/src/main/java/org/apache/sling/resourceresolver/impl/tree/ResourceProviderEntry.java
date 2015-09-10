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
package org.apache.sling.resourceresolver.impl.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.FastTreeMap;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceProviderEntry</code> class represents a node in the tree of
 * resource providers spanned by the root paths of the provider resources.
 * <p>
 * That means this class has a map of child ResourceProviderEntries, keyed by the child name
 * and a list of ProviderHandlers that are mapped to the path that this ResourceProviderEntry represents.
 * To locate a list of potential ResourceProviders the path is split into elements and then that list used to
 * walk down the tree of ResourceProviders. eg: for a path /a/b/c/d the list of ProviderHandlers would be accessed
 * by rootProvider.get("a").get("b").get("c").get("d")  assuming the final get("d") was not null. If it was, then the list
 * of ProviderHanders would be rootProvider.get("a").get("b").get("c").
 * <p>
 * This class is comparable to itself to help keep the child entries list sorted by their prefix.
 */
public class ResourceProviderEntry {

    private final Logger logger = LoggerFactory.getLogger(ResourceProviderEntry.class);

    /**
     * Returns the resource with the given path or <code>null</code> if neither
     * the resource provider of this entry nor the resource provider of any of
     * the child entries can provide the resource.
     *
     * @param path
     *            The path to the resource to return.
     * @param parameters
     * @return The resource for the path or <code>null</code> if no resource can
     *         be found.
     * @throws org.apache.sling.api.SlingException
     *             if an error occurrs trying to access an existing resource.
     */
    public Resource getResource(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String path,
            final Map<String, String> parameters,
            final boolean isResolve) {
        return getInternalResource(ctx, resourceResolver, path, parameters, isResolve);
    }

	/**
	 * Get a list of resource provider entries navigating down the tree starting
	 * from this provider until there are no providers left in the tree. Given a
	 * sequence of path elements a/b/c/d this function will inspect this
	 * ResourceProviderEntry for a child entry of "a" and if present add it to
	 * the list, then it will inspect that child entry for a child "b", then
	 * child "b" for child "c" etc until the list of elements is exhausted or
	 * the child does not exist.
	 *
	 * @param entries
	 *            List to add the entries to.
	 * @param elements
	 *            The path already split into segments.
	 */
    public List<ResourceProviderHandler> getMatchingProvider(ResourceResolverContext ctx, final String path) {
        List<ResourceProviderHandler> handlers = new ArrayList<ResourceProviderHandler>();
        for (final ResourceProviderHandler h : ctx.getProviders()) {
            if (path.startsWith(h.getInfo().getPath())) {
                handlers.add(h);
            }
        }
        return handlers;
    }

    /**
     * Resolve a resource from a path into a Resource
     *
     * @param ctx The resource resolver context
     * @param resourceResolver the ResourceResolver.
     * @param fullPath the Full path
     * @param parameters
     * @return null if no resource was found, a resource if one was found.
     */
    private Resource getInternalResource(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String fullPath,
            final Map<String, String> parameters,
            final boolean isResolve) {
        try {

            if (fullPath == null || fullPath.length() == 0 || fullPath.charAt(0) != '/') {
                logger.debug("Not absolute {}", fullPath);
                return null; // fullpath must be absolute
            }
            final List<ResourceProviderHandler> entries = this.getMatchingProvider(ctx, fullPath);

            Resource fallbackResource = null;

            // the path is in reverse order end first
            for (int i = entries.size() - 1; i >= 0; i--) {
                boolean foundFallback = false;
                ResourceProviderHandler h = entries.get(i);
                ResolveContext resolveContext = ctx.getResolveContext(resourceResolver, h, parameters);
                final Resource resource = h.getResourceProvider().getResource(resolveContext, fullPath, null);
                if (resource != null) {
                    if (resource.getResourceMetadata() != null && resource.getResourceMetadata()
                            .get(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING) != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Resolved Full {} using {} - continue resolving flag is set!",
                                    new Object[] { fullPath, h.getInfo() });
                        }
                        fallbackResource = resource;
                        fallbackResource.getResourceMetadata().remove(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING);
                        foundFallback = true;
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Resolved Full {} using {}",
                                    new Object[] { fullPath, h.getInfo() });
                        }
                        return resource;
                    }
                }
                if (!foundFallback) {
                    logger.debug("Resource null {} ", fullPath);
                    return fallbackResource;
                }
            }

            // resolve against this one
            final Resource resource = getResourceFromProviders(ctx, resourceResolver, fullPath, parameters);
            if (resource != null) {
                return resource;
            }

            if ( fallbackResource != null ) {
                logger.debug("Using first found resource {} for {}", fallbackResource, fullPath);
                return fallbackResource;
            }

            // query: /libs/sling/servlet/default
            // resource Provider: libs/sling/servlet/default/GET.servlet
            // list will match libs, sling, servlet, default
            // and there will be no resource provider at the end
            // SLING-3482 : this is only done for getResource but not resolve
            //              as it is important e.g. for servlet resolution
            //              to get the parent resource for resource traversal.
            if ( !isResolve ) {
                if (isIntermediatePath(ctx, fullPath)) {
                    logger.debug("Resolved Synthetic {}", fullPath);
                    return new SyntheticResource(resourceResolver, fullPath, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
                }
            }
        } catch ( final SlingException se ) {
            // we rethrow the SlingException (see SLING-4644)
            throw se;
        } catch (final Exception ignore) {
            logger.warn("Unexpected exception while trying to get resource for " + fullPath, ignore);
        }
        logger.debug("Resource null {} ", fullPath);
        return null;
    }

    private boolean isIntermediatePath(final ResourceResolverContext ctx, final String fullPath) {
        for (ResourceProviderHandler h : ctx.getProviders()) {
            if (h.getInfo().getPath().startsWith(fullPath)) {
                return true;
            }
        }
        return false;
    }

    public Resource getResourceFromProviders(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String fullPath,
            final Map<String, String> parameters) {
        Resource fallbackResource = null;
        for (ResourceProviderHandler h : getMatchingProvider(ctx, fullPath)) {
            boolean foundFallback = false;
            ResolveContext resolveContext = ctx.getResolveContext(resourceResolver, h, parameters);
            final Resource resource = h.getResourceProvider().getResource(resolveContext, fullPath, null);
            if (resource != null) {
                if ( resource.getResourceMetadata() != null && resource.getResourceMetadata().get(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING) != null ) {
                    logger.debug("Resolved Base {} using {} - continue resolving flag is set!", fullPath, h);
                    fallbackResource = resource;
                    fallbackResource.getResourceMetadata().remove(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING);
                    foundFallback = true;
                } else {
                    logger.debug("Resolved Base {} using {} ", fullPath, h);
                    return resource;
                }
            }
            if ( !foundFallback ) {
                logger.debug("Resource null {} ", fullPath);
                return fallbackResource;
            }
        }
        return fallbackResource;
    }

    private List<ProviderHandler> getModifyingProviderHandlers(final ResourceResolverContext ctx,
                                                        final ResourceResolver resourceResolver,
                                                        final String fullPath) {
        final List<ResourceProviderHandler> entries = this.getMatchingProvider(ctx, fullPath);

        // build up a list of viable ModifyingResourceProviders in order of specificity
        for (int i = entries.size() - 1; i >= 0; i--) {
            final ResourceProviderHandler h = entries.get(i);
                if ( h.getInfo().getModifiable() ) {
                    return Collections.singletonList(new ProviderHandler(h));
                }
            }

        return Collections.emptyList();
    }

    /**
     * Delete the resource.  Iterate over all viable ModifyingProviderHandlers giving each an opportunity to delete
     * the resource if they are able.
     *
     * @throws NullPointerException if resource is null
     * @throws UnsupportedOperationException If deletion is not allowed/possible
     * @throws PersistenceException If deletion fails
     */
    public void delete(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final Resource resource) throws PersistenceException {
        final String fullPath = resource.getPath();
        final List<ProviderHandler> viableHandlers = this.getModifyingProviderHandlers(ctx, resourceResolver, fullPath);

        boolean anyProviderAttempted = false;

        // Give all viable handlers a chance to delete the resource
        for (ProviderHandler currentProviderHandler : viableHandlers) {
            if (currentProviderHandler.canDelete(ctx, resource)) {
                ResourceProviderHandler h = currentProviderHandler.getResourceProviderHandler();
                ResolveContext resolveContext = ctx.getResolveContext(resourceResolver, h);
                if ( viableHandlers.size() == 1 || currentProviderHandler.getResourceProviderHandler().getResourceProvider().getResource(resolveContext, fullPath, null) != null ) {
                    currentProviderHandler.getResourceProviderHandler().getResourceProvider().delete(resolveContext, resource);
                    anyProviderAttempted = true;
                }
            }
        }

        // If none of the viable handlers could delete the resource or if the list of handlers was empty, throw an Exception
        if (!anyProviderAttempted) {
            throw new UnsupportedOperationException("delete at '" + fullPath + "'");
        }
    }

    /**
     * Create a resource.  Iterate over all viable ModifyingProviderHandlers stopping at the first one which creates
     * the resource and return the created resource.
     *
     * @throws UnsupportedOperationException If creation is not allowed/possible
     * @throws PersistenceException If creation fails
     * @return The new resource
     */
    public Resource create(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String fullPath,
            final Map<String, Object> properties) throws PersistenceException {
        final List<ProviderHandler> viableHandlers = this.getModifyingProviderHandlers(ctx, resourceResolver, fullPath);

        for (final ProviderHandler currentProviderHandler : viableHandlers) {
            if (currentProviderHandler.canCreate(ctx, resourceResolver, fullPath)) {
                ResourceProviderHandler h = currentProviderHandler.getResourceProviderHandler();
                ResolveContext resolveContext = ctx.getResolveContext(resourceResolver, h);
                final Resource creationResultResource = currentProviderHandler.getResourceProviderHandler().getResourceProvider().create(resolveContext, fullPath, properties);

                if (creationResultResource != null) {
                    return creationResultResource;
                }
            }
        }

        // If none of the viable handlers could create the resource or if the list of handlers was empty, throw an Exception
        throw new UnsupportedOperationException("create '" + ResourceUtil.getName(fullPath) + "' at " + ResourceUtil.getParent(fullPath));
    }

    private static final char SPLIT_SEP = '/';
    private static final String[] EMPTY_RESULT = new String[0];

    /**
     * Split the string by slash.
     * This method never returns null.
     * @param st The string to split
     * @return an array of the strings between the separator
     */
    public static String[] split(final String st) {

        if (st == null) {
            return EMPTY_RESULT;
        }
        final char[] pn = st.toCharArray();
        if (pn.length == 0) {
            return EMPTY_RESULT;
        }
        if (pn.length == 1 && pn[0] == SPLIT_SEP) {
            return EMPTY_RESULT;
        }
        int n = 1;
        int start = 0;
        int end = pn.length;
        while (start < end && SPLIT_SEP == pn[start])
            start++;
        while (start < end && SPLIT_SEP == pn[end - 1])
            end--;
        for (int i = start; i < end; i++) {
            if (SPLIT_SEP == pn[i]) {
                n++;
            }
        }
        final String[] e = new String[n];
        int s = start;
        int j = 0;
        for (int i = start; i < end; i++) {
            if (pn[i] == SPLIT_SEP) {
                e[j++] = new String(pn, s, i - s);
                s = i + 1;
            }
        }
        if (s < end) {
            e[j++] = new String(pn, s, end - s);
        }
        return e;
    }
}
