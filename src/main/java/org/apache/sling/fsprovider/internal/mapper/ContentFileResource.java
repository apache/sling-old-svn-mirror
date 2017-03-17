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
package org.apache.sling.fsprovider.internal.mapper;

import javax.jcr.Node;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.mapper.jcr.FsNode;

/**
 * Represents a JSON File with resource content.
 */
public final class ContentFileResource extends AbstractResource {

    // the owning resource resolver
    private final ResourceResolver resolver;

    // the path of this resource in the resource tree
    private final String resourcePath;

    // the file wrapped by this instance
    private final ContentFile contentFile;

    // the resource type, assigned on demand
    private String resourceType;
    private String resourceSuperType;

    // the resource metadata, assigned on demand
    private ResourceMetadata metaData;

    /**
     * @param resolver The owning resource resolver
     * @param resourcePath The resource path in the resource tree
     * @param contentFile Content file with sub path
     */
    ContentFileResource(ResourceResolver resolver, ContentFile contentFile) {
        this.resolver = resolver;
        this.contentFile = contentFile;
        this.resourcePath = contentFile.getPath()
                + (contentFile.getSubPath() != null ? "/" + contentFile.getSubPath() : "");
    }

    public String getPath() {
        return resourcePath;
    }

    public ResourceMetadata getResourceMetadata() {
        if (metaData == null) {
            metaData = new ResourceMetadata();
            metaData.setModificationTime(contentFile.getFile().lastModified());
            metaData.setResolutionPath(resourcePath);
        }
        return metaData;
    }

    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    public String getResourceSuperType() {
        if (resourceSuperType == null) {
            resourceSuperType = getValueMap().get("sling:resourceSuperType", String.class);
        }
        return resourceSuperType;
    }

    public String getResourceType() {
        if (resourceType == null) {
            ValueMap props = getValueMap();
            resourceType = props.get("sling:resourceType", String.class);
            if (resourceType == null) {
                // fallback to jcr:primaryType when resource type not set
                resourceType = props.get("jcr:primaryType", String.class);
            }
        }
        return resourceType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ContentFile.class) {
            return (AdapterType)this.contentFile;
        }
        else if (type == ValueMap.class) {
            return (AdapterType)contentFile.getValueMap();
        }
        else if (type == Node.class) {
            // support a subset of JCR API for content file resources
            return (AdapterType)new FsNode(contentFile, getResourceResolver());
        }
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", resourcePath)
                .append("file", contentFile.getFile().getPath())
                .append("subPath", contentFile.getSubPath())
                .append("resourceType", getResourceType())
                .build();
    }

}
