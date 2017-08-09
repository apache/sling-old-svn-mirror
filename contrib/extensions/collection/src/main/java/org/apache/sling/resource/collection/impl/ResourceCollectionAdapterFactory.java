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

package org.apache.sling.resource.collection.impl;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resource.collection.ResourceCollection;
import org.apache.sling.resource.collection.ResourceCollectionManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdapterFactory that adapts Resources to: {@link ResourceCollection}
 * And ResourceResolver to: {@link ResourceCollectionManager)
 */
@Component(service = AdapterFactory.class,
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
            Constants.SERVICE_DESCRIPTION + "=Apache Sling Resource Collection Adapter Factory",
            "adapters=org.apache.sling.resource.collection.ResourceCollection",
            "adapters=org.apache.sling.resource.collection.ResourceCollectionManager",
            "adaptables=org.apache.sling.api.resource.Resource",
            "adaptables=org.apache.sling.api.resource.ResourceResolver"
    })
public class ResourceCollectionAdapterFactory implements AdapterFactory {

    private final Logger log = LoggerFactory.getLogger(ResourceCollectionAdapterFactory.class);

    private static final Class<ResourceCollection> COLLECTION_CLASS = ResourceCollection.class;

    private static final Class<ResourceCollectionManager> COLLECTION_MGR_CLASS = ResourceCollectionManager.class;

    @Reference
    private ResourceCollectionManager collectionManager;

    // ---------- AdapterFactory -----------------------------------------------

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {
        if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        } else if (adaptable instanceof ResourceResolver) {
            return getAdapter((ResourceResolver) adaptable, type);
        } else {
            log.warn("Unable to handle adaptable {}",
                adaptable.getClass().getName());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(Resource resource,
            Class<AdapterType> type) {
        if (resource != null) {
            if (type == COLLECTION_CLASS) {
                if (resource.isResourceType(ResourceCollection.RESOURCE_TYPE)) {
                    return (AdapterType) new ResourceCollectionImpl(resource);
                }
            }
            log.debug("Unable to adapt resource of {} to type {}",
                resource.getResourceType(), type.getName());

        }
        log.debug("Unable to adapt null resource to type {}", type.getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(ResourceResolver resolver,
            Class<AdapterType> type) {
        if (COLLECTION_MGR_CLASS == type) {
            return (AdapterType) collectionManager;
        } else {
            log.warn("Unable to adapt resolver to requested type {}",
                type.getName());
            return null;
        }
    }
}
