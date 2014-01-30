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
package org.apache.sling.featureflags.impl;

import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.osgi.framework.Constants;

@Component(
        name = "org.apache.sling.featureflags.Feature",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        label = "Apache Sling Configured Feature",
        description = "Allows for the definition of statically configured features which are defined and enabled through OSGi configuration")
@Service
public class ConfiguredFeature implements Feature {

    private static final String PROP_FEATURE = "feature";

    @Property(label = "Name", description = "Short name of this feature. This name is used "
        + "to refer to this feature when checking for it to be enabled or not. This "
        + "property is required and defaults to a name derived from the feature's class "
        + "name and object identity. It is strongly recommended to define a useful and unique for the feature")
    private static final String NAME = "name";

    @Property(label = "Description", description = "Description for the feature. The "
        + "intent is to descibe the behaviour of the application if this feature would be "
        + "enabled. It is recommended to define this property. The default value is the value of the name property.")
    private static final String DESCRIPTION = "description";

    @Property(boolValue = false, label = "Enabled", description = "Boolean flag indicating whether the feature is "
        + "enabled or not by this configuration")
    private static final String ENABLED = "enabled";

    private String name;

    private String description;

    private boolean enabled;

    @Activate
    private void activate(final Map<String, Object> configuration) {
        final String pid = PropertiesUtil.toString(configuration.get(Constants.SERVICE_PID), getClass().getName() + "$"
            + System.identityHashCode(this));
        this.name = PropertiesUtil.toString(configuration.get(NAME), pid);
        this.description = PropertiesUtil.toString(configuration.get(DESCRIPTION), this.name);
        this.enabled = PropertiesUtil.toBoolean(configuration.get(ENABLED), false);
    }

    @Override
    public boolean isEnabled(ExecutionContext context) {

        // Request Parameter Override
        ServletRequest request = context.getRequest();
        if (request != null) {
            String[] features = request.getParameterValues(PROP_FEATURE);
            if (features != null) {
                for (String feature : features) {
                    if (this.name.equals(feature)) {
                        return true;
                    }
                }
            }
        }

        return this.enabled;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
