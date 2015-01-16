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
package org.apache.sling.settings.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class SettingsServiceConfigurator implements ManagedService {

    private final SlingSettingsServiceImpl settings;

    private final ServiceRegistration managedServiceReg;

    public SettingsServiceConfigurator(final BundleContext btx,
            final SlingSettingsServiceImpl s) {
        this.settings = s;
        // setup manager service for configuration handling
        final Dictionary<String, String> msProps = new Hashtable<String, String>();
        msProps.put(Constants.SERVICE_PID, s.getClass().getName());
        msProps.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Managed Service for the Settings Service");
        msProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        managedServiceReg = btx.registerService(ManagedService.class.getName(), this, msProps);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void updated(final Dictionary properties) throws ConfigurationException {
        if ( properties != null ) {
            this.settings.update(properties);
        }
    }

    public void destroy() {
        managedServiceReg.unregister();
    }
}
