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
package org.apache.sling.discovery.impl;

import java.util.Calendar;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;

/**
 * Example, default PropertyProvider which provides a few interesting static, as
 * well as a volatile, properties.
 * <p>
 * The volatile property is called 'infrastructure.lastPropertiesUpdate' and
 * allows to conclude when the properties were last read and propagated through
 * the topology
 */
// @Component - disable by default
@Service(value = { PropertyProvider.class })
@Properties({ @Property(name = PropertyProvider.PROPERTY_PROPERTIES, value = {
        "infrastructure.slingId", "infrastructure.slingHome",
        "infrastructure.lastPropertiesUpdate", "infrastructure.port" }) })
public class InfrastructurePropertyProvider implements PropertyProvider {

    @Reference
    private SlingSettingsService settingsService;

    /** the http port on which this instance is listening - provided as infrastructure.port property **/
    private String port = "";

    /**
     * Activate this property provider - this reads and stores the http port on which this instance is listening
     */
    protected void activate(final ComponentContext cc) {
        port = cc.getBundleContext().getProperty("org.osgi.service.http.port");

    }

    /**
     * Serve the properties
     */
    public String getProperty(final String name) {
        if (name.equals("infrastructure.slingId")) {
            return settingsService.getSlingId();
        } else if (name.equals("infrastructure.slingHome")) {
            return settingsService.getSlingHomePath();
        } else if (name.equals("infrastructure.lastPropertiesUpdate")) {
            return Calendar.getInstance().getTime().toString();
        } else if (name.equals("infrastructure.port")) {
            return port;
        }
        return null;
    }

}
