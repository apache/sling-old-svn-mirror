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

import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
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
 * @scr.component immediate="true" label="%defaultRtp.name"
 *                description="%defaultRtp.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling Default Resource Type Provider"
 * @scr.service
 */
public class DefaultResourceTypeProvider implements JcrResourceTypeProvider {

    /**
     *  Name of the configurable property name that defines mappings. The default values
     *  specify the use of path component 2 for the /content path, and add a similar
     *  definition for the /sling-test-pbrt path that is used in integration testing.     
     *  
     * @scr.property 
     *  values.1="/content:2" 
     *  values.2="/sling-test-pbrt:2"
     */
    private static final String PROP_PATH_MAPPING = "path.mapping";
    
    private static final Logger log = LoggerFactory.getLogger(DefaultResourceTypeProvider.class);
    
    /** Map a path prefix to a (1-based) index in the path components */
    static class Mapping {
        String path;
        int resourceTypeIndex;
        
        Mapping(String definition) {
            final String [] parts = definition.split(":");
            if(parts.length != 2) {
                log.debug("Invalid Mapping definition ignored: {}", definition);
            } else {
                path = parts[0];
                try {
                    resourceTypeIndex = Integer.parseInt(parts[1]);
                } catch(Exception e) {
                    log.warn("Invalid path index in Mapping {}", definition);
                }
            }
        }
        
        @Override
        public String toString() {
           return "Mapping: path=" + path + ", resource type index=" + resourceTypeIndex; 
        }
    }
    
    private Mapping [] mappings;
    
    /** Return a resource type for given node, if we have a mapping that applies */
    public String getResourceTypeForNode(Node node) throws RepositoryException {
        String result = null;
        
        if(mappings!=null) {
            final String nt = node.getPrimaryNodeType().getName();
            final String path = node.getPath();
            for(Mapping m : mappings) {
                if(path.startsWith(m.path) && "nt:unstructured".equals(nt)) {
                    final String [] paths = node.getPath().split("/");
                    if(paths.length >= m.resourceTypeIndex+1) {
                        result = paths[m.resourceTypeIndex];
                        log.debug("Default resource type {} used for Node {}", 
                                result, path);
                        break;
                    }
                }
            }
        }
        
        if(result==null) {
            log.debug("No Mapping applies to node {}, no resource type provided", node.getPath());
        }
        
        return result;
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
