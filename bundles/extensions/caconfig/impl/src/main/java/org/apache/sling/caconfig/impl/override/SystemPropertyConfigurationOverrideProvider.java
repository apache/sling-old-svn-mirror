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
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Provides parameter override map from system properties.
 */
@Component(service = ConfigurationOverrideProvider.class, immediate = true)
@Designate(ocd = SystemPropertyConfigurationOverrideProvider.Config.class)
public final class SystemPropertyConfigurationOverrideProvider implements ConfigurationOverrideProvider {

    /**
     * Prefix for override system property
     */
    public static final String SYSTEM_PROPERTY_PREFIX = "sling.caconfig.override.";

    @ObjectClassDefinition(name = "Apache Sling Context-Aware Configuration Override Provider: System Properties",
            description = "Allows to override configuration property values from system environment properties.")
    public static @interface Config {

        @AttributeDefinition(name = "Enabled",
                description = "Enable this override provider.")
        boolean enabled() default false;

        @AttributeDefinition(name = "Service Ranking",
                description = "Priority of configuration override providers (higher = higher priority).")
        int service_ranking() default 200;

    }

    private Collection<String> overrideStrings;

    @Activate
    void activate(Config config) {
        List<String> overrides = new ArrayList<>();

        if (config.enabled()) {
            Properties properties = System.getProperties();
            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                Object keyObject = keys.nextElement();
                if (keyObject instanceof String) {
                    String key = (String) keyObject;
                    if (StringUtils.startsWith(key, SYSTEM_PROPERTY_PREFIX)) {
                        overrides.add(StringUtils.substringAfter(key, SYSTEM_PROPERTY_PREFIX) + "=" + System.getProperty(key));
                    }
                }
            }
        }

        this.overrideStrings = overrides;
    }

    @Override
    public Collection<String> getOverrideStrings() {
        return overrideStrings;
    }

}
