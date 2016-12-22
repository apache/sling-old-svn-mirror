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
package org.apache.sling.caconfig.resource.impl.def;

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.management.ContextPathStrategyMultiplexer;
import org.apache.sling.caconfig.resource.impl.util.ConfigNameUtil;
import org.apache.sling.caconfig.resource.impl.util.PathEliminateDuplicatesIterator;
import org.apache.sling.caconfig.resource.impl.util.PathParentExpandIterator;
import org.apache.sling.caconfig.resource.impl.util.PropertyUtil;
import org.apache.sling.caconfig.resource.spi.CollectionInheritanceDecider;
import org.apache.sling.caconfig.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.resource.spi.ContextResource;
import org.apache.sling.caconfig.resource.spi.InheritanceDecision;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=ConfigurationResourceResolvingStrategy.class)
@Designate(ocd=DefaultConfigurationResourceResolvingStrategy.Config.class)
public class DefaultConfigurationResourceResolvingStrategy implements ConfigurationResourceResolvingStrategy {

    @ObjectClassDefinition(name="Apache Sling Context-Aware Configuration Default Resource Resolving Strategy",
                           description="Standardized access to configurations in the resource tree.")
    public static @interface Config {

        @AttributeDefinition(name="Enabled",
                description = "Enable this configuration resource resolving strategy.")
        boolean enabled() default true;

        @AttributeDefinition(name="Configurations path",
                             description = "Paths where the configurations are stored in.")
        String configPath() default "/conf";

        @AttributeDefinition(name="Fallback paths",
                description = "Global fallback configurations, ordered from most specific (checked first) to least specific.")
        String[] fallbackPaths() default {"/conf/global", "/apps/conf", "/libs/conf"};

