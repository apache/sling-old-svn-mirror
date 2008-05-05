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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.servlet.resolver.resource.ServletResourceProvider;

public class LocationUtil {

    private final ResourceResolver resolver;

    private final String methodName;

    public static LocationUtil create(SlingHttpServletRequest request) {
        if (HttpConstants.METHOD_GET.equals(request.getMethod())
            || HttpConstants.METHOD_HEAD.equals(request.getMethod())) {
            return new LocationUtilGet(request);
        }

        return new LocationUtil(request);
    }

    protected LocationUtil(SlingHttpServletRequest request) {
        resolver = request.getResourceResolver();
        methodName = request.getMethod();
    }

    public Collection<LocationResource> getScripts(
            SlingHttpServletRequest request) {

        SortedSet<LocationResource> resources = new TreeSet<LocationResource>();

        int ordinal = 0;

        Iterator<String> locations = new LocationIterator(request);
        while (locations.hasNext()) {
            String location = locations.next();

            // get the location resource, use a synthetic resource if there
            // is no real location. There may still be children at this
            // location
            Resource locationRes = getResource(null, location);
            ordinal = getLocationResources(ordinal, locationRes, resources);
        }

        return resources;
    }

    protected int getLocationResources(int ordinal, Resource location,
            Set<LocationResource> resources) {

        // now list the children and check them
        Iterator<Resource> children = getResolver().listChildren(location);
        while (children.hasNext()) {
            Resource child = children.next();

            String name = ResourceUtil.getName(child.getPath());
            String[] parts = name.split("\\.");

            // require method name plus script extension
            if (parts.length == 2 && getMethodName().equals(parts[0])) {
                LocationResource lr = new LocationResource(ordinal, child, 0,
                    LocationResource.WEIGHT_NONE);
                resources.add(lr);
                ordinal++;
            }
        }

        // special treatment for servlets registered with neither a method
        // name nor extensions and selectors
        String path = location.getPath()
            + ServletResourceProvider.SERVLET_PATH_EXTENSION;
        location = getResolver().getResource(path);
        if (location != null) {
            LocationResource lr = new LocationResource(ordinal, location, 0,
                LocationResource.WEIGHT_LAST_RESSORT);
            resources.add(lr);
            ordinal++;
        }

        return ordinal;
    }

    public String getMethodName() {
        return methodName;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    protected Resource getResource(Resource base, String path) {
        Resource res;
        if (base == null) {
            res = getResolver().getResource(path);
        } else {
            res = getResolver().getResource(base, path);
        }

        if (res == null) {
            if (!path.startsWith("/")) {
                if (base == null) {
                    path = "/".concat(path);
                } else {
                    path = base.getPath() + "/" + path;
                }
            }

            res = new SyntheticResource(resolver, path, "$synthetic$");
        }

        return res;
    }
}
