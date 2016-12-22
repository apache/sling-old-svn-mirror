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

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.resource.spi.ContextPathStrategy;
import org.apache.sling.caconfig.resource.spi.ContextResource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ContextPathStrategy.class)
@Designate(ocd=DefaultContextPathStrategy.Config.class)
public class DefaultContextPathStrategy implements ContextPathStrategy {

    @ObjectClassDefinition(name="Apache Sling Context-Aware Configuration Default Context Path Strategy",
            description="Detects context path by existence of " + PROPERTY_CONFIG_REF + " properties.")
    public static @interface Config {

        @AttributeDefinition(name="Enabled",
                      description = "Enable this context path strategy.")
        boolean enabled() default true;

        @AttributeDefinition(name="Config ref. resource names",
                description = "Names of resource to try to look up " + PROPERTY_CONFIG_REF + " property in. If list is empty only current resource is checked." +
                              " If the list is not empty than only those listed resources are used for look up. If you want to include the current resource you can use a dot for the value.")
        String[] configRefResourceNames();

        @AttributeDefinition(name="Config ref. property names",
                description = "Additional property names to " + PROPERTY_CONFIG_REF + " to look up a configuration reference. The names are used in the order defined, "
                            + "always starting with " + PROPERTY_CONFIG_REF + ". Once a property with a value is found, that value is used and the following property names are skipped.")
        String[] configRefPropertyNames();
    }

    private static final Logger log = LoggerFactory.getLogger(DefaultContextPathStrategy.class);

    private volatile Config config;

    @Activate
    private void activate(Config config) {
        this.config = config;
    }

    @Override
    public Iterator<ContextResource> findContextResources(Resource resource) {
        if (!config.enabled()) {
            return Collections.emptyIterator();
        }
        return new ConfigResourceIterator(resource);
    }

    /**
     * Searches the resource hierarchy upwards for all context and returns the root resource for each of them.
     */
    private class ConfigResourceIterator implements Iterator<ContextResource> {

        private ContextResource next;

        public ConfigResourceIterator(Resource startResource) {
            next = findNextContextResource(startResource);
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ContextResource next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            ContextResource result = next;
            next = findNextContextResource(next.getResource().getParent());
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Find next configuration context root for given resource.
         * @param startResource Resource to start searching
         * @return Next resource with sling:configRef property or null if none found.
         */
        private ContextResource findNextContextResource(Resource startResource) {
            // start at resource, go up
            Resource resource = startResource;

            while (resource != null) {
                String configRef = getConfigRef(resource);
                if (configRef != null) {
                    log.trace("+ Found context path {}, configRef {}", resource.getPath(), configRef);
                    return new ContextResource(resource, configRef);
                }
                // if getParent() returns null, stop
                resource = resource.getParent();
            }

            // if hit root and nothing found, return null
            return null;
        }

        private String getConfigRefValue(final Resource resource) {
            final ValueMap map = resource.getValueMap();
            String val = map.get(PROPERTY_CONFIG_REF, String.class);
            if ( val == null && !ArrayUtils.isEmpty(config.configRefPropertyNames()) ) {
                for(final String name : config.configRefPropertyNames()) {
                    val = map.get(name, String.class);
                    if ( val != null ) {
                        break;
                    }
                }
            }
            return val;
        }

        private String getConfigRef(final Resource resource) {
            if (ArrayUtils.isEmpty(config.configRefResourceNames())) {
                return getConfigRefValue(resource);
            }
            for (final String name : config.configRefResourceNames()) {
                final Resource lookupResource = resource.getChild(name);
                if (lookupResource != null) {
                    String configRef = getConfigRefValue(lookupResource);
                    if (configRef != null) {
                        return configRef;
                    }
                }
            }
            return null;
        }
    }

}
