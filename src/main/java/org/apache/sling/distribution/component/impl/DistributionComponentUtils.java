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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.impl.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporterFactory;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.vlt.FileVaultDistributionPackageBuilderFactory;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.impl.LocalDistributionTriggerFactory;

public class DistributionComponentUtils {
    public static final String DEFAULT_TARGET = "(name=)";
    public static final String KIND = "kind";
    public static final String TYPE = "type";
    public static final String TARGET_DESCRIPTOR_SEPARATOR = "|";
    public static final String NAME_SEPARATOR = "/";

    public static final String TYPE_SERVICE = "service";
    public static final String TYPE_LIST = "list";

    public static final String NAME = "name";
    public static final String PARENT_NAME = "parent.name";

    public static final String OWNER_NAME = "owner.name";

//    // kind|type=osgiFactory
//    static String[] osgiConfigFactories = new String [] {
//            "agent|simple=org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory",
//
//            "exporter|local=org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory",
//            "exporter|remote=org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporterFactory",
//            "exporter|agent=org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory",
//
//            "importer|local=org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory",
//            "importer|remote=org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporterFactory",
//
//            "packager|vlt=org.apache.sling.distribution.serialization.impl.vlt.FileVaultDistributionPackageBuilderFactory",
//
//            "requestAuthorization|privilege=org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy",
//
//            "transportAuthenticator|user=org.apache.sling.distribution.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory",
//
//            "trigger|resourceEvent=org.apache.sling.distribution.trigger.impl.LocalDistributionTriggerFactory",
//            "trigger|scheduledEvent=org.apache.sling.distribution.trigger.impl.LocalDistributionTriggerFactory",
//            "trigger|distributionEvent=org.apache.sling.distribution.trigger.impl.LocalDistributionTriggerFactory",
//            "trigger|persistedJcrEvent=org.apache.sling.distribution.trigger.impl.LocalDistributionTriggerFactory",
//
//    };
//
//    // kind=osgiService
//    static String[] osgiServices = new String [] {
//            "agent=org.apache.sling.distribution.agent.DistributionAgent",
//            "exporter=org.apache.sling.distribution.packaging.DistributionPackageExporter",
//            "importer=org.apache.sling.distribution.packaging.DistributionPackageImporter",
//            "packager=org.apache.sling.distribution.serialization.DistributionPackageBuilder",
//            "requestAuthorization=org.apache.sling.distribution.agent.DistributionRequestAuthorizationStrategy",
//            "transportAuthenticator=org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider",
//            "trigger=org.apache.sling.distribution.trigger.DistributionTrigger",
//
//    };

    static  {
        osgiConfigFactoryMap = new HashMap<String, String>();
        osgiServiceMap = new HashMap<String, String>();

        addService("agent", DistributionAgent.class);
        addService("exporter", DistributionPackageExporter.class);
        addService("importer", DistributionPackageImporter.class);
        addService("packager", DistributionPackageBuilder.class);
        addService("requestAuthorization", DistributionRequestAuthorizationStrategy.class);
        addService("transportAuthenticator", TransportAuthenticationProvider.class);
        addService("trigger", DistributionTrigger.class);


        addFactory("agent" ,"simple", SimpleDistributionAgentFactory.class);

        addFactory("exporter", "local", LocalDistributionPackageExporterFactory.class);
        addFactory("exporter", "remote", RemoteDistributionPackageExporterFactory.class);
        addFactory("exporter", "agent", AgentDistributionPackageExporterFactory.class);

        addFactory("importer", "local", LocalDistributionPackageImporterFactory.class);
        addFactory("importer", "remote", RemoteDistributionPackageImporterFactory.class);

        addFactory("packager", "vlt", FileVaultDistributionPackageBuilderFactory.class);

        addFactory("requestAuthorization", "privilege", PrivilegeDistributionRequestAuthorizationStrategy.class);

        addFactory("transportAuthenticator", "user", UserCredentialsTransportAuthenticationProviderFactory.class);

        addFactory("trigger", "resourceEvent", LocalDistributionTriggerFactory.class);
        addFactory("trigger", "scheduledEvent", LocalDistributionTriggerFactory.class);
        addFactory("trigger", "distributionEvent", LocalDistributionTriggerFactory.class);
        addFactory("trigger", "persistedJcrEvent", LocalDistributionTriggerFactory.class);

    }


    final static Map<String, String> osgiConfigFactoryMap;
    final static Map<String, String> osgiServiceMap;



    public DistributionComponentUtils() {

    }


    public Map<String, Map<String, Object>> transformToOsgi(Map<String, Object> settings) {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();

        String kind = PropertiesUtil.toString(settings.get(KIND), null);
        String type = PropertiesUtil.toString(settings.get(TYPE), null);
        String name = PropertiesUtil.toString(settings.get(NAME), null);


        if (TYPE_LIST.equals(type)) {
            name = PropertiesUtil.toString(settings.get(PARENT_NAME), null);
        }

        if (name == null || type == null || kind == null) {
            return result;
        }

        if (!osgiServiceMap.containsKey(kind)) {
            return result;
        }

        Map<String, Object> currentConfig = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String property = entry.getKey();

            if (entry.getValue() instanceof Map) {
                Map subComponentSettings = (Map) entry.getValue();
                String subComponentType = PropertiesUtil.toString(subComponentSettings.get(TYPE), null);

                if (TYPE_SERVICE.equals(subComponentType)) {
                    String subComponentName = PropertiesUtil.toString(subComponentSettings.get(NAME), null);

                    currentConfig.put(property + ".target", "(" + NAME + "=" + subComponentName + ")");
                }
                else {
                    subComponentSettings.put(PARENT_NAME, name);

                    if (TYPE_LIST.equals(type)) {
                        subComponentSettings.put(KIND, kind);
                    }

                    subComponentSettings.put(NAME, name + NAME_SEPARATOR + property);
                    Map<String, Map<String, Object>> subcomponentConfigs = transformToOsgi(subComponentSettings);

                    currentConfig.put(property + ".target", "(" + PARENT_NAME + "=" + name + ")");
                    result.putAll(subcomponentConfigs);
                }
            }
            else {
                currentConfig.put(entry.getKey(), entry.getValue());
            }
        }

        if (!TYPE_LIST.equals(type)) {
            result.put(kind + TARGET_DESCRIPTOR_SEPARATOR + type + TARGET_DESCRIPTOR_SEPARATOR + name, currentConfig);
        }

        return result;
    }


    public Map<String, Object> transformFromOsgi(List<Map<String, Object>> osgiConfigs) {
        Map<String, Object> result = new HashMap<String, Object>();

        return result;
    }

    public String  getFactoryPid(String resultKey) {
        int index = resultKey.lastIndexOf(DistributionComponentUtils.TARGET_DESCRIPTOR_SEPARATOR);
        String key = resultKey.substring(0, index);
        return osgiConfigFactoryMap.get(key);
    }

    public String getKind(Class type) {
        for (Map.Entry<String, String> entry : osgiServiceMap.entrySet()) {
            if (type.getName().equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }


    private static void addService(String kind, Class serviceClass) {
        osgiServiceMap.put(kind, serviceClass.getName());
    }

    private static void addFactory(String kind, String type, Class factoryClass) {
        osgiConfigFactoryMap.put(kind + TARGET_DESCRIPTOR_SEPARATOR + type, factoryClass.getName());
    }
}
