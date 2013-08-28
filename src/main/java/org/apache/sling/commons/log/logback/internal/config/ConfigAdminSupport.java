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

package org.apache.sling.commons.log.logback.internal.config;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class ConfigAdminSupport {

    private ServiceRegistration loggingConfigurable;

    private ServiceRegistration writerConfigurer;

    private ServiceRegistration configConfigurer;

    public ConfigAdminSupport(BundleContext context, LogConfigManager logConfigManager) {
        // prepare registration properties (will be reused)
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        // register for official (global) configuration now
        props.put(Constants.SERVICE_PID, LogConfigManager.PID);
        props.put(Constants.SERVICE_DESCRIPTION, "LogManager Configuration Admin support");
        loggingConfigurable = context.registerService("org.osgi.service.cm.ManagedService",
            new ConfigurationServiceFactory(logConfigManager,
                "org.apache.sling.commons.log.logback.internal.config.GlobalConfigurator"), props);

        // register for log writer configuration
        ConfigurationServiceFactory msf = new ConfigurationServiceFactory(logConfigManager,
            "org.apache.sling.commons.log.logback.internal.config.LogWriterManagedServiceFactory");
        props.put(Constants.SERVICE_PID, LogConfigManager.FACTORY_PID_WRITERS);
        props.put(Constants.SERVICE_DESCRIPTION, "LogWriter configurator");
        writerConfigurer = context.registerService("org.osgi.service.cm.ManagedServiceFactory", msf, props);

        // register for log configuration
        msf = new ConfigurationServiceFactory(logConfigManager,
            "org.apache.sling.commons.log.logback.internal.config.LoggerManagedServiceFactory");
        props.put(Constants.SERVICE_PID, LogConfigManager.FACTORY_PID_CONFIGS);
        props.put(Constants.SERVICE_DESCRIPTION, "Logger configurator");
        configConfigurer = context.registerService("org.osgi.service.cm.ManagedServiceFactory", msf, props);

    }

    public void shutdown() {
        if (loggingConfigurable != null) {
            loggingConfigurable.unregister();
            loggingConfigurable = null;
        }

        if (writerConfigurer != null) {
            writerConfigurer.unregister();
            writerConfigurer = null;
        }

        if (configConfigurer != null) {
            configConfigurer.unregister();
            configConfigurer = null;
        }
    }
}
