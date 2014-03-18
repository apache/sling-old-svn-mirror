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
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

/**
 * Read only resource that can adapt to one of the object provided as parameter.
 */
public class SimpleReadableResource extends AbstractResource {

    private final ResourceResolver resourceResolver;
    private final String resourcePath;
    private final Map<String, Object> properties;
    private final Object[] adapters;

    private final ResourceMetadata metadata = new ResourceMetadata();


    public SimpleReadableResource(ResourceResolver resourceResolver, String resourcePath,
                                    Map<String, Object> properties, Object... adapters) {

        this.resourceResolver = resourceResolver;
        this.resourcePath = resourcePath;
        this.properties = properties;
        this.adapters = adapters;
    }

    public String getPath() {
        return resourcePath;
    }

    public String getResourceType() {
        // get resource type from data
        final Object rt = this.properties.get("sling:resourceType");
        if ( rt != null ) {
            return rt.toString();
        }
        return "nt:unstructured";
    }

    public String getResourceSuperType() {
        // get resource type from data
        final Object rt = this.properties.get("sling:resourceSuperType");
        if ( rt != null ) {
            return rt.toString();
        }
        return "nt:unstructured";
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }


    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

        if (type == ValueMap.class || type == Map.class) {
            return (AdapterType) new ValueMapDecorator(getProperties());
        }
        else {
            if (adapters != null) {
                for (Object adapter : adapters) {
                    if (adapter == null) continue;

                    if (type.isArray()) {
                        if (adapter.getClass().isArray()) {
                            Object[] adapterArray = (Object[]) adapter;
                            AdapterType result = convertArray(type, adapterArray);

                            return result;
                        }
                    }
                    else if (type.isAssignableFrom(adapter.getClass())) {
                        return (AdapterType) adapter;
                    }
                }
            }
        }
        return super.adaptTo(type);
    }


    private  <ArrayType> ArrayType convertArray(Class<ArrayType> arrayType, Object[] array) {
        Object[] result = (Object[]) Array.newInstance(arrayType.getComponentType(), array.length);
        for (int i=0; i < array.length; i++) {
           if (array[i] != null && !arrayType.getComponentType().isAssignableFrom(array[i].getClass())) {
            return null;
           }
           result[i] = array[i];
        }

        return (ArrayType) result;
    }
}