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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager implementation which represents the distribution configurations as OSGI configuration.
 */
public class OsgiConfigurationManager implements DistributionConfigurationManager {
    final ConfigurationAdmin configurationAdmin;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public OsgiConfigurationManager(ConfigurationAdmin configurationAdmin) {

        this.configurationAdmin = configurationAdmin;
    }


    public List<DistributionConfiguration> getConfigs(ResourceResolver resolver, DistributionComponentKind kind) {
        List<Configuration> configurations = getOsgiConfigurations(kind, null);

        List<DistributionConfiguration> result = new ArrayList<DistributionConfiguration>();
        if (configurations == null || configurations.isEmpty()) {
            return result;
        }


        for (Configuration configuration : configurations) {
            @SuppressWarnings( "unchecked" )
            Dictionary<String, Object> propertiesDict = configuration.getProperties();
            Map<String, Object> properties = OsgiUtils.fromDictionary(propertiesDict);

            properties = filterBeforeRead(properties);
            String name = PropertiesUtil.toString(properties.get(DistributionComponentConstants.PN_NAME), null);
            result.add(new DistributionConfiguration(kind, name, properties));

        }

        return result;
    }

    public DistributionConfiguration getConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {
        List<Configuration> configurations = getOsgiConfigurations(kind, name);

        if (configurations == null || configurations.isEmpty()) {
            return null;
        }

        if (configurations.size() > 1) {
            log.warn("Found more than one configuration of kind: {} and with name: {}",
                    new String[]{kind.getName(), name});
        }

        Configuration configuration = configurations.get(0);

        if (configuration != null) {
            @SuppressWarnings( "unchecked" )
            Dictionary<String, Object> properties = configuration.getProperties();
            Map<String, Object> result = OsgiUtils.fromDictionary(properties);

            String factoryPid = PropertiesUtil.toString(result.get(ConfigurationAdmin.SERVICE_FACTORYPID), null);
            String type = kind.getType(factoryPid);

            result.put(DistributionComponentConstants.PN_TYPE, type);
            result = filterBeforeRead(result);
            return new DistributionConfiguration(kind, name, result);
        }

        return null;
    }

    public void saveConfig(ResourceResolver resolver, DistributionConfiguration config) {
        String componentName = config.getName();
        DistributionComponentKind componentKind = config.getKind();
        Map<String, Object> properties = config.getProperties();
        String componentType = PropertiesUtil.toString(properties.get(DistributionComponentConstants.PN_TYPE), null);

        if (componentKind == null || componentType == null) {
            throw new IllegalArgumentException("kind and type are required " + componentKind + componentType);
        }

        String factoryPid = componentKind.getFactory(componentType);
        if (factoryPid != null) {

            // SLING-5872 - Management of agent configurations must identify configurations by name
            // Remove the agents with the same name wich are not bind to the same factory.
            List<Configuration> configs = getOsgiConfigurations(componentKind, componentName);
            for (Iterator<Configuration> iter = configs.iterator() ; iter.hasNext() ; ) {
                Configuration conf = iter.next();
                if (factoryPid.equals(conf.getFactoryPid())) {
                    iter.remove();
                }
            }
            deleteOsgiConfigs(configs);

            properties.put(DistributionComponentConstants.PN_NAME, componentName);
            saveOsgiConfig(factoryPid, componentName, properties);
        }

    }

    public void deleteConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {
        List<Configuration> configs = getOsgiConfigurations(kind, name);

        deleteOsgiConfigs(configs);

        log.info("Delete component {}", name);

    }


    private void deleteOsgiConfigs(List<Configuration> configurations) {
        for (Configuration configuration : configurations) {
            String pid = configuration.getPid();

            try {
                configuration.delete();
                log.info("Deleted configuration {}", pid);
            } catch (IOException e) {
                log.warn("Cannot delete configuration {}", pid, e);
            }
        }
    }

    private List<Configuration> getOsgiConfigurations(DistributionComponentKind kind, String componentName) {
        List<String> factoryPids = kind.getFactories();

        List<Configuration> allConfigurations = new ArrayList<Configuration>();
        for (String factoryPid : factoryPids) {
            List<Configuration> configurations = getOsgiConfigurationsFromFactory(factoryPid, componentName);
            allConfigurations.addAll(configurations);
        }

        return allConfigurations;
    }


    private Configuration saveOsgiConfig(String factoryPid, String componentName, Map<String, Object> properties) {
        try {
            List<Configuration> configurations = getOsgiConfigurationsFromFactory(factoryPid, componentName);
            Configuration configuration = null;
            if (configurations == null || configurations.isEmpty()) {
                configuration = configurationAdmin.createFactoryConfiguration(factoryPid);
            } else {
                configuration = configurations.get(0);
            }

            properties = filterBeforeSave(properties);

            configuration.update(OsgiUtils.toDictionary(properties));

            return configuration;
        } catch (IOException e) {
            log.error("Cannot create configuration with factory {}", factoryPid, e);
        }

        return null;
    }

    private List<Configuration> getOsgiConfigurationsFromFactory(String factoryPid, String componentName) {
        List<Configuration> result = new ArrayList<Configuration>();

        try {
            String filter = OsgiUtils.getFilter(factoryPid, DistributionComponentConstants.PN_NAME, componentName);

            Configuration[] configurations = configurationAdmin.listConfigurations(filter);
            if (configurations != null) {
                result.addAll(Arrays.asList(configurations));
            }
        } catch (IOException e) {
            log.error("cannot get osgi configs", e);
        } catch (InvalidSyntaxException e) {
            log.error("cannot parse filter", e);
        }

        return result;
    }

    private Map<String, Object> filterBeforeSave(Map<String, Object> properties) {
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().endsWith(".target")) {
                String entryValue = (String) entry.getValue();
                entryValue = packOsgiFilter(entryValue);
                if (entryValue != null) {
                    result.put(entry.getKey(), entryValue);
                }
            } else {
                result.put(entry.getKey(), entry.getValue());
            }

        }

        result = OsgiUtils.sanitize(result);
        return result;
    }

    private Map<String, Object> filterBeforeRead(Map<String, Object> properties) {
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().endsWith(".target")) {
                String entryValue = (String) entry.getValue();
                entryValue = unpackOsgiFilter(entryValue);
                if (entryValue != null) {
                    result.put(entry.getKey(), entryValue);
                }
            } else {
                result.put(entry.getKey(), entry.getValue());
            }

        }

        result = OsgiUtils.sanitize(result);
        return result;
    }


    private String unpackOsgiFilter(String propertyValue) {

        String result = null;

        String namePattern = "\\(" + DistributionComponentConstants.PN_NAME + "=(.*?)\\)";

        Pattern r = Pattern.compile(namePattern);
        Matcher m = r.matcher(propertyValue);

        if (m.matches()) {
            result = m.group(1);
        }

        return result;
    }

    private String packOsgiFilter(String propertyValue) {
        return "(" + DistributionComponentConstants.PN_NAME + "=" + OsgiUtils.escape(propertyValue) + ")";
    }
}
