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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.resourceresolver.impl.helper.AbstractIterator;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link AuthenticatedResourceProvider} implementation keeps a resource
 * provider and the authentication information (through the {@link ResolveContext}.
 *
 * The methods are similar to {@link ResourceProvider}.
 */
public class AuthenticatedResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(ResourceResolverImpl.class);

    public static final AuthenticatedResourceProvider UNAUTHENTICATED_PROVIDER = new AuthenticatedResourceProvider(null, false, null, null);

    private final ResourceProvider<Object> provider;

    private final ResolveContext<Object> resolveContext;

    private final ResourceAccessSecurityTracker tracker;

    private final boolean useRAS;

    public AuthenticatedResourceProvider(@Nonnull final ResourceProvider<Object> provider,
            final boolean useRAS,
            @Nonnull final ResolveContext<Object> resolveContext,
            @Nonnull final ResourceAccessSecurityTracker tracker) {
        this.provider = provider;
        this.resolveContext = resolveContext;
        this.tracker = tracker;
        this.useRAS = useRAS;
    }

    /**
     * Get the resolve context.
     * @return The resolve context
     */
    public @Nonnull ResolveContext<Object> getResolveContext() {
        return this.resolveContext;
    }

    /**
     * #see {@link ResourceProvider#refresh(ResolveContext)}
     */
    public void refresh() {
        this.provider.refresh(this.resolveContext);
    }

    /**
     * #see {@link ResourceProvider#isLive(ResolveContext)}
     */
    public boolean isLive() {
        return this.provider.isLive(this.resolveContext);
    }

    /**
     * #see {@link ResourceProvider#getParent(ResolveContext, Resource)}
     */
    public Resource getParent(final Resource child) {
        return wrapResource(this.provider.getParent(this.resolveContext, child));
    }

    /**
     * #see {@link ResourceProvider#getResource(ResolveContext, String, ResourceContext, Resource)}
     */
    public Resource getResource(final String path, final Resource parent, final Map<String, String> parameters) {
        final ResourceContext resourceContext;
        if ( parameters != null ) {
            resourceContext = new ResourceContext() {

                @Override
                public Map<String, String> getResolveParameters() {
                    return parameters;
                }
            };
        } else {
            resourceContext = ResourceContext.EMPTY_CONTEXT;
        }
        return wrapResource(this.provider.getResource(this.resolveContext, path, resourceContext, parent));
    }

    /**
     * #see {@link ResourceProvider#listChildren(ResolveContext, Resource)}
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        return wrapIterator(this.provider.listChildren(this.resolveContext, parent));
    }

    /**
     * #see {@link ResourceProvider#getAttributeNames(ResolveContext)}
     */
    public Collection<String> getAttributeNames(final Set<String> attributeNames) {
        Collection<String> rpAttributeNames = this.provider.getAttributeNames(this.resolveContext);
        if (rpAttributeNames != null) {
            attributeNames.addAll(rpAttributeNames);
        }
        return attributeNames;
    }

    /**
     * #see {@link ResourceProvider#getAttribute(ResolveContext, String)}
     */
    public Object getAttribute(final String name) {
        return this.provider.getAttribute(this.resolveContext, name);
    }

    /**
     * #see {@link ResourceProvider#create(ResolveContext, String, Map)}
     */
    public Resource create(final ResourceResolver resolver,
            final String path,
            final Map<String, Object> properties)
    throws PersistenceException {
        if ( this.canCreate(resolver, path) ) {
            return this.provider.create(this.resolveContext, path, properties);
        } else {
            return null;
        }
    }

    /**
     * #see {@link ResourceProvider#delete(ResolveContext, Resource)}
     */
    public void delete(final Resource resource) throws PersistenceException {
        if ( this.canDelete(resource) ) {
            this.provider.delete(this.resolveContext, resource);
        } else {
            throw new PersistenceException("Unable to delete resource " + resource.getPath());
        }
    }

    /**
     * #see {@link ResourceProvider#revert(ResolveContext)}
     */
    public void revert() {
        this.provider.revert(this.resolveContext);
    }

    /**
     * #see {@link ResourceProvider#commit(ResolveContext)}
     */
    public void commit() throws PersistenceException {
        this.provider.commit(this.resolveContext);
    }

    /**
     * #see {@link ResourceProvider#hasChanges(ResolveContext)}
     */
    public boolean hasChanges() {
        return this.provider.hasChanges(this.resolveContext);
    }

    /**
     * #see {@link ResourceProvider#getQueryLanguageProvider()}
     */
    private QueryLanguageProvider<Object> getQueryLanguageProvider() {
        return this.provider.getQueryLanguageProvider();
    }

    /**
     * #see {@link QueryLanguageProvider#getSupportedLanguages(ResolveContext)}
     */
    public String[] getSupportedLanguages() {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return jcrQueryProvider.getSupportedLanguages(this.resolveContext);
    }

    /**
     * #see {@link QueryLanguageProvider}{@link #findResources(String, String)}
     */
    public Iterator<Resource> findResources(final String query, final String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return wrapIterator(jcrQueryProvider.findResources(this.resolveContext, transformQuery(query, language), language));
    }

    /**
     * #see {@link QueryLanguageProvider#queryResources(ResolveContext, String, String)}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator<Map<String, Object>> queryResources(final String query, final String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return (Iterator) jcrQueryProvider.queryResources(this.resolveContext, transformQuery(query, language), language);
    }

    /**
     * #see {@link ResourceProvider#adaptTo(ResolveContext, Class)}
     */
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        return this.provider.adaptTo(this.resolveContext, type);
    }

    /**
     * #see {@link ResourceProvider#copy(ResolveContext, String, String)}
     */
    public boolean copy(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        return this.provider.copy(this.resolveContext, srcAbsPath, destAbsPath);
    }

    /**
     * #see {@link ResourceProvider#move(ResolveContext, String, String)}
     */
    public boolean move(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        return this.provider.move(this.resolveContext, srcAbsPath, destAbsPath);
    }

    private boolean canCreate(final ResourceResolver resolver, final String path) {
        boolean allowed = true;
        if ( this.useRAS ) {
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

    private boolean canDelete(final Resource resource) {
        boolean allowed = true;
        if ( this.useRAS ) {
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
    private String transformQuery ( final String query, final String language ) {
        String returnValue = query;

        if (this.useRAS) {
            final ResourceAccessSecurity resourceAccessSecurity = tracker
                    .getProviderResourceAccessSecurity();
            if (resourceAccessSecurity != null) {
                try {
                    returnValue = resourceAccessSecurity.transformQuery(
                            returnValue, language, this.resolveContext.getResourceResolver());
                } catch (AccessSecurityException e) {
                    logger.error(
                            "AccessSecurityException occurred while trying to transform the query {} (language {}).",
                            new Object[] { query, language }, e);
                }
            }
        }

        final ResourceAccessSecurity resourceAccessSecurity = tracker
                .getApplicationResourceAccessSecurity();
        if (resourceAccessSecurity != null) {
            try {
                returnValue = resourceAccessSecurity.transformQuery(
                        returnValue, language, this.resolveContext.getResourceResolver());
            } catch (AccessSecurityException e) {
                logger.error(
                        "AccessSecurityException occurred while trying to transform the query {} (language {}).",
                        new Object[] { query, language }, e);
            }
        }

        return returnValue;
    }

    /**
     * Wrap a resource with additional resource access security
     * @param rsrc The resource or {@code null}.
     * @return The wrapped resource or {@code null}
     */
    private @CheckForNull Resource wrapResource(@CheckForNull Resource rsrc) {
        Resource returnValue = null;

        if (useRAS && rsrc != null) {
            final ResourceAccessSecurity resourceAccessSecurity = tracker.getProviderResourceAccessSecurity();
            if (resourceAccessSecurity != null) {
                returnValue = resourceAccessSecurity.getReadableResource(rsrc);
            }
        } else {
            returnValue = rsrc;
        }

        if ( returnValue != null ) {
            final ResourceAccessSecurity resourceAccessSecurity = tracker.getApplicationResourceAccessSecurity();
            if (resourceAccessSecurity != null) {
                returnValue = resourceAccessSecurity.getReadableResource(returnValue);
            }
        }

        return returnValue;
    }

    private Iterator<Resource> wrapIterator(Iterator<Resource> iterator) {
        if (iterator == null) {
            return iterator;
        } else {
            return new SecureIterator(iterator);
        }
    }

    private class SecureIterator extends AbstractIterator<Resource> {

        private final Iterator<Resource> iterator;

        public SecureIterator(Iterator<Resource> iterator) {
            this.iterator = iterator;
        }

        @Override
        protected Resource seek() {
            while (iterator.hasNext()) {
                final Resource resource = wrapResource(iterator.next());
                if (resource != null) {
                    return resource;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "# rp: " + this.provider + "]";
    }
}
