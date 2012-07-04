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
package org.apache.sling.samples.pathbasedrtp;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceWrapper;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Default resource type provider that uses a component of the node path
 *  to define the default resource type.
 *
 *  A number of mappings can be configured, for example "/content:2" would
 *  cause a node at /content/foo/bar to get the "foo" resource type if it
 *  doesn't have a default one: "/content" is used to select nodes to which the
 *  mapping applies, and "2" is the (1-based) index of the path component to
 *  use as the resource type.
 *
 */
@Component(metatype=true, label="%defaultRtp.name", description="%defaultRtp.description")
@Service
@Property(name="service.description", value="Sling Sample Resource Decorator")
public class PathBasedResourceDecorator implements ResourceDecorator {

    /**
     *  Name of the configurable property name that defines mappings. The default values
     *  specify the use of path component 2 for the /content path, and add a similar
     *  definition for the /sling-test-pbrt path that is used in integration testing.
     *
     */
    @Property(value={"/content:2", "/sling-test-pbrt:2"}, unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_PATH_MAPPING = "path.mapping";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Mapping [] mappings;

    /**
     * @see org.apache.sling.api.resource.ResourceDecorator#decorate(org.apache.sling.api.resource.Resource, javax.servlet.http.HttpServletRequest)
     */
    public Resource decorate(Resource resource, HttpServletRequest request) {
        return this.decorate(resource);
    }

    /** Return a resource type for given node, if we have a mapping that applies */
    public Resource decorate(Resource resource) {
        String result = null;

        if (mappings!=null) {
            // let's check when we should apply the mapping
            // 1. if the resource is a star resource
            boolean apply = false;
            String resourceType = null;
            if ( resource.getPath().endsWith("/*") ) {
                apply = true;
                resourceType = Mapping.DEFAULT_NODE_TYPE;
            } else {
                // 2. if the resource is adaptable to a node
                //    and the primary node type equals the resource type
                try {
                    final Node node = resource.adaptTo(Node.class);
                    if ( node != null && node.getPrimaryNodeType().getName().equals(resource.getResourceType()) ) {
                        apply = true;
                        resourceType = resource.getResourceType();
                    }
                } catch (RepositoryException re) {
                    // we ignore this
                }
            }
            if ( apply ) {
                final String path = resource.getPath();
                for(Mapping m : mappings) {
                    result = m.getResourceType(path, resourceType);
                    if (result != null) {
                        log.debug("Default resource type {} used for resource {}", result, path);
                        break;
                    }
                }
            }
        }

        if (result==null && log.isDebugEnabled()) {
            log.debug("No Mapping applies to node {}, no resource type provided", resource.getPath());
        }
        if ( result != null ) {
            final String resourceType = result;
            return new ResourceWrapper(resource) {

                @Override
                public String getResourceType() {
                    return resourceType;
                }

            };
        }
        return resource;
    }

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
        final Dictionary<?, ?> properties = componentContext.getProperties();
        final String[] mappingList = (String[]) properties.get(PROP_PATH_MAPPING);

        if(mappingList== null || mappingList.length == 0) {
            mappings = null;
        } else {
            mappings = new Mapping[mappingList.length];
            for(int i=0; i < mappingList.length; i++) {
                mappings[i] = new Mapping(mappingList[i]);
                log.debug("Added {}", mappings[i]);
            }
        }
    }
}