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
package org.apache.sling.contextaware.config.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.spi.ContextPathStrategy;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ContextPathStrategy.class,
    property=ContextPathStrategy.ROOTS + "=/")
public class DefaultContextPathStrategy implements ContextPathStrategy {

    /**
     * Property that points to the configuration to be used.
     * Additionally each resource having this property marks the beginning of a new context sub-tree.
     */
    public static final String PROPERTY_CONFIG = "sling:config-ref";
   
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public Collection<Resource> findContextPaths(Resource resource) {
        List<Resource> resources = new ArrayList<>();
        findConfigRefs(resources, resource);
        return resources;
    }
    
    /**
     * Searches the resource hierarchy upwards for all context and returns the root resource for each of them.
     * @param refs List to add found resources to
     * @param startResource Resource to start searching
     */
    private void findConfigRefs(final List<Resource> resources, final Resource startResource) {
        Resource resource = findNextContextResource(startResource);
        if (resource != null) {
            resources.add(resource);
            findConfigRefs(resources, resource.getParent());
        }
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
