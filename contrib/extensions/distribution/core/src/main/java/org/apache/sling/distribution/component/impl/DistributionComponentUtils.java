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
package org.apache.sling.distribution.component.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * Helper class that facilitates the transformation of "json" like properties into OSGI configs.
 */
public class DistributionComponentUtils {
    public static final int MAX_DEPTH_LEVEL = 5;

    /**
     * Property representing component kind.
     */
    public static final String PN_KIND = "kind";

    /**
     * Property representing component type.
     */
    public static final String PN_TYPE = "type";

    /**
     * Property representing component name
     */
    public static final String PN_NAME = "name";

    /**
     * Special type value for binding to existing services by name.
     * packager : { "type" : "service", "name": "vlt" }
     * is transformed into
     * packager.target = (name=vlt)
     */
    public static final String TYPE_SERVICE = "service";

    /**
     * Special type value for binding to multiple subcomponents.
     * triggers : { "type" : "list", "kind": "trigger", "trigger1": { .... }, "trigger2": {...} }
     * is transformed into
     * triggers.target = (parent.ref.id=myname)
     */
    public static final String TYPE_LIST = "list";


    private static final String DESCRIPTOR_SEPARATOR = "|";
    private static final String NAME_SEPARATOR = "/";

    // ID properties used to represent references in a tree
    static final String PN_ID = "ref.id";
    static final String PN_PARENT_ID = "parent.ref.id";
    static final String PN_OWNER_ID = "owner.ref.id";


    /**
     * Returns the list of osgi configs in topological order
     * @param componentKind the kind of the component
     * @param componentName the name of the component
     * @param settings the map representation of the component dependencies
     * @return a list of osgi configs maps
     */
    public List<Map<String,Object>> transformToOsgi(String componentKind, String componentName, Map<String,Object> settings) {
        settings.put(PN_KIND, componentKind);
        settings.put(PN_NAME, componentName);
        settings.put(PN_ID, getComponentID(componentKind, componentName));
        settings.put(PN_OWNER_ID, getComponentID(componentKind, componentName));

        return transformToOsgiInternal(settings, 0);
    }


    private List<Map<String, Object>> transformToOsgiInternal(Map<String, Object> settings, int level) {

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        if (level > MAX_DEPTH_LEVEL) {
            return result;
        }

        String kind = PropertiesUtil.toString(settings.get(PN_KIND), null);
        String type = PropertiesUtil.toString(settings.get(PN_TYPE), null);
        String id = PropertiesUtil.toString(settings.get(PN_ID), null);
        String ownerId = PropertiesUtil.toString(settings.get(PN_OWNER_ID), null);


        if (TYPE_LIST.equals(type)) {
            id = PropertiesUtil.toString(settings.get(PN_PARENT_ID), null);
        }

        if (id == null || type == null || kind == null || ownerId == null) {
            return result;
        }

        if (!isSupportedKind(kind)) {
            return result;
        }

        Map<String, Object> currentConfig = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String property = entry.getKey();

            if (entry.getValue() instanceof Map) {
                Map subComponentSettings = (Map) entry.getValue();
                String subComponentType = PropertiesUtil.toString(subComponentSettings.get(PN_TYPE), null);

                if (TYPE_SERVICE.equals(subComponentType)) {
                    String subComponentName = PropertiesUtil.toString(subComponentSettings.get(PN_NAME), null);

                    currentConfig.put(property + ".target", "(" + PN_NAME + "=" + subComponentName + ")");
                }
                else {
                    subComponentSettings.put(PN_PARENT_ID, id);
                    subComponentSettings.put(PN_OWNER_ID, ownerId);

                    if (TYPE_LIST.equals(type)) {
                        subComponentSettings.put(PN_KIND, kind);
                    }

                    subComponentSettings.put(PN_ID, id + NAME_SEPARATOR + property);
                    List<Map<String, Object>> subcomponentConfigs = transformToOsgiInternal(subComponentSettings, level + 1);

                    currentConfig.put(property + ".target", "(" + PN_PARENT_ID + "=" + id + ")");
                    result.addAll(subcomponentConfigs);
                }
            }
            else {
                currentConfig.put(entry.getKey(), entry.getValue());
            }
        }

        if (!TYPE_LIST.equals(type)) {
            result.add(currentConfig);
        }

        return result;
    }


    public String  getFactoryPid(String kind, String type) {
        DistributionComponentKind componentKind = DistributionComponentKind.fromName(kind);
        if (componentKind != null) {
            componentKind.getFactory(type);
        }

        return null;
    }


    public List<String> getAllFactoryPids() {
        List<String> result = new ArrayList<String>();

        for (DistributionComponentKind kind : DistributionComponentKind.values()) {
            List<String> factories = kind.getFactories();
            result.addAll(factories);
        }

        return result;
    }




    public boolean isSupportedKind(String kind) {
        return DistributionComponentKind.fromName(kind) != null;
    }



    public String getComponentID(String componentKind, String componentName) {
        return componentKind + NAME_SEPARATOR + componentName;
    }
}
