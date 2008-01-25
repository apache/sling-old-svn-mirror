/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.resource;

import static org.apache.sling.api.resource.ResourceMetadata.RESOLUTION_PATH;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;

/** A Resource that wraps a JCR Property */
public class JcrPropertyResource implements Resource {

    /** This empty string is currently used as the Resource type
     *  for all Resources of this class.
     */
    public static final String DEFAULT_RESOURCE_TYPE = "";

    private final Property property;
    private final String path;
    private final ResourceMetadata metadata;
    private final String resourceType;

    JcrPropertyResource(Property p) throws RepositoryException {
        property = p;
        path = p.getPath();
        metadata = new ResourceMetadata();
        metadata.put(RESOLUTION_PATH, path);
        resourceType = DEFAULT_RESOURCE_TYPE;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + resourceType + ", path=" + path;
    }

    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == Property.class) {
            return (Type) property;
        }
        return null;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getPath() {
        return path;
    }

    /** Returns null as microsling has no ResourceProviders */
    public ResourceProvider getResourceProvider() {
        return null;
    }
}
