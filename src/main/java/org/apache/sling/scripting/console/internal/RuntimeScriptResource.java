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

package org.apache.sling.scripting.console.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * User: chetanm
 * Date: 5/1/12
 * Time: 12:20 PM
 */
class RuntimeScriptResource extends SlingAdaptable implements Resource {
    private ResourceMetadata metadata = new ResourceMetadata();
    private final String extension;
    private final String path;
    private final byte[] scriptContent;

    public RuntimeScriptResource(String extension, String scriptText) {
        this.extension = extension;
        this.scriptContent = getScriptContent(scriptText);
        this.path = "script." + extension;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if(InputStream.class.isAssignableFrom(type)){
            return (AdapterType) new ByteArrayInputStream(scriptContent);
        }
        return super.adaptTo(type);
    }

    /**
     * Returns the absolute path of this resource in the resource tree.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the name of this resource. The name of a resource is the last
     * segment of the {@link #getPath() path}.
     *
     * @since 2.1.0
     */
    public String getName() {
        return "script";
    }

    /**
     * Returns the parent resource or <code>null</code> if this resource
     * represents the root of the resource tree.
     *
     * @since 2.1.0
     */
    public Resource getParent() {
        return null;
    }

    /**
     * Returns an iterator of the direct children of this resource.
     * <p/>
     * This method is a convenience and returns exactly the same resources as
     * calling <code>getResourceResolver().listChildren(resource)</code>.
     *
     * @see org.apache.sling.api.resource.ResourceResolver#listChildren(org.apache.sling.api.resource.Resource)
     * @since 2.1.0
     */
    public Iterator<Resource> listChildren() {
        return Collections.<Resource>emptyList().iterator();
    }

    /**
     * Returns the child at the given relative path of this resource or
     * <code>null</code> if no such child exists.
     * <p/>
     * This method is a convenience and returns exactly the same resources as
     * calling <code>getResourceResolver().getResource(resource, relPath)</code>.
     *
     * @see org.apache.sling.api.resource.ResourceResolver#getResource(org.apache.sling.api.resource.Resource, String)
     * @since 2.1.0
     */
    public Resource getChild(String relPath) {
        return null;
    }

    /**
     * The resource type is meant to point to rendering/processing scripts,
     * editing dialogs, etc. It is usually a path in the repository, where
     * scripts and other tools definitions are found, but the
     * {@link org.apache.sling.api.resource.ResourceResolver} is free to set this to any suitable value such
     * as the primary node type of the JCR node from which the resource is
     * created.
     * <p/>
     * If the resource instance represents a resource which is not actually
     * existing, this method returns {@link #RESOURCE_TYPE_NON_EXISTING}.
     */
    public String getResourceType() {
        return RESOURCE_TYPE_NON_EXISTING;
    }

    /**
     * Returns the super type of the type of the resource or <code>null</code>
     * if the {@link #getResourceType()} has no supertype.
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * Returns <code>true</code> if the resource type or any of the resource's
     * super type(s) equals the given resource type.
     *
     * @param resourceType The resource type to check this resource against.
     * @return <code>true</code> if the resource type or any of the resource's
     *         super type(s) equals the given resource type. <code>false</code>
     *         is also returned if <code>resourceType</code> is
     *         <code>null</code>.
     * @since 2.1.0
     */
    public boolean isResourceType(String resourceType) {
        return false;
    }

    /**
     * Returns the metadata of this resource. The concrete data contained in the
     * {@link org.apache.sling.api.resource.ResourceMetadata} object returned is implementation specific
     * except for the {@link org.apache.sling.api.resource.ResourceMetadata#RESOLUTION_PATH} property which is
     * required to be set to the part of the request URI used to resolve the
     * resource.
     *
     * @see org.apache.sling.api.resource.ResourceMetadata
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * Returns the {@link org.apache.sling.api.resource.ResourceResolver} from which this resource has been
     * retrieved.
     */
    public ResourceResolver getResourceResolver() {
        return null;
    }

    private byte[] getScriptContent(String scriptText) {
        try {
            return scriptText.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
