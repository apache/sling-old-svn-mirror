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
package org.apache.sling.api.resource;

import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.adapter.Adaptable;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Resources are pieces of content on which Sling acts
 * <p>
 * The <code>Resource</code> is also an {@link Adaptable} to get adapters to
 * other types. A JCR based resource might support adapting to the JCR Node on
 * which the resource is based.
 * <p>
 * A <code>Resource</code> object is valid for as long as the
 * <code>ResourceResolver</code> that provided this instance is valid. The
 * same applies in general to all objects returned by this instance,
 * especially those returned by a call to {@link #adaptTo(Class)}.
 * <p>
 * All implementations must support returning a value map from
 * {@link #getValueMap()}, even if the map is empty.
 * <p>
 * Implementor's Note: It is recommended to not implement this interface
 * directly. Rather consider either extending from {@link AbstractResource} or
 * {@link ResourceWrapper}. This will make sure your implementation will not be
 * suffering from missing method problems should the Sling Resource API be
 * extended in the future.
 */
@ProviderType
public interface Resource extends Adaptable {

    /**
     * The special resource type for resource instances representing nonexisting
     * resources (value is "sling:nonexisting"). This resource type is used by
     * {@link ResourceResolver} instances to mark a resource which could not
     * actually be resolved.
     *
     * @see #getResourceType()
     * @see ResourceUtil#isNonExistingResource(Resource)
     * @see ResourceResolver#resolve(javax.servlet.http.HttpServletRequest,
     *      String)
     */
    String RESOURCE_TYPE_NON_EXISTING = "sling:nonexisting";

    /**
     * Returns the absolute path of this resource in the resource tree.
     * @return The resource path
     */
    @Nonnull String getPath();

    /**
     * Returns the name of this resource. The name of a resource is the last
     * segment of the {@link #getPath() path}.
     *
     * @return The resource name
     * @since 2.1 (Sling API Bundle 2.2.0)
     */
    @Nonnull String getName();

    /**
     * Returns the parent resource or <code>null</code> if this resource
     * represents the root of the resource tree.
     *
     * @return The parent resource or {@code null}.
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             get the resource object from the path.
     * @throws IllegalStateException if the resource resolver has already been
     *             closed}.
     * @since 2.1 (Sling API Bundle 2.1.0)
     * @see ResourceResolver#getParent(Resource)
     */
    @CheckForNull Resource getParent();

    /**
     * Returns an iterator of the direct children of this resource.
     * <p>
     * This method is a convenience and returns exactly the same resources as
     * calling <code>getResourceResolver().listChildren(resource)</code>.
     *
     * @return An iterator for child resources.
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             get the resource iterator.
     * @throws IllegalStateException if the resource resolver has already been
     *             closed}.
     * @since 2.1 (Sling API Bundle 2.1.0)
     * @see ResourceResolver#listChildren(Resource)
     */
    @Nonnull Iterator<Resource> listChildren();

    /**
     * Returns an iterable of the direct children of this resource.
     * <p>
     * This method is a convenience and returns exactly the same resources as
     * calling <code>getResourceResolver().getChildren(resource)</code>.
     *
     * @return An iterable for child resources
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             get the resource iterator.
     * @throws IllegalStateException if the resource resolver has already been
     *             closed}.
     * @since 2.2 (Sling API Bundle 2.2.0)
     * @see ResourceResolver#getChildren(Resource)
     */
    @Nonnull Iterable<Resource> getChildren();

    /**
     * Returns the child at the given relative path of this resource or
     * <code>null</code> if no such child exists.
     * <p>
     * This method is a convenience and returns exactly the same resources as
     * calling <code>getResourceResolver().getResource(resource, relPath)</code>.
     *
     * @param relPath relative path to the child resource
     * @return The child resource or {@code null}
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             get the resource object from the path.
     * @throws IllegalStateException if the resource resolver has already been
     *             closed}.
     * @since 2.1 (Sling API Bundle 2.1.0)
     * @see ResourceResolver#getResource(Resource, String)
     */
    @CheckForNull Resource getChild(@Nonnull String relPath);

    /**
     * The resource type is meant to point to rendering/processing scripts,
     * editing dialogs, etc. It is usually a path in the repository, where
     * scripts and other tools definitions are found, but the
     * {@link ResourceResolver} is free to set this to any suitable value such
     * as the primary node type of the JCR node from which the resource is
     * created.
     * <p>
     * If the resource instance represents a resource which is not actually
     * existing, this method returns {@link #RESOURCE_TYPE_NON_EXISTING}.
     * @return The resource type
     */
    @Nonnull String getResourceType();

    /**
     * Returns the super type of the resource if the resource defines its
     * own super type. Otherwise <code>null</code> is returned.
     * A resource might return a resource super type to overwrite the
     * resource type hierarchy.
     * If a client is interested in the effective resource super type
     * of a resource, it should call {@link ResourceResolver#getParentResourceType(Resource)}.
     * @return The super type of the resource or {@code null}.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link ResourceResolver#close() closed}.
     */
    @CheckForNull String getResourceSuperType();

    /**
     * Checks if the resource has any child resources.
     *
     * @return <code>true</code> if the resource has any child resources
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link ResourceResolver#close() closed}.
     * @since 2.4.4 (Sling API Bundle 2.5.0)
     */
    boolean hasChildren();

    /**
     * Is just a shortcut for {@code getResourceResolver().isResourceType(this, resourceType)}.
     *
     * @param resourceType the resource type to check this resource against
     * @see ResourceResolver#isResourceType(Resource, String)
     * @since 2.1.0 (Sling API Bundle 2.1.0)
     * @return <code>true</code> if the resource type or any of the resource's
     *         super type(s) equals the given resource type, <code>false</code> otherwise;
     *         <code>false</code> can also be returned if <code>resourceType</code> is null
     */
    boolean isResourceType(String resourceType);

    /**
     * Returns the meta data of this resource. The concrete data contained in the
     * {@link ResourceMetadata} object returned is implementation specific
     * except for the {@link ResourceMetadata#RESOLUTION_PATH} property which is
     * required to be set to the part of the request URI used to resolve the
     * resource.
     *
     * @return The resource meta data
     * @see ResourceMetadata
     */
    @Nonnull ResourceMetadata getResourceMetadata();

    /**
     * Returns the {@link ResourceResolver} from which this resource has been
     * retrieved.
     * @return The resource resolver
     */
    @Nonnull ResourceResolver getResourceResolver();

    /**
     * Returns a value map for this resource.
     * The value map allows to read the properties of the resource.
     * @return A value map
     * @since 2.5 (Sling API Bundle 2.7.0)
     */
    @Nonnull ValueMap getValueMap();
}
