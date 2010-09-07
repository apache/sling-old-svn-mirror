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
package org.apache.sling.sample.slingshot.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.sample.slingshot.Constants;

/**
 * This resource decorator "converts" the standard resource types
 * nt:file, sling:Folder and nt:filder to the resource types used inside
 * Slingshot.
 *
 * We only adapt if the resource is within the Slingshot albums folder.
 */
@Component
@Service(value=ResourceDecorator.class)
@Properties({
   @Property(name="service.description",
             value="Apache Sling - Slingshot Resource Type Decorator")
})
public class ResourceTypeConversionDecorator
    implements ResourceDecorator {

    /**
     * @see org.apache.sling.api.resource.ResourceDecorator#decorate(org.apache.sling.api.resource.Resource)
     */
    public Resource decorate(Resource resource) {
        return this.decorate(resource, null);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceDecorator#decorate(org.apache.sling.api.resource.Resource, javax.servlet.http.HttpServletRequest)
     */
    public Resource decorate(Resource resource, HttpServletRequest request) {
        if ( resource.getPath().startsWith(Constants.ALBUMS_ROOT) ) {
            if ( ResourceUtil.isA(resource, Constants.RESOURCETYPE_FILE) ) {
                return new SlingshotResource(resource, Constants.RESOURCETYPE_PHOTO);
            }
            if ( ResourceUtil.isA(resource, Constants.RESOURCETYPE_EXT_FOLDER)
                 || ResourceUtil.isA(resource, Constants.RESOURCETYPE_FOLDER) ) {
                return new SlingshotResource(resource, Constants.RESOURCETYPE_ALBUM);
            }
        }
        return resource;
    }

    public static final class SlingshotResource extends ResourceWrapper {

        private final String resourceType;

        public SlingshotResource(final Resource resource, final String resourceType) {
            super(resource);
            this.resourceType = resourceType;
        }

        @Override
        public String getResourceType() {
            return this.resourceType;
        }

        @Override
        public String getResourceSuperType() {
            return super.getResourceType();
        }

    }
}
