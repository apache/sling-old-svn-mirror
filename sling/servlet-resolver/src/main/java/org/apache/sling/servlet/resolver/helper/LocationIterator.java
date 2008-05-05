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
package org.apache.sling.servlet.resolver.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlet.resolver.ServletResolverConstants;

public class LocationIterator implements Iterator<String> {

    private final Resource resource;

    private final ResourceResolver resolver;

    private final String[] searchPath;

    private String resourceType;

    private String relPath;

    private int pathCounter;

    private String nextLocation;

    public LocationIterator(SlingHttpServletRequest request) {
        resource = request.getResource();
        resolver = request.getResourceResolver();

        String[] tmpPath = resolver.getSearchPath();
        if (tmpPath == null || tmpPath.length == 0) {
            tmpPath = new String[] { "/" };
        } else {
            for (int i=0; i< tmpPath.length; i++) {
                if (!tmpPath[i].endsWith("/")) {
                    tmpPath[i] = tmpPath[i].concat("/");
                }
            }
        }
        searchPath = tmpPath;

        resourceType = resource.getResourceType();

        nextLocation = seek();
    }

    public boolean hasNext() {
        return nextLocation != null;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String result = nextLocation;
        nextLocation = seek();
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private String seek() {

        if (relPath == null) {

            if (resourceType == null) {
                return null;
            }
            
            String typePath = JcrResourceUtil.resourceTypeToPath(resourceType);
            if (typePath.startsWith("/")) {
                resourceType = getResourceSuperType();
                return typePath;
            }

            relPath = typePath;
        }

        String result = searchPath[pathCounter].concat(relPath);

        pathCounter++;
        if (pathCounter >= searchPath.length) {
            relPath = null;
            resourceType = getResourceSuperType();
            pathCounter = 0;
        }

        return result;
    }

    private String getResourceSuperType() {

        // if the current resource type is the default value, there are no more
        if (resourceType == ServletResolverConstants.DEFAULT_SERVLET_NAME) {
            return null;
        }

        // get the super type of the current resource type
        String superType;
        if (resourceType == resource.getResourceType()) {
            superType = resource.getResourceSuperType();
        } else {
            superType = JcrResourceUtil.getResourceSuperType(resolver,
                resourceType);
        }

        // use default resource type if there is no super type any more
        if (superType == null) {
            superType = ServletResolverConstants.DEFAULT_SERVLET_NAME;
        }

        return superType;
    }

}
