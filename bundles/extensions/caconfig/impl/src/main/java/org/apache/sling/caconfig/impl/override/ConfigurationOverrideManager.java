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
package org.apache.sling.caconfig.impl.override;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.impl.ConfigurationResourceWrapper;
import org.apache.sling.caconfig.resource.impl.util.MapUtil;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.commons.osgi.RankedServices.ChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects all {@link ConfigurationOverrideProvider} implementations in the container
 * and consolidates their result based on service ranking.
 */
@Component(service = ConfigurationOverrideManager.class,
reference={
        @Reference(name="configurationOverrideProvider", service=ConfigurationOverrideProvider.class,
                bind="bindConfigurationOverrideProvider", unbind="unbindConfigurationOverrideProvider",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ConfigurationOverrideManager implements ChangeListener {

    private RankedServices<ConfigurationOverrideProvider> items = new RankedServices<>(Order.DESCENDING, this);
    private volatile Collection<OverrideItem> allOverrides = Collections.emptyList();
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurationOverrideManager.class);
    
    protected void bindConfigurationOverrideProvider(ConfigurationOverrideProvider item, Map<String, Object> props) {
        items.bind(item, props);
    }
    
    protected void unbindConfigurationOverrideProvider(ConfigurationOverrideProvider item, Map<String, Object> props) {
        items.unbind(item, props);
    }
    
    /**
     * Checks if the whole configuration for the given context path and name is overridden.
     * @param contextPath Context path
     * @param configName Config name
     * @return true if the whole configuration is overridden.
     */
    public boolean isAllOverridden(String contextPath, String configName) {
        for (OverrideItem override : allOverrides) {
            if (StringUtils.equals(configName, override.getConfigName()) && override.matchesPath(contextPath)) {
                if (override.isAllProperties()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Override properties for given context path and configuration name.
     * @param contextPath Path of context resource for which configuration was resolved
     * @param configName Configuration name
     * @param properties Resolved configuration properties
     * @return Overwritten or replaced properties - or null if no override took place
     */
    public Map<String,Object> overrideProperties(String contextPath, String configName, Map<String,Object> properties) {
        if (allOverrides.size() == 0) {
            return null;
        }
        boolean anyMatch = false;
        Map<String,Object> overrideProperties = new HashMap<>(properties);
        
        for (OverrideItem override : allOverrides) {
            if (StringUtils.equals(configName, override.getConfigName()) && override.matchesPath(contextPath)) {
                if (override.isAllProperties()) {
                    overrideProperties.clear();
                }
                overrideProperties.putAll(override.getProperties());
                anyMatch = true;
            }
        }
        
        if (anyMatch) {
            return overrideProperties;
        }
        else {
            return null;
        }
    }
    
    /**
     * Override properties in given configuration resource (if any overrides are defined).
     * @param contextPath Context path
     * @param configName Configuration name
     * @param configResource Resolved configuration resource
     * @return Resource with overwritten configuration properties - or original configuration resource if no override took place
     */
    public Resource overrideProperties(String contextPath, String configName, Resource configResource) {
        if (configResource == null) {
            return null;
        }
        Map<String,Object> overrideProperties = overrideProperties(contextPath, configName, configResource.getValueMap());
        if (overrideProperties == null) {
            return configResource;
        }
        if (log.isTraceEnabled()) {
            log.trace("! Override properties for context path " + contextPath + ", name '" + configName + "', "
                    + "config path " + configResource.getPath() + ": "
                    + MapUtil.traceOutput(configResource.getValueMap()) + " -> " + MapUtil.traceOutput(overrideProperties));
        }
        return new ConfigurationResourceWrapper(configResource, new ValueMapDecorator(overrideProperties));
    }

    /**
     * If a provider is added or removed parse and collect all overrides again (to ensure correct overall order is preserved).
     */
    @Override
    public void changed() {
        List<OverrideItem> overrides = new ArrayList<>();
        for (ConfigurationOverrideProvider item : items) {
            Collection<OverrideItem> itemOverrides = OverrideStringParser.parse(item.getOverrideStrings());
            if (log.isDebugEnabled() && !itemOverrides.isEmpty()) {
                log.debug("Override items from " + item.getClass().getName() + ":\n" + StringUtils.join(itemOverrides, "\n"));
            }
            overrides.addAll(itemOverrides);
        }
        allOverrides = overrides;
    }

}
