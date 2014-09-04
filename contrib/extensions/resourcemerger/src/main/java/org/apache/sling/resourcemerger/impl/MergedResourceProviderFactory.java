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
package org.apache.sling.resourcemerger.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.api.ResourceMergerService;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;

@Component(label = "Apache Sling Merged Resource Provider Factory",
           description = "This resource provider delivers merged resources based on the search paths.",
           metatype=true)
@Service
@Property(name=MergedResourcePicker.MERGE_ROOT, value=MergedResourceProviderFactory.DEFAULT_ROOT,
    label="Root",
    description="The mount point of merged resources")
/**
 * The <code>MergedResourceProviderFactory</code> creates merged resource
 * providers and implements the <code>ResourceMergerService</code>.
 */
public class MergedResourceProviderFactory implements MergedResourcePicker, ResourceMergerService {

    public static final String DEFAULT_ROOT = "/mnt/overlay";

    private String mergeRootPath;

    public Iterator<Resource> pickResources(ResourceResolver resolver, String relativePath) {
        List<Resource> resources = new ArrayList<Resource>();
        final String[] searchPaths = resolver.getSearchPath();
        for (int i = searchPaths.length - 1; i >= 0; i--) {
            final String basePath = searchPaths[i];
            final String fullPath = basePath + relativePath;
            Resource resource = resolver.getResource(fullPath);
            if (resource != null) {
                resources.add(resource);
            } else {
                resources.add(new NonExistingResource(resolver, fullPath));
            }
        }
        return resources.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public String getMergedResourcePath(final String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Provided relative path is null");
        }

        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Provided path is not a relative path");
        }

        return mergeRootPath + "/" + relativePath;
    }

    /**
     * {@inheritDoc}
     */
    public Resource getMergedResource(final Resource resource) {
        if (resource != null) {
            final ResourceResolver resolver = resource.getResourceResolver();
            final String[] searchPaths = resolver.getSearchPath();
            for (final String searchPathPrefix : searchPaths) {
                if (resource.getPath().startsWith(searchPathPrefix)) {
                    final String searchPath = searchPathPrefix.substring(0, searchPathPrefix.length() - 1);
                    return resolver.getResource(resource.getPath().replaceFirst(searchPath, mergeRootPath));
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMergedResource(final Resource resource) {
        if (resource == null) {
            return false;
        }

        return Boolean.TRUE.equals(resource.getResourceMetadata().get(MergedResourceConstants.METADATA_FLAG));
    }

    /**
     * {@inheritDoc}
     */
    public String getResourcePath(final String searchPath, final String mergedResourcePath) {
        if( searchPath == null || !searchPath.startsWith("/") || !searchPath.endsWith("/") ) {
            throw new IllegalArgumentException("Provided path is not a valid search path: " + searchPath);
        }
        if ( mergedResourcePath == null || !mergedResourcePath.startsWith(this.mergeRootPath + "/") ) {
            throw new IllegalArgumentException("Provided path does not point to a merged resource: " + mergedResourcePath);
        }
        return searchPath + mergedResourcePath.substring(this.mergeRootPath.length() + 1);
    }

    @Activate
    protected void configure(final Map<String, Object> properties) {
        mergeRootPath = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), DEFAULT_ROOT);
    }
}
