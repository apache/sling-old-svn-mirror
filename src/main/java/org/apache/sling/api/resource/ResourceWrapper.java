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
package org.apache.sling.api.resource;

import java.util.Iterator;

import javax.annotation.Nonnull;

/**
 * The <code>ResourceWrapper</code> is a wrapper for any <code>Resource</code>
 * delegating all method calls to the wrapped resource by default. Extensions of
 * this class may overwrite any method to return different values as
 * appropriate.
 */
public class ResourceWrapper implements Resource {

    /** the wrapped resource */
    private final Resource resource;

    /**
     * Creates a new wrapper instance delegating all method calls to the given
     * <code>resource</code>.
     */
    public ResourceWrapper(@Nonnull final Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the <code>Resource</code> wrapped by this instance. This method
     * can be overwritten by subclasses if required. All methods implemented by
     * this class use this method to get the resource object.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Returns the value of calling <code>getPath</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public String getPath() {
        return getResource().getPath();
    }

    /**
     * Returns the value of calling <code>getName</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     */
    public String getName() {
        return getResource().getName();
    }

    /**
     * Returns the value of calling <code>getParent</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     */
    public Resource getParent() {
        return getResource().getParent();
    }

    /**
     * Returns the value of calling <code>getChild</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     */
    public Resource getChild(String relPath) {
        return getResource().getChild(relPath);
    }

    /**
     * Returns the value of calling <code>listChildren</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     */
    public Iterator<Resource> listChildren() {
        return getResource().listChildren();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getChildren()
     */
    public Iterable<Resource> getChildren() {
        return getResource().getChildren();
    }

    /**
     * Returns the value of calling <code>getResourceMetadata</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public ResourceMetadata getResourceMetadata() {
        return getResource().getResourceMetadata();
    }

    /**
     * Returns the value of calling <code>getResourceResolver</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public ResourceResolver getResourceResolver() {
        return getResource().getResourceResolver();
    }

    /**
     * Returns the value of calling <code>getResourceType</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public String getResourceType() {
        return getResource().getResourceType();
    }

    /**
     * Returns the value of calling <code>getResourceSuperType</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public String getResourceSuperType() {
        return getResource().getResourceSuperType();
    }

    /**
     * Returns the value of calling <code>hasChildren</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.4.4  (Sling API Bundle 2.5.0)
     */
	public boolean hasChildren() {
		return getResource().hasChildren();
	}

    /**
     * Returns the value of calling <code>isResourceType</code> on the
     * {@link #getResource() wrapped resource}.
     *
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     */
    public boolean isResourceType(final String resourceType) {
        return getResource().isResourceType(resourceType);
    }

    /**
     * Returns the value of calling <code>adaptTo</code> on the
     * {@link #getResource() wrapped resource}.
     */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getResource().adaptTo(type);
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getValueMap()
     */
    public ValueMap getValueMap() {
        return getResource().getValueMap();
    }

    /**
     * Returns a string representation of this wrapper consisting of the class'
     * simple name, the {@link #getResourceType() resource type} and
     * {@link #getPath() path} as well as the string representation of the
     * {@link #getResource() wrapped resource}.
     */
    @Override
    public String toString() {
        final String simpleName = getClass().getSimpleName();
        final String className = (simpleName.length() > 0) ? simpleName : getClass().getName();
        return className + ", type=" + getResourceType()
            + ", path=" + getPath() + ", resource=[" + getResource() + "]";
    }

}
