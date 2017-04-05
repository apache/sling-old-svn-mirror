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
package org.apache.sling.caconfig.impl.def;

import static org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.impl.ConfigurationResourceWrapper;
import org.apache.sling.caconfig.resource.impl.util.PropertyUtil;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=ConfigurationInheritanceStrategy.class)
@Designate(ocd=DefaultConfigurationInheritanceStrategy.Config.class)
public class DefaultConfigurationInheritanceStrategy implements ConfigurationInheritanceStrategy {

    @ObjectClassDefinition(name="Apache Sling Context-Aware Configuration Default Inheritance Strategy",
            description="Standardized resource inheritance for configurations.")
    public static @interface Config {
    
        @AttributeDefinition(name="Enabled",
                description = "Enable this configuration inheritance strategy.")
        boolean enabled() default true;
        
        @AttributeDefinition(name="Config property inheritance property names",
                description = "Additional property names to " + PROPERTY_CONFIG_PROPERTY_INHERIT + " to handle property inheritance. The names are used in the order defined, "
                     + "always starting with " + PROPERTY_CONFIG_PROPERTY_INHERIT + ". Once a property with a value is found, that value is used and the following property names are skipped.")
        String[] configPropertyInheritancePropertyNames();
    
    }

    private Config config;

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigurationInheritanceStrategy.class);
    
    @Activate
    private void activate(final Config config) {
        this.config = config;
    }
    
    @Override
    public Resource getResource(Iterator<Resource> configResources) {
        if (!config.enabled()) {
            return null;
        }
        if (!configResources.hasNext()) {
            return null;
        }
        Resource primary = configResources.next();
        if (!isPropertyInheritance(primary) || !configResources.hasNext()) {
            return primary;
        }
        Map<String,Object> mergedProps = getInheritedProperties(primary.getValueMap(), configResources);
        return new ConfigurationResourceWrapper(primary, new ValueMapDecorator(mergedProps));
    }
    
    private boolean isPropertyInheritance(Resource resource) {
        return PropertyUtil.getBooleanValueAdditionalKeys(resource.getValueMap(), PROPERTY_CONFIG_PROPERTY_INHERIT,
                config.configPropertyInheritancePropertyNames());
    }
    
    private Map<String,Object> getInheritedProperties(Map<String,Object> parentProps, Iterator<Resource> inheritanceChain) {
        if (!inheritanceChain.hasNext()) {
            return parentProps;
        }
        Resource next = inheritanceChain.next();
        log.trace("! Property inheritance: Merge with properties from {}", next.getPath());
        Map<String,Object> merged = new HashMap<>(next.getValueMap());
        merged.putAll(parentProps);
        if (isPropertyInheritance(next)) {
            return getInheritedProperties(merged, inheritanceChain);
        }
        else {
            return merged;
        }
    }

}