        @AttributeDefinition(name="Config collection inheritance property names",
                description = "Additional property names to " + PROPERTY_CONFIG_COLLECTION_INHERIT + " to handle configuration inheritance. The names are used in the order defined, "
                            + "always starting with " + PROPERTY_CONFIG_COLLECTION_INHERIT + ". Once a property with a value is found, that value is used and the following property names are skipped.")
        String[] configCollectionInheritancePropertyNames();

    }

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigurationResourceResolvingStrategy.class);

    private volatile Config config;

    @Reference
    private ContextPathStrategyMultiplexer contextPathStrategy;

    @Reference(cardinality=ReferenceCardinality.MULTIPLE,
            policy=ReferencePolicy.DYNAMIC,
            fieldOption=FieldOption.REPLACE)
    private volatile List<CollectionInheritanceDecider> collectionInheritanceDeciders;

    @Activate
    private void activate(final Config config) {
        this.config = config;
    }

    @Deactivate
    private void deactivate() {
        this.config = null;
    }

    @SuppressWarnings("unchecked")
    Iterator<String> getResolvePaths(final Resource contentResource, final Collection<String> bucketNames) {
        return new IteratorChain(
            // add all config references found in resource hierarchy
            findConfigRefs(contentResource, bucketNames),
            // finally add the global fallbacks
            new ArrayIterator(this.config.fallbackPaths())
        );
    }

    /**
     * Searches the resource hierarchy upwards for all config references and returns them.
     * @param refs List to add found resources to
     * @param startResource Resource to start searching
     */
    @SuppressWarnings("unchecked")
    private Iterator<String> findConfigRefs(final Resource startResource, final Collection<String> bucketNames) {

        // collect all context path resources (but filter out those without config reference)
        final Iterator<ContextResource> contextResources = new FilterIterator(contextPathStrategy.findContextResources(startResource),
                new Predicate() {
                    @Override
                    public boolean evaluate(Object object) {
                        ContextResource contextResource = (ContextResource)object;
                        return StringUtils.isNotBlank(contextResource.getConfigRef());
                    }
                });

        // get config resource path for each context resource, filter out items where not reference could be resolved
        final Iterator<String> configPaths = new Iterator<String>() {

            private final List<ContextResource> relativePaths = new ArrayList<>();

            private String next = seek();

            private String useFromRelativePathsWith;

            private String seek() {
                String val = null;
                while ( val == null && (useFromRelativePathsWith != null || contextResources.hasNext()) ) {
                    if ( useFromRelativePathsWith != null ) {
                        final ContextResource contextResource = relativePaths.remove(relativePaths.size() - 1);
                        val = checkPath(contextResource, useFromRelativePathsWith + "/" + contextResource.getConfigRef(), bucketNames);
                        if (val != null) {
                            log.trace("+ Found reference for context path {}: {}", contextResource.getResource().getPath(), val);
                        }
                        if ( relativePaths.isEmpty() ) {
                            useFromRelativePathsWith = null;
                        }
                    } else {
                        final ContextResource contextResource = contextResources.next();
                        val = contextResource.getConfigRef();

                        // if absolute path found we are (probably) done
                        if (val != null && val.startsWith("/")) {
                            val = checkPath(contextResource, val, bucketNames);
                        }

                        if (val != null) {
                            final boolean isAbsolute = val.startsWith("/");
                            if ( isAbsolute && !relativePaths.isEmpty() ) {
                                useFromRelativePathsWith = val;
                                val = null;
                            } else if ( !isAbsolute ) {
                                relativePaths.add(0, contextResource);
                                val = null;
                            }
                        }
                        
                        if (val != null) {
                            log.trace("+ Found reference for context path {}: {}", contextResource.getResource().getPath(), val);
                        }
                    }
                }
                if ( val == null && !relativePaths.isEmpty() ) {
                    log.error("Relative references not used as no absolute reference was found: {}", relativePaths);
                }
                return val;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public String next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                final String result = next;
                next = seek();
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        // expand paths and eliminate duplicates
        return new PathEliminateDuplicatesIterator(new PathParentExpandIterator(config.configPath(), configPaths));
    }

    private String checkPath(final ContextResource contextResource, String ref, final Collection<String> bucketNames) {
        // combine full path if relativeRef is present
        ref = ResourceUtil.normalize(ref);

        for (String bucketName : bucketNames) {
            String notAllowedPostfix = "/" + bucketName;
            if (ref != null && ref.endsWith(notAllowedPostfix)) {
                log.warn("Ignoring reference to {} from {} - Probably misconfigured as it ends with '{}'",
                        contextResource.getConfigRef(), contextResource.getResource().getPath(), notAllowedPostfix);
                ref = null;
            }
        }
        
        if (ref != null && !isAllowedConfigPath(ref)) {
            log.warn("Ignoring reference to {} from {} - not in allowed paths.",
                    contextResource.getConfigRef(), contextResource.getResource().getPath());
            ref = null;
        }

        if (ref != null && isFallbackConfigPath(ref)) {
            log.warn("Ignoring reference to {} from {} - already a fallback path.",
                    contextResource.getConfigRef(), contextResource.getResource().getPath());
            ref = null;
        }

        return ref;
    }

    private boolean isAllowedConfigPath(String path) {
        return path.startsWith(config.configPath() + "/");
    }

    private boolean isFallbackConfigPath(final String ref) {
        for(final String path : this.config.fallbackPaths()) {
            if (StringUtils.equals(ref, path) || StringUtils.startsWith(ref, path + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnabledAndParamsValid(final Resource contentResource, final Collection<String> bucketNames, final String configName) {
        return config.enabled() && contentResource != null && ConfigNameUtil.isValid(bucketNames) && ConfigNameUtil.isValid(configName);
    }

    private String buildResourcePath(String path, String name) {
        return ResourceUtil.normalize(path + "/" + name);
    }

    @Override
    public Resource getResource(final Resource contentResource, final Collection<String> bucketNames, final String configName) {
        Iterator<Resource> resources = getResourceInheritanceChain(contentResource, bucketNames, configName);
        if (resources != null && resources.hasNext()) {
            return resources.next();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Iterator<Resource> getResourceInheritanceChainInternal(final Collection<String> bucketNames, final String configName,
            final Iterator<String> paths, final ResourceResolver resourceResolver) {

        // find all matching items among all configured paths
        Iterator<Resource> matchingResources = IteratorUtils.transformedIterator(paths, new Transformer() {
            @Override
            public Object transform(Object input) {
                String path = (String)input;
                for (String bucketName : bucketNames) {
                    final String name = bucketName + "/" + configName;
                    final String configPath = buildResourcePath(path, name);
                    Resource resource = resourceResolver.getResource(configPath);
                    if (resource != null) {
                        log.trace("+ Found matching config resource for inheritance chain: {}", configPath);
                        return resource;
                    }
                    else {
                        log.trace("- No matching config resource for inheritance chain: {}", configPath);
                    }
                }
                return null;
            }
        });
        Iterator<Resource> result = IteratorUtils.filteredIterator(matchingResources, PredicateUtils.notNullPredicate());
        if (result.hasNext()) {
            return result;
        }
        return null;
    }

    @Override
    public Iterator<Resource> getResourceInheritanceChain(Resource contentResource, Collection<String> bucketNames, String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketNames, configName)) {
            return null;
        }
        final ResourceResolver resourceResolver = contentResource.getResourceResolver();

        Iterator<String> paths = getResolvePaths(contentResource, bucketNames);
        return getResourceInheritanceChainInternal(bucketNames, configName, paths, resourceResolver);
    }

    private boolean include(final List<CollectionInheritanceDecider> deciders,
            final String bucketName,
            final Resource resource,
            final Set<String> blockedItems) {
        boolean result = !blockedItems.contains(resource.getName());
        if ( result && deciders != null && !deciders.isEmpty() ) {
            for(int i=deciders.size()-1;i>=0;i--) {
                final InheritanceDecision decision = deciders.get(i).decide(resource, bucketName);
                if ( decision == InheritanceDecision.EXCLUDE ) {
                    log.trace("- Block resource collection inheritance for bucket {}, resource {} because {} retruned EXCLUDE.",
                            bucketName, resource.getPath(), deciders.get(i));
                    result = false;
                    break;
                } else if ( decision == InheritanceDecision.BLOCK ) {
                    log.trace("- Block resource collection inheritance for bucket {}, resource {} because {} retruned BLOCK.",
                            bucketName, resource.getPath(), deciders.get(i));
                    result = false;
                    blockedItems.add(resource.getName());
                    break;
                }
            }
        }
        return result;
    }

    private Collection<Resource> getResourceCollectionInternal(final Collection<String> bucketNames, final String configName,
            Iterator<String> paths, ResourceResolver resourceResolver) {

        final Map<String,Resource> result = new LinkedHashMap<>();
        final List<CollectionInheritanceDecider> deciders = this.collectionInheritanceDeciders;
        final Set<String> blockedItems = new HashSet<>();

        boolean inherit = false;
        while (paths.hasNext()) {
            final String path = paths.next();
            
            Resource item = null;
            String bucketNameUsed = null;
            for (String bucketName : bucketNames) {
                String name = bucketName + "/" + configName;
                String configPath = buildResourcePath(path, name);
                item = resourceResolver.getResource(configPath);
                if (item != null) {
                    bucketNameUsed = bucketName;
                    break;
                }
                else {
                    log.trace("- No collection parent resource found: {}", configPath);
                }
            }

            if (item != null) {
                log.trace("o Check children of collection parent resource: {}", item.getPath());
                if (item.hasChildren()) {
                    for (Resource child : item.getChildren()) {
                        if (isValidResourceCollectionItem(child)
                                && !result.containsKey(child.getName())
                                && include(deciders, bucketNameUsed, child, blockedItems)) {
                            log.trace("+ Found collection resource item {}", child.getPath());
                            result.put(child.getName(), child);
                       }
                    }
                }

                // check collection inheritance mode on current level - should we check on next-highest level as well?
                final ValueMap valueMap = item.getValueMap();
                inherit = PropertyUtil.getBooleanValueAdditionalKeys(valueMap, PROPERTY_CONFIG_COLLECTION_INHERIT,
                        config.configCollectionInheritancePropertyNames());
                if (!inherit) {
                    break;
                }
            }
        }

        return result.values();
    }

    @Override
    public Collection<Resource> getResourceCollection(final Resource contentResource, final Collection<String> bucketNames, final String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketNames, configName)) {
            return null;
        }
        Iterator<String> paths = getResolvePaths(contentResource, bucketNames);
        Collection<Resource> result = getResourceCollectionInternal(bucketNames, configName, paths, contentResource.getResourceResolver());
        if (!result.isEmpty()) {
            return result;
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Iterator<Resource>> getResourceCollectionInheritanceChain(final Resource contentResource,
            final Collection<String> bucketNames, final String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketNames, configName)) {
            return null;
        }
        final ResourceResolver resourceResolver = contentResource.getResourceResolver();
        final List<String> paths = IteratorUtils.toList(getResolvePaths(contentResource, bucketNames));
        
        // get resource collection with respect to collection inheritance
        Collection<Resource> resourceCollection = getResourceCollectionInternal(bucketNames, configName, paths.iterator(), resourceResolver);
        
        // get inheritance chain for each item found
        // yes, this resolves the closest item twice, but is the easiest solution to combine both logic aspects
        Iterator<Iterator<Resource>> result = IteratorUtils.transformedIterator(resourceCollection.iterator(), new Transformer() {
            @Override
            public Object transform(Object input) {
                Resource item = (Resource)input;
                return getResourceInheritanceChainInternal(bucketNames, configName + "/" + item.getName(), paths.iterator(), resourceResolver);
            }
        });
        if (result.hasNext()) {
            return IteratorUtils.toList(result);
        }
        else {
            return null;
        }
    }

    private boolean isValidResourceCollectionItem(Resource resource) {
        // do not include jcr:content nodes in resource collection list
        return !StringUtils.equals(resource.getName(), "jcr:content");
    }

    @Override
    public String getResourcePath(Resource contentResource, String bucketName, String configName) {
        if (!isEnabledAndParamsValid(contentResource, Collections.singleton(bucketName), configName)) {
            return null;
        }
        String name = bucketName + "/" + configName;

        Iterator<String> configPaths = this.findConfigRefs(contentResource, Collections.singleton(bucketName));
        if (configPaths.hasNext()) {
            String configPath = buildResourcePath(configPaths.next(), name);
            log.trace("+ Building configuration path for name '{}' for resource {}: {}", name, contentResource.getPath(), configPath);
            return configPath;
        }
        else {
            log.trace("- No configuration path for name '{}' found for resource {}", name, contentResource.getPath());
            return null;
        }
    }

    @Override
    public String getResourceCollectionParentPath(Resource contentResource, String bucketName, String configName) {
        return getResourcePath(contentResource, bucketName, configName);
    }

}
