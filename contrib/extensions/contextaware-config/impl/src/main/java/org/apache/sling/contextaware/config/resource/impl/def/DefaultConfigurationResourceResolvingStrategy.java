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
package org.apache.sling.contextaware.config.resource.impl.def;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.contextaware.config.resource.impl.ContextPathStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=ConfigurationResourceResolvingStrategy.class)
@Designate(ocd=DefaultConfigurationResourceResolvingStrategy.Config.class)
public class DefaultConfigurationResourceResolvingStrategy implements ConfigurationResourceResolvingStrategy {

    @ObjectClassDefinition(name="Apache Sling Context-Aware Default Configuration Resource Resolving Strategy",
                           description="Standardized access to configurations in the resource tree.")
    static @interface Config {

        @AttributeDefinition(name="Enabled",
                description = "Enable this configuration resourcer resolving strategy.")
        boolean enabled() default true;
        
        @AttributeDefinition(name="Allowed paths",
                             description = "Whitelist of paths where configurations can reside in.")
        String[] allowedPaths() default {"/conf", "/apps/conf", "/libs/conf"};

        @AttributeDefinition(name="Fallback paths",
                description = "Global fallback configurations, ordered from most specific (checked first) to least specific.")
        String[] fallbackPaths() default {"/conf/global", "/apps/conf", "/libs/conf"};
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile Config config;
    
    @Reference
    private ContextPathStrategyMultiplexer contextPathStrategy;

    Config getConfiguration() {
        return this.config;
    }

    @Activate
    private void activate(final Config config) {
        this.config = config;
    }

    @Deactivate
    private void deactivate() {
        this.config = null;
    }

    @SuppressWarnings("unchecked")
    Iterator<String> getResolvePaths(final Resource contentResource) {
        return new IteratorChain(
            // add all config references found in resource hierarchy
            findConfigRefs(contentResource),
            // finally add the global fallbacks
            new ArrayIterator(this.config.fallbackPaths())
        );
    }

    /**
     * Check the name.
     * A name must not be null and relative.
     * @param name The name
     * @return {@code true} if it is valid
     */
    private boolean checkName(final String name) {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.contains("../") ) {
            return false;
        }
        return true;
    }

    /**
     * Searches the resource hierarchy upwards for all config references and returns them.
     * @param refs List to add found resources to
     * @param startResource Resource to start searching
     */
    @SuppressWarnings("unchecked")
    private Iterator<String> findConfigRefs(final Resource startResource) {
        Iterator<Resource> contextResources = contextPathStrategy.findContextResources(startResource);
        // get config resource path for each context resource, filter out items where not reference could be resolved
        return new FilterIterator(new TransformIterator(contextResources, new Transformer() {
                @Override
                public Object transform(Object input) {
                    return getReference((Resource)input);
                }
            }), PredicateUtils.notNullPredicate());
    }

    private String getReference(final Resource resource) {
        String ref = resource.getValueMap().get(DefaultContextPathStrategy.PROPERTY_CONFIG, String.class);

        if (ref != null) {
            // if absolute path found we are (probably) done
            if (ref.startsWith("/")) {
                // combine full path if relativeRef is present
                ref = ResourceUtil.normalize(ref);

                if (ref != null && !isAllowedConfigPath(ref)) {
                    logger.warn("Ignoring reference to {} from {} - not in allowed paths.", ref, resource.getPath());
                    ref = null;
                }

                if (ref != null && isFallbackConfigPath(ref)) {
                    logger.warn("Ignoring reference to {} from {} - already a fallback path.", ref, resource.getPath());
                    ref = null;
                }

            } else {
                logger.error("Invalid relative reference found for {} : {}. This entry is ignored", resource.getPath(), ref);
            }
        }
        
        if (ref != null) {
            logger.trace("Reference '{}' found at {}", ref, resource.getPath());
        }

        return ref;
    }

