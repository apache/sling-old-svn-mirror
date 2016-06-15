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
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.resources.impl.common.AbstractReadableResourceProvider;
import org.apache.sling.distribution.resources.impl.common.SimplePathInfo;

/**
 * {@link ResourceProvider} for Osgi services for a specific service interface.
 * The main resource contains a list of service properties and can be adapted to the underlying service.
 * The accepted path is resourceRoot/{friendlyNameProperty}/childResourceName.
 */
public class DistributionServiceResourceProvider extends AbstractReadableResourceProvider {

    private final DistributionComponentKind kind;
    private final DistributionComponentProvider componentProvider;

    private static final String SERVICES_RESOURCE_TYPE = DistributionResourceTypes.DEFAULT_SERVICE_RESOURCE_TYPE;

    DistributionServiceResourceProvider(String kind,
                                        DistributionComponentProvider componentProvider,
                                        String resourceRoot) {
        super(resourceRoot);
        this.kind = DistributionComponentKind.fromName(kind);
        this.componentProvider = componentProvider;
    }

    @Override
    protected Map<String, Object> getInternalResourceProperties(ResourceResolver resolver, SimplePathInfo pathInfo) {
        if (pathInfo.isRoot()) {
            return getResourceRootProperties();
        } else if (pathInfo.isMain()) {
            return getResourceProperties(resolver, pathInfo.getMainResourceName());
        } else if (pathInfo.isChild()) {
            DistributionComponent component = componentProvider.getComponent(kind, pathInfo.getMainResourceName());

            if (component != null) {
                return getChildResourceProperties(component, pathInfo.getChildResourceName());
            }
        }

        return null;
    }


    @Override
    protected Iterable<String> getInternalResourceChildren(ResourceResolver resolver, SimplePathInfo pathInfo) {
        if (pathInfo.isMain()) {
            DistributionComponent component = componentProvider.getComponent(kind, pathInfo.getMainResourceName());

            if (component != null) {
                return getChildResourceChildren(component, pathInfo.getChildResourceName());
            }
        }

        return null;
    }

    private Map<String, Object> getResourceProperties(ResourceResolver resolver, String resourceName) {

        DistributionComponent component = componentProvider.getComponent(kind, resourceName);

        if (component != null) {
            Map<String, Object> properties = new HashMap<String, Object>();

            properties.put(DistributionComponentConstants.PN_NAME, resourceName);
            properties.put(INTERNAL_ADAPTABLE, component.getService());

            String resourceType = getResourceType(kind);
            properties.put(SLING_RESOURCE_TYPE, resourceType);
            return properties;
        }

        return null;
    }

    private Map<String, Object> getResourceRootProperties() {

        List<DistributionComponent> componentList = componentProvider.getComponents(kind);

        List<String> nameList = new ArrayList<String>();
        for (DistributionComponent component : componentList) {
            nameList.add(component.getName());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ITEMS, nameList.toArray(new String[nameList.size()]));

        String resourceType = getRootResourceType(kind);
        result.put(SLING_RESOURCE_TYPE, resourceType);

        return result;
    }


    private String getResourceType(DistributionComponentKind kind) {
        if (DistributionComponentKind.AGENT.equals(kind)) {
            return DistributionResourceTypes.AGENT_RESOURCE_TYPE;
        } else if (DistributionComponentKind.IMPORTER.equals(kind)) {
            return DistributionResourceTypes.IMPORTER_RESOURCE_TYPE;
        } else if (DistributionComponentKind.EXPORTER.equals(kind)) {
            return DistributionResourceTypes.EXPORTER_RESOURCE_TYPE;
        } else if (DistributionComponentKind.TRIGGER.equals(kind)) {
            return DistributionResourceTypes.TRIGGER_RESOURCE_TYPE;
        }

        return SERVICES_RESOURCE_TYPE;
    }

    private String getRootResourceType(DistributionComponentKind kind) {
        if (DistributionComponentKind.AGENT.equals(kind)) {
            return DistributionResourceTypes.AGENT_LIST_RESOURCE_TYPE;
        } else if (DistributionComponentKind.IMPORTER.equals(kind)) {
            return DistributionResourceTypes.IMPORTER_LIST_RESOURCE_TYPE;
        } else if (DistributionComponentKind.EXPORTER.equals(kind)) {
            return DistributionResourceTypes.EXPORTER_LIST_RESOURCE_TYPE;
        } else if (DistributionComponentKind.TRIGGER.equals(kind)) {
            return DistributionResourceTypes.TRIGGER_LIST_RESOURCE_TYPE;
        }

        return SERVICES_RESOURCE_TYPE;
    }

    Map<String, Object> getChildResourceProperties(DistributionComponent component, String childResourceName) {
        return null;
    }

    Iterable<String> getChildResourceChildren(DistributionComponent component, String childResourceName) {
        return null;
    }
}
