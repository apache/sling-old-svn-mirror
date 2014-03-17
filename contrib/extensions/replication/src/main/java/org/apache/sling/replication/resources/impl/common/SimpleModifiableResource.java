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
package org.apache.sling.replication.resources.impl.common;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;

/**
 * Resource that can be adapted to a ModifiableValueMap
 */
public class SimpleModifiableResource extends SimpleReadableResource {

    private final AbstractModifyingResourceProvider resourceProvider;

    public SimpleModifiableResource(ResourceResolver resourceResolver, AbstractModifyingResourceProvider resourceProvider,
                                    String resourcePath,
                                    Map<String, Object> properties) {
        super(resourceResolver, resourcePath, properties);
        this.resourceProvider = resourceProvider;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

        if (type == ValueMap.class || type == Map.class) {
            return (AdapterType) new SimpleModifiableValueMap(getResourceResolver(), resourceProvider, this);

        } else if (type == ModifiableValueMap.class) {
            return (AdapterType) new SimpleModifiableValueMap(getResourceResolver(), resourceProvider, this);
        }

        return super.adaptTo(type);
    }
}