    private boolean isAllowedConfigPath(String path) {
        for (String pattern : this.config.allowedPaths()) {
            if (logger.isTraceEnabled()) {
                logger.trace("- checking if '{}' starts with {}", path, pattern);
            }
            if (path.equals(pattern) || path.startsWith(pattern + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isFallbackConfigPath(final String ref) {
        for(final String name : this.config.fallbackPaths()) {
            if ( name.equals(ref) ) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isEnabledAndParamsValid(final Resource contentResource, final String bucketName, final String configName) {
        return config.enabled() && contentResource != null && checkName(bucketName) && checkName(configName);
    }
    
    private String buildResourcePath(String path, String name) {
        return ResourceUtil.normalize(path + "/" + name);
    }

    @Override
    public Resource getResource(final Resource contentResource, final String bucketName, final String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketName, configName)) {
            return null;
        }
        String name = bucketName + "/" + configName;
        logger.debug("Searching {} for resource {}", name, contentResource.getPath());

        // strategy: find first item among all configured paths
        int idx = 1;
        Iterator<String> paths = getResolvePaths(contentResource);
        while (paths.hasNext()) {
            final String path = paths.next();
            final Resource item = contentResource.getResourceResolver().getResource(buildResourcePath(path, name));
            if (item != null) {
                logger.debug("Resolved config item at [{}]: {}", idx, item.getPath());

                return item;
            }
            idx++;
        }

        logger.debug("Could not resolve any config item for '{}' (or no permissions to read it)", name);

        // nothing found
        return null;
    }

    @Override
    public Collection<Resource> getResourceCollection(final Resource contentResource, final String bucketName, final String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketName, configName)) {
            return Collections.emptyList();
        }
        String name = bucketName + "/" + configName;
        if (logger.isTraceEnabled()) {
            logger.trace("- searching for list '{}'", name);
        }

        final Set<String> names = new HashSet<>();
        final List<Resource> result = new ArrayList<>();
        int idx = 1;
        Iterator<String> paths = getResolvePaths(contentResource);
        while (paths.hasNext()) {
            final String path = paths.next();
            Resource item = contentResource.getResourceResolver().getResource(buildResourcePath(path, name));
            if (item != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("+ resolved config item at [{}]: {}", idx, item.getPath());
                }

                for (Resource child : item.getChildren()) {
                    if ( !child.getName().contains(":") && !names.contains(child.getName()) ) {
                        result.add(child);
                        names.add(child.getName());
                    }
                }

            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("- no item '{}' under config '{}'", name, path);
                }
            }
            idx++;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("- final list has {} items", result.size());
        }

        return result;
    }
    
    @Override
    public String getResourcePath(Resource contentResource, String bucketName, String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketName, configName)) {
            return null;
        }
        String name = bucketName + "/" + configName;

        Iterator<String> configPaths = this.findConfigRefs(contentResource);
        if (configPaths.hasNext()) {
            String configPath = buildResourcePath(configPaths.next(), name);
            logger.debug("Building configuration path {} for resource {}: {}", name, contentResource.getPath(), configPath);
            return configPath;
        }
        else {
            logger.debug("No configuration path {}  foundfor resource {}.", name, contentResource.getPath());
            return null;
        }
    }

    @Override
    public String getResourceCollectionParentPath(Resource contentResource, String bucketName, String configName) {
        if (!isEnabledAndParamsValid(contentResource, bucketName, configName)) {
            return null;
        }
        String name = bucketName + "/" + configName;

        Iterator<String> configPaths = this.findConfigRefs(contentResource);
        if (configPaths.hasNext()) {
            String configPath = buildResourcePath(configPaths.next(), name);
            logger.debug("Building configuration collection parent path {} for resource {}: {}", name, contentResource.getPath(), configPath);
            return configPath;
        }
        else {
            logger.debug("No configuration collection parent path {}  foundfor resource {}.", name, contentResource.getPath());
            return null;
        }
    }

}
