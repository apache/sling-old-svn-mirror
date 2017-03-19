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
package org.apache.sling.caconfig.management.impl.console;

import java.io.PrintWriter;

import org.apache.sling.caconfig.impl.ConfigurationPersistenceStrategyBridge;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Print original class information for a {@link ConfigurationPersistenceStrategy2} bridge service.
 */
class ConfigurationPersistenceStrategyPrinter implements ServiceConfigurationPrinter<ConfigurationPersistenceStrategy2> {

    @Override
    public void printConfiguration(PrintWriter pw, ServiceReference<ConfigurationPersistenceStrategy2> serviceReference, BundleContext bundleContext) {
        ConfigurationPersistenceStrategy2 service = bundleContext.getService(serviceReference);
        if (service instanceof ConfigurationPersistenceStrategyBridge.Adapter) {
            ConfigurationPersistenceStrategyBridge.Adapter adapter =
                    (ConfigurationPersistenceStrategyBridge.Adapter)service;
            pw.print(INDENT);
            pw.print(BULLET);
            pw.println("Delegates to " + adapter.getOriginalServiceClass().getName());
        }
        bundleContext.ungetService(serviceReference);
    }
    
}
