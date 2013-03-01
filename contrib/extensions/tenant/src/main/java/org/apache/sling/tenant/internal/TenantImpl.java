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
package org.apache.sling.tenant.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.tenant.Tenant;

/**
 * A resource backed tenant implementation.
 */
class TenantImpl implements Tenant {

    private final String id;

    private ValueMap vm;

    TenantImpl(Resource resource) {
        this.id = resource.getName();
        loadProperties(resource);
    }

    void loadProperties(Resource resource) {
        ValueMap jcrVM = ResourceUtil.getValueMap(resource);

        Map<String, Object> localMap = new HashMap<String, Object>();
        // copy all items to local map
        localMap.putAll(jcrVM);

        // decoarate it as value map
        ValueMapDecorator localVM = new ValueMapDecorator(localMap);

        this.vm = localVM;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return vm.get(Tenant.PROP_NAME, String.class);
    }

    public String getDescription() {
        return vm.get(Tenant.PROP_DESCRIPTION, String.class);
    }

    public Object getProperty(String name) {
        return vm.get(name);
    }

    public <Type> Type getProperty(String name, Type type) {
        return vm.get(name, type);
    }

    public Iterator<String> getPropertyNames() {
        return vm.keySet().iterator();
    }

}