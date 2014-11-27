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


import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DistributionComponentManager} implementation based on OSGI configs.
 * For each tree of properties a set of OSGI configs is generated and registered in ConfigurationAdmin.
 * To delete a component all configs owned by that component will be unregistered from ConfigurationAdmin.
 */
@Component
@Service(DistributionComponentManager.class)
public class DefaultDistributionComponentManager implements DistributionComponentManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    DistributionComponentUtils componentUtils = new DistributionComponentUtils();


    @Reference
    ConfigurationAdmin configurationAdmin;

    @Activate
    void activate() {
    }

    public void createComponent(@Nonnull String componentKind, @Nonnull String componentName, @Nonnull Map<String, Object> properties) {


        if (componentUtils.isSupportedKind(componentKind)) {
            String componentID = componentUtils.getComponentID(componentKind, componentName);


            List<Map<String, Object>> osgiConfigs = componentUtils.transformToOsgi(componentKind, componentName, properties);

            List<Configuration> createdConfigurations = new ArrayList<Configuration>();
            for(Map<String, Object> osgiConfig : osgiConfigs) {
                String kind = PropertiesUtil.toString(osgiConfig.get(DistributionComponentUtils.PN_KIND), null);
                String type = PropertiesUtil.toString(osgiConfig.get(DistributionComponentUtils.PN_TYPE), null);

                String factoryPid = componentUtils.getFactoryPid(kind, type);
                if (factoryPid != null) {
                    Configuration configuration = createOsgiConfig(factoryPid, osgiConfig);
                    if (configuration != null) {
                        createdConfigurations.add(configuration);
                    }
                    else {
                        deleteOsgiConfigs(createdConfigurations);
                        throw new RuntimeException("Cannot create all configurations");
                    }
                }
            }

            log.info("Component created {}", componentID);
        }
        else {
            throw new IllegalArgumentException("Unsupported kind " + componentKind);
        }
    }



    private Configuration createOsgiConfig(String factoryPid, Map<String, Object> properties) {
        try {
            String componentId = PropertiesUtil.toString(properties.get(DistributionComponentUtils.PN_ID), null);
            Configuration[] configurations = getOsgiConfigurations(factoryPid, componentId);
            Configuration configuration = null;
            if (configurations == null || configurations.length == 0) {
                configuration = configurationAdmin.createFactoryConfiguration(factoryPid);
            }
            else {
                configuration = configurations[0];
            }

            properties = OsgiUtils.sanitize(properties);

            configuration.update(OsgiUtils.toDictionary(properties));

            return configuration;
        } catch (IOException e) {
            log.error("Cannot create configuration with factory {}", factoryPid, e);
        }

        return null;
    }


    private Configuration[] getOsgiConfigurations(String factoryPid, String componentID) {
        try {
            String filter = OsgiUtils.getFilter(factoryPid, DistributionComponentUtils.PN_ID, componentID);

            return configurationAdmin.listConfigurations(filter);
        } catch (IOException e) {
            return null;
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

    private Configuration[] getOwnedConfigurations(String factoryPid, String ownerID) {
        try {
            String filter = OsgiUtils.getFilter(factoryPid, DistributionComponentUtils.PN_OWNER_ID, ownerID);

            return configurationAdmin.listConfigurations(filter);
        } catch (IOException e) {
            return null;
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }



    private List<Configuration> getAllOwnedConfigurations(String componentFullName) {
        List<Configuration> allConfigurations = new ArrayList<Configuration>();
        List<String> factoryPids = componentUtils.getAllFactoryPids();
        for (String factoryPid : factoryPids) {
            Configuration[] configurations = getOwnedConfigurations(factoryPid, componentFullName);

            if (configurations != null) {
                allConfigurations.addAll(Arrays.asList(configurations));
            }

        }

        return allConfigurations;
    }


    public void deleteComponent(@Nonnull String componentKind, String componentName) {
        String componentID = componentUtils.getComponentID(componentKind, componentName);
        List<Configuration> ownedConfigurations = getAllOwnedConfigurations(componentID);

        deleteOsgiConfigs(ownedConfigurations);

        log.info("Delete component {}", componentID);

    }


    private void deleteOsgiConfigs(List<Configuration> configurations) {
        for (Configuration configuration : configurations) {
            try {
                configuration.delete();
                log.info("Deleted configuration {}", configuration.getPid());
            } catch (IOException e) {
                log.error("Cannot delete configuration {}", configuration.getPid(), e);
            }
        }
    }


}
