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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Provides parameter override map from OSGi factory configuration.
 */
@Component(service = ConfigurationOverrideProvider.class, immediate = true)
@Designate(ocd = OsgiConfigurationOverrideProvider.Config.class, factory = true)
public final class OsgiConfigurationOverrideProvider implements ConfigurationOverrideProvider {

    @ObjectClassDefinition(name = "Apache Sling Context-Aware Configuration Override Provider: OSGi configuration",
            description = "Allows to override configuration property values from OSGi configurations.")
    public static @interface Config {

        @AttributeDefinition(name = "Description",
                description = "This description is used for display in the web console.")
        String description();

        @AttributeDefinition(name = "Overrides",
                description = "Override strings - examples:\n"
                + "{configName}/{propertyName}={propertyJsonValue}\n"
                + "{configName}={propertyJsonObject}\n"
                + "[{contextPath}]{configName}/{propertyName}={propertyJsonValue}\n"
                + "[{contextPath}]{configName}={propertyJsonObject}")
        String[] overrides();

        @AttributeDefinition(name = "Enabled",
                description = "Enable this override provider.")
        boolean enabled() default false;

        @AttributeDefinition(name = "Service Ranking",
                description = "Priority of configuration override providers (higher = higher priority).")
        int service_ranking() default 100;
        
        String webconsole_configurationFactory_nameHint() default "{description}, enabled={enabled}";

    }

    private Collection<String> overrideStrings;

    @Activate
    void activate(Config config) {
        List<String> overrides = new ArrayList<>();
        if (config.enabled()) {
            overrides.addAll(Arrays.asList(config.overrides()));
        }
        this.overrideStrings = overrides;
    }

    @Override
    public Collection<String> getOverrideStrings() {
        return overrideStrings;
    }
    
}
