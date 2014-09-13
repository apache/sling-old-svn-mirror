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

import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;

/**
 * {@inheritDoc}
 */
public class CRUDMergedResource extends MergedResource {

    private final MergedResourcePicker picker;

    private final String relativePath;

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath   Merge root path
     * @param relativePath    Relative path
     * @param mappedResources List of physical mapped resources' paths
     */
    CRUDMergedResource(final ResourceResolver resolver,
                   final String mergeRootPath,
                   final String relativePath,
                   final List<Resource> mappedResources,
                   final List<ValueMap> valueMaps,
                   final MergedResourcePicker picker) {
        super(resolver, mergeRootPath, relativePath, mappedResources, valueMaps);
        this.picker = picker;
        this.relativePath = relativePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ModifiableValueMap.class) {
            final Iterator<Resource> iter = this.picker.pickResources(this.getResourceResolver(), this.relativePath);
            Resource highestRsrc = null;
            while ( iter.hasNext() ) {
                highestRsrc = iter.next();
            }
            if ( ResourceUtil.isNonExistingResource(highestRsrc) ) {
                final String paths[] = (String[])this.getResourceMetadata().get(MergedResourceConstants.METADATA_RESOURCES);

                final Resource copyResource = this.getResourceResolver().getResource(paths[paths.length - 1]);
                try {
                    final Resource newResource = ResourceUtil.getOrCreateResource(this.getResourceResolver(), highestRsrc.getPath(), copyResource.getResourceType(), null, false);
                    return (AdapterType)newResource.adaptTo(ModifiableValueMap.class);
                } catch ( final PersistenceException pe) {
                    // we ignore this for now
                    return null;
                }
            }
            return (AdapterType)highestRsrc.adaptTo(ModifiableValueMap.class);
        }
        return super.adaptTo(type);
    }
}
