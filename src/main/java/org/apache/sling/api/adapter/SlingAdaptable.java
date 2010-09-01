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
 * The <code>SlingAdaptable</code> class is an (abstract) default
 * implementation of the <code>Adaptable</code> interface. It just uses the
 * default {@link org.apache.sling.api.adapter.AdapterManager} implemented
 * to adapt itself to the requested type.
 * <p>
 * Extensions of this class may overwrite the {@link #adaptTo(Class)} method
 * using their own knowledge of adapters and may call this base class
 * implementation to fall back to an extended adapters.
 *
 * @since 2.2
 */
public abstract class SlingAdaptable implements Adaptable {

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

    /** Cache */
    private Map<Class<?>, Object> adaptersCache;

    /**
     * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
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
