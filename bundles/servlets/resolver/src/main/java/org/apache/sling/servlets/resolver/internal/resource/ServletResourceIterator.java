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
package org.apache.sling.servlets.resolver.internal.resource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.SyntheticResource;

class ServletResourceIterator implements Iterator<Resource> {

    private final ServletResourceProvider provider;

    private final Resource parentResource;

    private Iterator<String> pathIter;

    private String parentPath;

    private Resource next;

    private Set<String> visited;

    ServletResourceIterator(ServletResourceProvider provider, Resource parent) {
        this.provider = provider;
        this.parentResource = parent;

        pathIter = provider.getServletPathIterator();
        parentPath = parent.getPath();
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath.concat("/");
        }
        visited = new HashSet<String>();
        next = seek();
    }

    public boolean hasNext() {
        return next != null;
    }

    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Resource result = next;
        next = seek();
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    private Resource seek() {
        while (pathIter.hasNext()) {
            String path = pathIter.next();
            if (path.startsWith(parentPath)) {
                int nextSlash = path.indexOf('/', parentPath.length());
                if (nextSlash < 0) {
                    return new ServletResource(
                        parentResource.getResourceResolver(),
                        provider.getServlet(), path);
                }

                path = path.substring(0, nextSlash);
                if (!visited.contains(path)) {
                    visited.add(path);
                    Resource res =  parentResource.getResourceResolver().getResource(path);
                    if (res == null) {
                        res = new SyntheticResource(
                            parentResource.getResourceResolver(), path,
                            ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
                    }
                    return res;
                }
            }
        }

        return null;
    }

}
