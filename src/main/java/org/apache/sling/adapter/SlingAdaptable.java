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
package org.apache.sling.adapter;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.adapter.internal.AdapterManagerImpl;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterManager;

/**
 * The <code>SlingAdaptable</code> class is an (abstract) default
 * implementation of the <code>Adaptable</code> interface. It just uses the
 * default {@link org.apache.sling.api.adapter.AdapterManager} implemented in this bundle to adapt the itself
 * to the requested type.
 * <p>
 * Extensions of this class may overwrite the {@link #adaptTo(Class)} method
 * using their own knowledge of adapters and may call this base class
 * implementation to fall back to an extended adapters.
 */
public abstract class SlingAdaptable implements Adaptable {

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
                final AdapterManager mgr = AdapterManagerImpl.getInstance();
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
