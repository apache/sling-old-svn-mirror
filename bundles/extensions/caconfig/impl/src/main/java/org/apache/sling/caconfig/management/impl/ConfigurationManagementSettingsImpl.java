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
package org.apache.sling.caconfig.management.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.sling.caconfig.management.ConfigurationManagementSettings;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ConfigurationManagementSettings.class)
@Designate(ocd=ConfigurationManagementSettingsImpl.Config.class)
public class ConfigurationManagementSettingsImpl implements ConfigurationManagementSettings {
    
    @ObjectClassDefinition(name="Apache Sling Context-Aware Configuration Management Settings",
            description="Management settings for reading and writing configurations.")
    static @interface Config {
        
        @AttributeDefinition(name="Ignore Property Regex",
                      description = "List of regular expressions with property names that should be ignored when reading or writing configuration data properties.")
        String[] ignorePropertyNameRegex() default {
            "^jcr:.+$"
        };

    }
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurationManagementSettingsImpl.class);
    
    private Pattern[] ignorePropertyNameRegex;
    
    
    @Activate
    private void activate(Config config) {
        List<Pattern> patterns = new ArrayList<>();
        for (String patternString : config.ignorePropertyNameRegex()) {
           try {
               patterns.add(Pattern.compile(patternString));
           }
           catch (PatternSyntaxException ex) {
               log.warn("Ignoring invalid regex pattern: " + patternString, ex);
           }
        }
        this.ignorePropertyNameRegex = patterns.toArray(new Pattern[patterns.size()]);
    }
    
    @Override
    public Set<String> getIgnoredPropertyNames(Set<String> propertyNames) {
        Set<String> ignoredPropertyNames = new HashSet<>();
        for (String propertyName : propertyNames) {
            for (Pattern ignorePattern : ignorePropertyNameRegex) {
                if (ignorePattern.matcher(propertyName).matches()) {
                    ignoredPropertyNames.add(propertyName);
                    break;
                }
            }
        }
        return ignoredPropertyNames;
    }

}
