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
package org.apache.sling.models.impl.via;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.models.spi.ViaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public abstract class AbstractResourceTypeViaProvider implements ViaProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Object getAdaptable(Object original, String value) {
        if (!handle(value)) {
            return ORIGINAL;
        }
         if (original instanceof Resource) {
            final Resource resource = (Resource) original;
            final String resourceType = getResourceType(resource, value);
            if (resourceType == null) {
                log.warn("Could not determine forced resource type for {} using via value {}.", resource, value);
                return null;
            }
            return new ResourceTypeForcingResourceWrapper(resource, resourceType);
         } else if (original instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) original;
            final Resource resource = request.getResource();
            if (resource == null) {
                return null;
            }
            final String resourceType = getResourceType(resource, value);
            if (resourceType == null) {
                log.warn("Could not determine forced resource type for {} using via value {}.", resource, value);
                return null;
            }
            return new ResourceTypeForcingRequestWrapper(request, resource, resourceType);
         } else {
            log.warn("Received unexpected adaptable of type {}.", original.getClass().getName());
            return null;
         }
    }

    protected abstract boolean handle(@Nonnull String value);

    protected abstract @CheckForNull String getResourceType(@Nonnull Resource resource, @Nonnull String value);

    private class ResourceTypeForcingResourceWrapper extends ResourceWrapper {

        private final String resourceType;

        private ResourceTypeForcingResourceWrapper(Resource resource, String resourceType) {
            super(resource);
            this.resourceType = resourceType;
        }

        @Override
        public String getResourceType() {
            return resourceType;
        }
    }

    private class ResourceTypeForcingRequestWrapper extends SlingHttpServletRequestWrapper {

        private final Resource resource;

        private ResourceTypeForcingRequestWrapper(SlingHttpServletRequest request, Resource resource, String resourceType) {
            super(request);
            this.resource = new ResourceTypeForcingResourceWrapper(resource, resourceType);
        }

        @Override
        public Resource getResource() {
            return resource;
        }
    }
}
