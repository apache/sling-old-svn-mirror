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
package org.apache.sling.api.adapter;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>SlingAdaptable</code> class is an (abstract) default implementation
 * of the <code>Adaptable</code> interface. It just uses the default
 * {@link AdapterManager} implemented to adapt itself to the requested type.
 * <p>
 * Extensions of this class may overwrite the {@link #adaptTo(Class)} method
 * using their own knowledge of adapters and should call this base class
 * implementation to fall back for other types.
 *
 * @since 2.2 (Sling API Bundle 2.2.0)
 */
public abstract class SlingAdaptable implements Adaptable {

    /** The adapter manager used for adapting the synthetic resource. */
    private static volatile AdapterManager ADAPTER_MANAGER;

    /**
     * Sets the global {@link AdapterManager} to be used by this class.
     * <p>
     * This method is intended to only be called by the {@link AdapterManager}
     * wanting to register itself for use. Currently only a single adapter
     * manager is supported by this class.
     *
     * @param adapterMgr The {@link AdapterManager} to be globally used.
     */
    public static void setAdapterManager(final AdapterManager adapterMgr) {
        ADAPTER_MANAGER = adapterMgr;
    }

    /**
     * Unsets the global {@link AdapterManager}.
     * <p>
     * This method is intended to only be called by the {@link AdapterManager}
     * wanting to unregister itself. Currently only a single adapter manager is
     * supported by this class.
     *
     * @param adapterMgr The {@link AdapterManager} to be unset. If this is not
     *            the same as currently registered this method has no effect.
     */
    public static void unsetAdapterManager(final AdapterManager adapterMgr) {
        if (ADAPTER_MANAGER == adapterMgr) {
            ADAPTER_MANAGER = null;
        }
    }

    /**
     * Cached adapters per type.
     * <p>
     * This map is created on demand by the {@link #adaptTo(Class)} method as a
     * regular <code>HashMap</code>. This means, that extensions of this class
     * are intended to be short-lived to not hold on to objects and classes for
     * too long.
     */
    private Map<Class<?>, Object> adaptersCache;

    /**
     * Calls into the registered {@link AdapterManager} to adapt this object to
     * the desired <code>type</code>.
     * <p>
     * This method implements a cache of adapters to improve performance. That
     * is repeated calls to this method with the same class will result in the
     * same object to be returned.
     *
     * @param <AdapterType> The generic type to which this resource is adapted
     *            to
     * @param type The Class object of the target type, such as
     *            <code>javax.jcr.Node.class</code> or
     *            <code>java.io.File.class</code>
     * @return The adapter target or <code>null</code> if the resource cannot
     *         adapt to the requested type
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType result = null;
        synchronized ( this ) {
            if ( adaptersCache != null ) {
                result = (AdapterType) adaptersCache.get(type);
            }
            if ( result == null ) {
                final AdapterManager mgr = ADAPTER_MANAGER;
                result = (mgr == null ? null : mgr.getAdapter(this, type));
                if ( result != null ) {
                    if ( adaptersCache == null ) {
                        adaptersCache = new HashMap<Class<?>, Object>();
                    }
                    adaptersCache.put(type, result);
                }
            }
        }
        return result;
    }
}
