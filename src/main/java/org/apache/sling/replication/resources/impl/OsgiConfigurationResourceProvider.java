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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public OsgiConfigurationResourceProvider(ConfigurationAdmin configurationAdmin,
                                             String configFactory,
                                             String friendlyNameProperty,
                                             String resourceRoot,
                                             Map<String, String> additionalResourceProperties) {
        super(resourceRoot, additionalResourceProperties);

        this.configurationAdmin = configurationAdmin;
        this.configFactory = configFactory;
        this.friendlyNameProperty = friendlyNameProperty;
    }

    @Override
    protected void save(ResourceResolver resourceResolver, Map<String, Map<String, Object>> changedResources,
                        Set<String> deletedResources) throws PersistenceException {
        try {
            for (Map.Entry<String, Map<String, Object>> entry : changedResources.entrySet()) {
                String configName = entry.getKey();
                Map<String, Object> properties = entry.getValue();

                Configuration configuration = getConfiguration(configName);

                if (configuration == null) {
                    configuration = configurationAdmin.createFactoryConfiguration(configFactory);
                }
                configuration.update(toDictionary(properties));
            }

            for (String configName : deletedResources) {
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

        List<String> nameList = new ArrayList<String>();

        for (Configuration configuration : configurations) {
            String configName = (String) configuration.getProperties().get(friendlyNameProperty);
            nameList.add(configName);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", nameList.toArray(new String[nameList.size()]));

        return result;
    }

    @Override
    protected Map<String, Object> getResourceProperties(String resourceName) {

        Configuration configuration = getConfiguration(resourceName);


        if (configuration == null) {
            return null;
        }

        if (!configFactory.equals(configuration.getFactoryPid())) {
            return null;
        }


        return toMap(configuration);
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

}
