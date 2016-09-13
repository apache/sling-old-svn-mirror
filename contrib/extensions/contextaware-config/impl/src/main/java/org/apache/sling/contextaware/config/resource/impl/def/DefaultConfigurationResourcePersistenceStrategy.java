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
package org.apache.sling.contextaware.config.resource.impl.def;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourcePersistenceStrategy;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = ConfigurationResourcePersistenceStrategy.class)
@Designate(ocd=DefaultConfigurationResourcePersistenceStrategy.Config.class)
public class DefaultConfigurationResourcePersistenceStrategy implements ConfigurationResourcePersistenceStrategy {

    @ObjectClassDefinition(name="Apache Sling Context-Aware Default Configuration Resource Persistence Strategy",
            description="Directly uses configuration resources for storing configuration data.")
    static @interface Config {
        
        @AttributeDefinition(name="Enabled",
                      description = "Enable this configuration resource persistence strategy.")
        boolean enabled() default true;

    }

    private volatile Config config;

    @Activate
    private void activate(ComponentContext componentContext, Config config) {
        this.config = config; 
    }
        
    /**
     * The default persistence strategy is quite simple: directly use the configuration resources.
     */
    @Override
    public Resource getResource(Resource resource) {
        if (!config.enabled()) {
            return null;
        }
        return resource;
    }

}
