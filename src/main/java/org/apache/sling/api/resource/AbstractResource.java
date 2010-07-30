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

import org.apache.sling.api.adapter.AdapterManager;

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
public abstract class AbstractResource implements Resource {

    /** The adapter manager used for adapting the synthetic resource. */
    private static volatile AdapterManager ADAPTER_MANAGER;

    /**
     * Set the adapter manager to be used by a synthetic resource. A bundle
     * implementing the adapter manager can set the manager through this method.
     * The set adapter manager will be used in the {@link #adaptTo(Class)}
     * method of a synthetic resource.
     *
     * @param adapterMgr The adapter manager.
     */
    public static void setAdapterManager(final AdapterManager adapterMgr) {
        ADAPTER_MANAGER = adapterMgr;
    }

    /**
     * Unset an adapter manager previously set with
     * {@link #setAdapterManager(AdapterManager)}. If this method is called with
     * an <code>AdapterManager</code> different from the currently set one it
     * has no effect.
     *
     * @param adapterMgr The adapter manager
     */
    public static void unsetAdapterManager(final AdapterManager adapterMgr) {
        if (ADAPTER_MANAGER == adapterMgr) {
            ADAPTER_MANAGER = null;
        }
    }

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
    @SuppressWarnings("deprecation")
    public Resource getParent() {
        /*
         * Implemented calling the deprecated ResourceUtil.getParent method
         * (which actually has the implementation) to prevent problems if there
         * are implementations of the pre-2.1.0 Resource interface in the
         * framework.
         */
        return ResourceUtil.getParent(this);
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
     * Returns <code>true</code> if this resource is of the given resource type
     * or if any of the super resource types equals the given resource type.
     * <p>
     * This method is implemented by first checking the resource type then
     * walking up the resource super type chain using the
     * {@link ResourceUtil#findResourceSuperType(Resource)} and
     * {@link ResourceUtil#getResourceSuperType(ResourceResolver, String)}
     * methods.
     */
    public boolean isResourceType(String resourceType) {
        /*
         * Implemented calling the ResourceUtil.isA method (which actually has
         * the implementation) to prevent problems if there are implementations
         * of the pre-2.1.0 Resource interface in the framework.
         */
        return ResourceUtil.isA(this, resourceType);
    }

    /**
     * If a adapter manager has been set through
     * {@link #setAdapterManager(AdapterManager)} this adapter manager is used
     * to adapt the resource to the given type. Otherwise this method returns
     * <code>null</code>.
     * <p>
     * This default base implementation is intended to be overwritten by
     * extensions. Overwriting implementations are are encouraged to call this
     * base class implementation if they themselves cannot adapt to the
     * requested type.
     */
    public <Type> Type adaptTo(Class<Type> type) {
        final AdapterManager adapterMgr = ADAPTER_MANAGER;
        if (adapterMgr != null) {
            return adapterMgr.getAdapter(this, type);
        }
        return null;
    }

}
