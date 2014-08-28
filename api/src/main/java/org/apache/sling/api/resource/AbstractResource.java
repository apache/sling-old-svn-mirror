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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * The <code>AbstractResource</code> is an abstract implementation of the
 * {@link Resource} interface.
 * <p>
 * Implementations of the {@link Resource} interface are strongly encouraged to
 * either extend from this class or the {@link ResourceWrapper} class instead of
 * implementing the {@link Resource} from the ground up. This will ensure to
 * always be able to support new methods that might be introduced in the
 * {@link Resource} interface in the future.
 *
 * @since 2.1.0
 */
public abstract class AbstractResource
    extends SlingAdaptable
    implements Resource {

    /**
     * Returns the name of this resource.
     * <p>
     * This method is implemented as a pure string operation by calling the
     * {@link ResourceUtil#getName(String)} method with the path of this
     * resource.
     */
    public String getName() {
        return ResourceUtil.getName(getPath());
    }

    /**
     * Returns the parent resource of this resource.
     * <p>
     * This method is implemented by getting the parent resource path first
     * calling the {@link ResourceUtil#getParent(String)} method and then to
     * retrieve that resource from the resource resolver.
     */
    public Resource getParent() {
        final String parentPath = ResourceUtil.getParent(getPath());
        if (parentPath == null) {
            return null;
        }
        return getResourceResolver().getResource(parentPath);
    }

    /**
     * Returns the indicated child of this resource.
     * <p>
     * This method is implemented calling the
     * {@link ResourceResolver#getResource(Resource, String)} method. As such
     * the <code>relPath</code> argument may even be an absolute path or a path
     * containing relative path segments <code>.</code> (current resource) and
     * <code>..</code> (parent resource).
     * <p>
     * Implementations should not generally overwrite this method without
     * calling this base class implementation.
     */
    public Resource getChild(String relPath) {
        return getResourceResolver().getResource(this, relPath);
    }

    /**
     * Returns an iterator on the direct child resources.
     * <p>
     * This method is implemented calling the
     * {@link ResourceResolver#listChildren(Resource)} method.
     * <p>
     * Implementations should not generally overwrite this method without
     * calling this base class implementation.
     */
    public Iterator<Resource> listChildren() {
        return getResourceResolver().listChildren(this);
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getChildren()
     */
    public Iterable<Resource> getChildren() {
        return new Iterable<Resource>() {

            public Iterator<Resource> iterator() {
                return listChildren();
            }
        };
    }

    /**
     * Checks to see if there are direct children of this resource by invoking
     * {@link ResourceResolver#hasChildren(Resource)}.
     */
    public boolean hasChildren() {
        return getResourceResolver().hasChildren(this);
    }

    /**
     * Returns <code>true</code> if this resource is of the given resource type
     * or if any of the super resource types equals the given resource type.
     * <p>
     * This method delegates to {@link ResourceResolver#isResourceType(Resource, String)}
     */
    public boolean isResourceType(final String resourceType) {
        return this.getResourceResolver().isResourceType(this, resourceType);
    }

    /**
     * This method calls {@link Resource#adaptTo(Class)}
     * with the {@link ValueMap} class as an argument. If the
     * <code>adaptTo</code> method returns a map, this map is returned. If the
     * resource is not adaptable to a value map, next an adaption to {@link Map}
     * is tried and if this is successful the map is wrapped as a value map. If
     * the adaptions are not successful an empty value map is returned.
     * @see org.apache.sling.api.resource.Resource#getValueMap()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ValueMap getValueMap() {
        // adapt to ValueMap if resource is not null
        ValueMap valueMap = this.adaptTo(ValueMap.class);

        // if no resource or no ValueMap adapter, check Map
        if (valueMap == null) {

            Map map = this.adaptTo(Map.class);

            // if not even adapting to map, assume an empty map
            if (map == null) {
                map = new HashMap<String, Object>();
            }

            // .. and decorate the plain map
            valueMap = new DeepReadValueMapDecorator(this, new ValueMapDecorator(map));
        }

        return valueMap;
    }
}
