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
package org.apache.sling.testing.resourceresolver;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * Resource that wraps a property value.
 */
class MockPropertyResource extends AbstractResource {
    
    private final String path;
    private final ValueMap props;
    private final String key;
    private final ResourceResolver resolver;
    private final ResourceMetadata rm = new ResourceMetadata();

    public MockPropertyResource(final String path,
            final ValueMap props,
            final ResourceResolver resolver) {
        this.path = path;
        this.props = props;
        this.key = ResourceUtil.getName(path);
        this.resolver = resolver;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return rm;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resolver;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType value = props.get(key, type);
        if (value!=null) {
            return value;
        }
        return super.adaptTo(type);
    }

}
