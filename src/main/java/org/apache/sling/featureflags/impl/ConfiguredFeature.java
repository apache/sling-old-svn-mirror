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

import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(ocd = ConfiguredFeature.Config.class, factory = true)
@Component(service = Feature.class,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = {
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public class ConfiguredFeature implements Feature {

    @ObjectClassDefinition(name = "Apache Sling Configured Feature",
            description = "Allows for the definition of statically configured features which are defined and enabled through OSGi configuration",
            factoryPid = "org.apache.sling.featureflags.Feature")
    public static @interface Config {

        @AttributeDefinition(name = "Name", description = "Short name of this feature. This name is used "
            + "to refer to this feature when checking for it to be enabled or not. This "
            + "property is required and defaults to a name derived from the feature's class "
            + "name and object identity. It is strongly recommended to define a useful and unique for the feature")
        String name();

        @AttributeDefinition(name = "Description", description = "Description for the feature. The "
                + "intent is to descibe the behaviour of the application if this feature would be "
                + "enabled. It is recommended to define this property. The default value is the value of the name property.")
        String description();

        @AttributeDefinition(name = "Enabled", description = "Boolean flag indicating whether the feature is "
                + "enabled or not by this configuration")
        boolean enabled() default false;
    }

    private static final String PROP_FEATURE = "feature";


    private String name;

    private String description;

    private boolean enabled;

    @Activate
    private void activate(final Config config, final Map<String, Object> properties) {
        this.name = config.name();
        if ( this.name == null ) {
            Object pid = properties.get(Constants.SERVICE_PID);
            if ( pid == null ) {
                this.name = getClass().getName() + "$" + System.identityHashCode(this);
            } else {
                this.name = pid.toString();
            }

        }
        this.description = config.description();
        if ( this.description == null ) {
            this.description = this.name;
        }
        this.enabled = config.enabled();
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
