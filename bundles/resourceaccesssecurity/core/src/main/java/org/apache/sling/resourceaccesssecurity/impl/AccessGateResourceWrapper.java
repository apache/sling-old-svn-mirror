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
package org.apache.sling.resourceaccesssecurity.impl;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;

/**
 * The <code>AccessGateResourceWrapper</code> wraps a <code>Resource</code> and
 * intercepts calls to adaptTo to wrap the adapted <code>ValueMap</code> or
 * also a <code>ModifiableValueMap</code> to enforce access rules defined
 * by implementations of <code>ResourceAccessGate</code>
 *
 */
public class AccessGateResourceWrapper extends ResourceWrapper {

    private final List<ResourceAccessGate> accessGatesForReadValues;
    private final boolean modifiable;

    /**
     * Creates a new wrapper instance delegating all method calls to the given
     * <code>resource</code>, but intercepts the calls with checks to the
     * applied ResourceAccessGate instances for read and/or update values.
     * 
     * @param resource resource to protect
     * @param accessGatesForReadForValues list of access gates to ask when reading values. If 
     *      the list is <code>null</code> or empty there are no read restrictions
     * @param modifiable if <code>true</code> the resource can be updated
     */
    public AccessGateResourceWrapper(final Resource resource,
                                     final List<ResourceAccessGate> accessGatesForReadForValues,
                                     final boolean modifiable ) {
        super( resource );
        this.accessGatesForReadValues = accessGatesForReadForValues;
        this.modifiable = modifiable;
    }

    /**
     * Returns the value of calling <code>adaptTo</code> on the
     * {@link #getResource() wrapped resource}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        // we do not support the deprecated PersistableValueMap
        AdapterType adapter = getResource().adaptTo(type);

        if (adapter != null && !modifiable) {
            if (type == ModifiableValueMap.class) {
                adapter = null;
            }
            else if (type == Map.class || type == ValueMap.class) {
                // protect also against accidental modifications when changes are done in an adapted map
                adapter = (AdapterType) new ReadOnlyValueMapWrapper((Map) adapter);
            }
        }


        return adapter;


    }


}
