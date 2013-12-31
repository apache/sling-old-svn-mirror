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
package org.apache.sling.featureflags.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.ResourceTypeMapper;

/**
 * Resource decorator implementing the resource type mapping
 */
@Component
@Service(value=ResourceDecorator.class)
public class ResourceDecoratorImpl implements ResourceDecorator {

    @Reference
    private FeatureManager manager;

    @Override
    public Resource decorate(final Resource resource) {
        final ClientContext info = manager.getCurrentClientContext();
        if ( info != null ) {
            for(final ResourceTypeMapper f : ((ClientContextImpl)info).getMappingFeatures() ) {

                final String resourceType = resource.getResourceType();
                final String overwriteType = f.getResourceTypeMapping().get(resourceType);
                if ( overwriteType != null ) {
                    return new ResourceWrapper(resource) {

                        @Override
                        public String getResourceType() {
                            return overwriteType;
                        }

                        @Override
                        public String getResourceSuperType() {
                            return resourceType;
                        }
                    };
                }
            }
        }
        return resource;
    }

    @Override
    public Resource decorate(final Resource resource, final HttpServletRequest request) {
        return this.decorate(resource);
    }
}
