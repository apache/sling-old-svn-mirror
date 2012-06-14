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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

@Adaptable(adaptableClass = Resource.class, adapters = @Adapter(value = { Map.class, ValueMap.class }))
public final class RedirectResource extends SyntheticResource {

    static final String RT_SLING_REDIRECT = "sling:redirect";

    static final String PROP_SLING_TARGET = "sling:target";

    static final String PROP_SLING_STATUS = "sling:status";

    private final Map<String, Object> values;

    public RedirectResource(final ResourceResolver resolver, final String path,
            final String target, final int status) {
        super(resolver, path, RT_SLING_REDIRECT);

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_SLING_TARGET, target);
        props.put(PROP_SLING_STATUS, status);
        this.values = Collections.unmodifiableMap(props);
    }

    /**
     * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) new ValueMapDecorator(values);
        } else if (type == Map.class) {
            return (AdapterType) values;
        }

        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return super.toString() + ", values=" + values;
    }
}