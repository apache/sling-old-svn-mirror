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
package org.apache.sling.servlets.resolver.internal.helper;

import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.resolver.internal.ServletResolverConstants;

/**
 * The <code>ResourceCollector</code> class provides a single public method -
 * {@link #getServlets(ResourceResolver)} - which is used to find an ordered collection
 * of <code>Resource</code> instances which may be used to find a servlet or
 * script to handle a request to the given resource.
 */
public class NamedScriptResourceCollector extends AbstractResourceCollector {

    private final String scriptName;

    public static NamedScriptResourceCollector create(final String name,
            final Resource resource,
            final String[] executionPaths) {
        final String resourceType;
        final String resourceSuperType;
        final String baseResourceType;
        final String extension;
        final String scriptName;
        if ( resource != null ) {
            resourceType = resource.getResourceType();
            resourceSuperType = resource.getResourceSuperType();
            baseResourceType = ServletResolverConstants.DEFAULT_SERVLET_NAME;
        } else {
            resourceType = "";
            resourceSuperType = null;
            baseResourceType = "";
        }
        scriptName = name;
        final int pos = name.lastIndexOf('.');
        if ( pos == -1 ) {
            extension = null;
        } else {
            extension = name.substring(pos);
        }
        return new NamedScriptResourceCollector(baseResourceType,
                resourceType,
                resourceSuperType,
                scriptName,
                extension,
                executionPaths);
    }

    public NamedScriptResourceCollector(final String baseResourceType,
                              final String resourceType,
                              final String resourceSuperType,
                              final String scriptName,
                              final String extension,
                              final String[] executionPaths) {
        super(baseResourceType, resourceType, resourceSuperType, null, extension, executionPaths);
        this.scriptName = scriptName;
        // create the hash code once
        final String key = baseResourceType + ':' + this.scriptName + ':' +
            this.resourceType + ':' + (this.resourceSuperType == null ? "" : this.resourceSuperType) +
            ':' + (this.extension == null ? "" : this.extension);
        this.hashCode = key.hashCode();
    }

    protected void getWeightedResources(final Set<Resource> resources,
                                        final Resource location) {
        final ResourceResolver resolver = location.getResourceResolver();
        // if extension is set, we first check for an exact script match
        if ( this.extension != null ) {
            final String path = ResourceUtil.normalize(location.getPath() + '/' + this.scriptName);
            if ( this.isPathAllowed(path) ) {
                final Resource current = resolver.getResource(path);
                if ( current != null ) {
                    this.addWeightedResource(resources, current, 0, WeightedResource.WEIGHT_EXTENSION);
                }
            }
        }
        // if the script name denotes a path we have to get the denoted resource
        // first
        final Resource current;
        final String name;
        final int pos = this.scriptName.lastIndexOf('/');
        if ( pos == -1 ) {
            current = location;
            name = this.scriptName;
        } else {
            current = getResource(resolver, location.getPath() + '/' + this.scriptName.substring(0, pos));
            name = this.scriptName.substring(pos + 1);
        }
        final Iterator<Resource> children = resolver.listChildren(current);
        while (children.hasNext()) {
            final Resource child = children.next();

            if ( !this.isPathAllowed(child.getPath()) ) {
                continue;
            }
            final String currentScriptName = ResourceUtil.getName(child);
            final int lastDot = currentScriptName.lastIndexOf('.');
            if (lastDot < 0) {
                // no extension in the name, this is not a script
                continue;
            }

            if ( currentScriptName.substring(0, lastDot).equals(name) ) {
                this.addWeightedResource(resources, child, 0, WeightedResource.WEIGHT_PREFIX);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof NamedScriptResourceCollector) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        if ( super.equals(obj) ) {
            final NamedScriptResourceCollector o = (NamedScriptResourceCollector)obj;
            if ( stringEquals(scriptName, o.scriptName)) {
                return true;
            }
        }
        return false;
    }
}
