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
package org.apache.sling.replication.resources.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.resources.impl.common.AbstractModifyingResourceProvider;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * {@link ResourceProvider} for Osgi configurations for a specific configuration factory.
 * All CRUD operations are supported for the underlying resources.
 * The accepted path is resourceRoot/{friendlyNameProperty}/childResourceName.
 */
public class OsgiConfigurationResourceProvider extends AbstractModifyingResourceProvider implements ResourceProvider, ModifyingResourceProvider {

    private final ConfigurationAdmin configurationAdmin;
    private final String configFactory;
    private final String friendlyNameProperty;
    private final String groupPrefix;

    public OsgiConfigurationResourceProvider(ConfigurationAdmin configurationAdmin,
                                             String configFactory,
                                             String friendlyNameProperty,
                                             String resourceRoot,
                                             String groupPrefix,
                                             Map<String, String> additionalResourceProperties) {
        super(resourceRoot, additionalResourceProperties);

        this.configurationAdmin = configurationAdmin;
        this.configFactory = configFactory;
        this.friendlyNameProperty = friendlyNameProperty;
        this.groupPrefix = groupPrefix;
    }

    @Override
    protected void save(ResourceResolver resourceResolver, Map<String, Map<String, Object>> changedResources,
                        Set<String> deletedResources) throws PersistenceException {
        try {
            for (Map.Entry<String, Map<String, Object>> entry : changedResources.entrySet()) {
                String resourceName = entry.getKey();
                Map<String, Object> properties = entry.getValue();

                String configName = getConfigName(resourceName);
                properties.put(friendlyNameProperty, configName);

                Configuration configuration = getConfiguration(configName);

                if (configuration == null) {
                    configuration = configurationAdmin.createFactoryConfiguration(configFactory);
                }

                properties = filterBeforeSave(properties);
                configuration.update(toDictionary(properties));
            }

            for (String resourceName : deletedResources) {
                String configName = getConfigName(resourceName);
                Configuration configuration = getConfiguration(configName);
                if (configuration != null) {
                    configuration.delete();
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Resources cannot be commited", e);
        }
    }

    @Override
    protected Map<String, Object> getResourceRootProperties() {
        Configuration[] configurations = getConfigurations(null);
        configurations = configurations == null ? new Configuration[0] : configurations;

        List<String> nameList = new ArrayList<String>();

        for (Configuration configuration : configurations) {
            String configName = (String) configuration.getProperties().get(friendlyNameProperty);

            if (configName != null) {
                nameList.add(configName);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", nameList.toArray(new String[nameList.size()]));

        return result;
    }

    @Override
    protected Map<String, Object> getResourceProperties(String resourceName) {

        String configName = getConfigName(resourceName);
        Configuration configuration = getConfiguration(configName);


        if (configuration == null) {
            return null;
        }

        if (!configFactory.equals(configuration.getFactoryPid())) {
            return null;
        }


        Map<String, Object> properties = toMap(configuration);
        return filterBeforeRead(properties);
    }


    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

    Configuration getConfiguration(String configName) {
        Configuration[] configurations = getConfigurations(configName);

        if (configurations == null || configurations.length != 1) {
            return null;
        }

        return configurations[0];
    }

    Configuration[] getConfigurations(String configName) {
        try {
            String filter = getFilter(configName);

            return configurationAdmin.listConfigurations(filter);
        } catch (IOException e) {
            return null;
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

    String getFilter(String configName) {
        if (configName != null) {
            return "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + OsgiUtils.escape(configFactory) + ")("
                    + friendlyNameProperty + "=" + OsgiUtils.escape(configName) + "))";
        } else if (configFactory != null) {
            return "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + OsgiUtils.escape(configFactory) + ")";
        }

        return null;
    }


    private Map<String, Object> toMap(Configuration configuration) {
        return fromDictionary(configuration.getProperties());
    }


    public static <K, V> Map<K, V> fromDictionary(Dictionary<K, V> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<K, V> map = new HashMap<K, V>(dictionary.size());
        Enumeration<K> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            K key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }

    public static <K, V> Dictionary<K, V> toDictionary(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        Dictionary<K, V> dictionary = new Hashtable<K, V>(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            dictionary.put(entry.getKey(), entry.getValue());
        }

        return dictionary;
    }

    private Map<String, Object>  filterBeforeSave(Map<String, Object> properties) {
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().endsWith(".target")) {
                String entryValue = (String) entry.getValue();
                entryValue = packOsgiFilter(entryValue);
                if (entryValue != null) {
                    result.put(entry.getKey(), entryValue);
                }
            }
            else {
                result.put(entry.getKey(), entry.getValue());
            }

        }

        return result;

    }

    private Map<String, Object>  filterBeforeRead(Map<String, Object> properties) {
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().endsWith(".target")) {
                String entryValue = (String) entry.getValue();
                entryValue = unpackOsgiFilter(entryValue);
                if (entryValue != null) {
                    result.put(entry.getKey(), entryValue);
                }
            }
            else {
                result.put(entry.getKey(), entry.getValue());
            }

        }

        return result;
    }


    private String unpackOsgiFilter(String propertyValue) {

        String result = null;

        String namePattern = "\\(" + friendlyNameProperty + "=(.*?)\\)";

        Pattern r = Pattern.compile(namePattern);
        Matcher m = r.matcher(propertyValue);

        if (m.matches()) {
            result = m.group(1);
        }

        return result;
    }

    private String packOsgiFilter(String propertyValue) {
        return "(" + friendlyNameProperty + "=" + OsgiUtils.escape(propertyValue) + ")";
    }

    private String getConfigName(String configName) {
        return groupPrefix == null? configName : groupPrefix + "/" + configName;
    }
}
