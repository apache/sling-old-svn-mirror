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

package org.apache.sling.distribution.resources.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.resources.impl.common.AbstractReadableResourceProvider;

/**
 * {@link ResourceProvider} for Osgi services for a specific service interface.
 * The main resource contains a list of service properties and can be adapted to the underlying service.
 * The accepted path is resourceRoot/{friendlyNameProperty}/childResourceName.
 */
public class DistributionServiceResourceProvider extends AbstractReadableResourceProvider  {

    private final DistributionComponentKind kind;
    private final DistributionComponentProvider componentProvider;


    public DistributionServiceResourceProvider(String kind,
                                               DistributionComponentProvider componentProvider,
                                               String resourceRoot,
                                               Map<String, String> additionalResourceProperties) {
        super(resourceRoot, additionalResourceProperties);
        this.kind = DistributionComponentKind.fromName(kind);
        this.componentProvider = componentProvider;
    }

    @Override
    protected Map<String, Object> getResourceProperties(String resourceName) {

        DistributionComponent component = componentProvider.getComponent(kind, resourceName);

        if (component != null) {
            Map<String, Object> properties = new HashMap<String, Object>();

            properties.put(DistributionComponentUtils.PN_NAME, resourceName);
            properties.put(ADAPTABLE_PROPERTY_NAME, component.getService());
            return properties;
        }

        return null;
    }

    @Override
    protected Map<String, Object> getResourceRootProperties() {

        List<DistributionComponent> componentList = componentProvider.getComponents(kind);

        List<String> nameList = new ArrayList<String>();
        for (DistributionComponent component : componentList) {
            nameList.add(component.getName());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", nameList.toArray(new String[nameList.size()]));

        return result;
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

}
