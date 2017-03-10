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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.FsResourceProvider;
import org.apache.sling.fsprovider.internal.mapper.valuemap.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>FsResource</code> represents a file system file or folder as
 * a Sling Resource.
 */
@Adaptable(adaptableClass=Resource.class, adapters={
    @Adapter({File.class, URL.class}),
    @Adapter(condition="If the resource is an FsResource and is a readable file.", value=InputStream.class)
})
public final class FileResource extends AbstractResource {

    /**
     * The resource type for file system files mapped into the resource tree by
     * the {@link FsResourceProvider} (value is "nt:file").
     */
    static final String RESOURCE_TYPE_FILE = "nt:file";

    /**
     * The resource type for file system folders mapped into the resource tree
     * by the {@link FsResourceProvider} (value is "nt:folder").
     */
    static final String RESOURCE_TYPE_FOLDER = "nt:folder";

    // the owning resource resolver
    private final ResourceResolver resolver;

    // the path of this resource in the resource tree
    private final String resourcePath;

    // the file wrapped by this instance
    private final File file;

    // the resource type, assigned on demand
    private String resourceType;

    // the resource metadata, assigned on demand
    private ResourceMetadata metaData;

    private static final Logger log = LoggerFactory.getLogger(FileResource.class);
    
    /**
     * Creates an instance of this File system resource.
     *
     * @param resolver The owning resource resolver
     * @param resourcePath The resource path in the resource tree
     * @param file The wrapped file
     */
    FileResource(ResourceResolver resolver, String resourcePath, File file) {
        this.resolver = resolver;
        this.resourcePath = resourcePath;
        this.file = file;
    }

    /**
     * Returns the path of this resource
     */
    public String getPath() {
        return resourcePath;
    }

    /**
     * Returns the resource meta data for this resource containing the file
     * length, last modification time and the resource path (same as
     * {@link #getPath()}).
     */
    public ResourceMetadata getResourceMetadata() {
        if (metaData == null) {
            metaData = new ResourceMetadata();
            metaData.setContentLength(file.length());
            metaData.setModificationTime(file.lastModified());
            metaData.setResolutionPath(resourcePath);
            if ( this.file.isDirectory() ) {
                metaData.put(FsResourceProvider.RESOURCE_METADATA_FILE_DIRECTORY, Boolean.TRUE);
            }
        }
        return metaData;
    }

    /**
     * Returns the resource resolver which cause this resource object to be
     * created.
     */
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    /**
     * Returns <code>null</code>}
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * Returns {@link #RESOURCE_TYPE_FILE} if this resource
     * wraps a file. Otherwise {@link #RESOURCE_TYPE_FOLDER}
     * is returned.
     */
    public String getResourceType() {
        if (resourceType == null) {
            resourceType = file.isFile() ? RESOURCE_TYPE_FILE : RESOURCE_TYPE_FOLDER;
        }
        return resourceType;
    }

    /**
     * Returns an adapter for this resource. This implementation supports
     * <code>File</code>, <code>InputStream</code> and <code>URL</code>
     * plus those supported by the adapter manager.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == File.class) {
            return (AdapterType) file;
        }
        else if (type == InputStream.class) {
            if (!file.isDirectory() && file.canRead()) {
                try {
                    return (AdapterType) new FileInputStream(file);
                }
                catch (IOException ioe) {
                    log.info("adaptTo: Cannot open a stream on the file " + file, ioe);
                }
            }
            else {
                log.debug("adaptTo: File {} is not a readable file", file);
            }
        }
        else if (type == URL.class) {
            try {
                return (AdapterType) file.toURI().toURL();
            }
            catch (MalformedURLException mue) {
                log.info("adaptTo: Cannot convert the file path " + file + " to an URL", mue);
            }

        }
        else if (type == ValueMap.class) {
            // this resource simulates nt:file/nt:folder behavior by returning it as resource type
            // we should simulate the corresponding JCR properties in a value map as well
            if (file.exists() && file.canRead()) {
                Map<String,Object> props = new HashMap<String, Object>();
                props.put("jcr:primaryType", getResourceType());
                props.put("jcr:createdBy", "system");
                Calendar lastModifed = Calendar.getInstance();
                lastModifed.setTimeInMillis(file.lastModified());
                props.put("jcr:created", lastModifed);
                return (AdapterType) new ValueMapDecorator(props);
            }
        }
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", resourcePath)
                .append("file", file.getPath())
                .append("resourceType", getResourceType())
                .build();
    }

}
