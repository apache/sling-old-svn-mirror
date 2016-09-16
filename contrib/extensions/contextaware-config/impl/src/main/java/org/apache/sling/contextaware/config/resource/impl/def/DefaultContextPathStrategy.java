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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.spi.ContextPathStrategy;
import org.osgi.service.component.ComponentContext;
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

    @ObjectClassDefinition(name="Apache Sling Context-Aware Default Context Path Strategy",
            description="Detects context path by existence of " + PROPERTY_CONFIG + " properties.")
    static @interface Config {
        
        @AttributeDefinition(name="Enabled",
                      description = "Enable this context path strategy.")
        boolean enabled() default true;
        
    }
    
    /**
     * Property that points to the configuration to be used.
     * Additionally each resource having this property marks the beginning of a new context sub-tree.
     */
    public static final String PROPERTY_CONFIG = "sling:config-ref";
   
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private volatile Config config;
    
    @Activate
    private void activate(ComponentContext componentContext, Config config) {
        this.config = config; 
    }
    
    @Override
    public Iterator<Resource> findContextResources(Resource resource) {
        if (!config.enabled()) {
            return Collections.emptyIterator();
        }
        return new ConfigResourceIterator(resource);
    }
    
    /**
     * Searches the resource hierarchy upwards for all context and returns the root resource for each of them.
     */
    private class ConfigResourceIterator implements Iterator<Resource> {

        private Resource next;
        
        public ConfigResourceIterator(Resource startResource) {
            next = findNextContextResource(startResource);
        }
        
        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Resource next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Resource result = next;
            next = findNextContextResource(next.getParent());
            return result;
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Find next configuration context root for given resource.
         * @param startResource Resource to start searching
         * @return Next resource with sling:config-ref property or null if none found.
         */
        private Resource findNextContextResource(Resource startResource) {
            // start at resource, go up
            Resource resource = startResource;
            
            while (resource != null) {
                if (hasConfigRef(resource)) {
                    log.trace("Found context path '{}'.", resource.getPath());
                    return resource;
                }
                // if getParent() returns null, stop
                resource = resource.getParent();
            }

            // if hit root and nothing found, return null
            return null;
        }
        
        private boolean hasConfigRef(final Resource resource) {
            return resource.getValueMap().get(PROPERTY_CONFIG, String.class) != null;
        }

    }

}
