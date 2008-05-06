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
package org.apache.sling.commons.testing.sling;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

public class MockResource extends SyntheticResource {

    private String resourceType;
    private String resourceSuperType;
    
    public MockResource(ResourceResolver resourceResolver, String path,
            String resourceType) {
        this(resourceResolver, path, resourceType, null);
    }
    
    public MockResource(ResourceResolver resourceResolver, String path,
            String resourceType, String resourceSuperType) {
        super(resourceResolver, path, resourceType);
        
        setResourceType(resourceType);
        setResourceSuperType(resourceSuperType);
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }
    
    @Override
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    @Override
    public String getResourceSuperType() {
        return resourceSuperType;
    }
    
    public void setResourceSuperType(String resourceSuperType) {
        this.resourceSuperType = resourceSuperType;
    }
}
