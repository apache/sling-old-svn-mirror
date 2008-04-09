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
package org.apache.sling.jcr.resource.internal.helper.starresource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;

/** Used to provide the equivalent of an empty Node for GET requests
 *  to *.something (SLING-344)
 */
public class StarResource extends SyntheticResource {
    
    public final static String PATH_PATTERN = "/*.";
    public final static String PATH_CLEAN_SUFFIX = "/*";
    public final static String DEFAULT_RESOURCE_TYPE = "sling:syntheticStarResource";
    
    @SuppressWarnings("serial")
    static class SyntheticStarResourceException extends SlingException {
        SyntheticStarResourceException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
    
    /** True if a StarResource should be used for the given request, if
     *  a real Resource was not found */
    public static boolean appliesTo(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path.contains(PATH_PATTERN) || path.endsWith(PATH_CLEAN_SUFFIX);
    }

    /**
     * Returns true if the path of the resource ends with the
     * {@link #PATH_CLEAN_SUFFIX} and therefore should be considered a star
     * resource.
     */
    public static boolean isStarResource(Resource res) {
        return res.getPath().endsWith(PATH_CLEAN_SUFFIX);
    }
    
    public StarResource(ResourceResolver resourceResolver, String path, JcrResourceTypeProvider drtp) throws SlingException {
        super(resourceResolver, convertPath(path), null);
        
        // The only way we can set a meaningful resource type is via the drtp
        final Node n = new FakeNode(getPath());
        String resourceType = null;
        if(drtp != null) {
            try {
                resourceType = drtp.getResourceTypeForNode(n);
            } catch(RepositoryException re) {
                throw new SyntheticStarResourceException("getResourceTypeForNode failed", re);
            }
        }
        if(resourceType == null) {
            resourceType = DEFAULT_RESOURCE_TYPE;
        }
        setResourceType(resourceType);
    }

    /** adaptTo(Node) returns a Fake node, that returns empty values
     *  for everything except the Node path */
    @SuppressWarnings("unchecked")
    @Override
    public <Type> Type adaptTo(Class<Type> type) {
        if(type == Node.class) {
            return (Type) new FakeNode(getPath());
        }
        return null;
    }
    
    /** Cleanup our path, for example /foo/*.html becomes /foo/* */
    protected static String convertPath(String path) {
        final int index = path.indexOf(PATH_PATTERN);
        if(index >= 0) {
            return path.substring(0, index) + PATH_CLEAN_SUFFIX;
        }
        return path;
    }
            
}
