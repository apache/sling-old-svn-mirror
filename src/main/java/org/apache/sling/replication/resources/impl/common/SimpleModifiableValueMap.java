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
package org.apache.sling.replication.resources.impl.common;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.replication.resources.impl.common.AbstractModifyingResourceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * ValueMap that signals to the underlying resource provider the its values are changed.
 */
public class SimpleModifiableValueMap extends ModifiableValueMapDecorator
        implements ModifiableValueMap {

    private final ResourceResolver resourceResolver;
    private final AbstractModifyingResourceProvider resourceProvider;
    private final SimpleModifiableResource resource;

    public SimpleModifiableValueMap(ResourceResolver resourceResolver,
                                    AbstractModifyingResourceProvider resourceProvider,
                                    SimpleModifiableResource resource) {
        super(resource.getProperties());
        this.resourceResolver = resourceResolver;
        this.resourceProvider = resourceProvider;
        this.resource = resource;
    }

    @Override
    public Object put(String key, Object value) {
        Object result = super.put(key, value);
        changed();
        return result;
    }

    @Override
    public Object remove(Object key) {
        Object result = super.remove(key);
        changed();
        return result;
    }

    @Override
    public void putAll(Map<? extends String, ?> t) {
        super.putAll(t);
        changed();
    }

    @Override
    public void clear() {
        super.clear();
        changed();
    }

    private void changed() {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        for (Entry<String, Object> entry : entrySet()) {
            newProperties.put(entry.getKey(), entry.getValue());
        }
        resourceProvider.change(resourceResolver, resource.getPath(), newProperties);
    }
}
