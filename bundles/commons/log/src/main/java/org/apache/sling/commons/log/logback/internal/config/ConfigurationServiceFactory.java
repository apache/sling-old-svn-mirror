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

import org.apache.sling.commons.log.logback.internal.LogConfigManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ConfigurationServiceFactory implements ServiceFactory {

    private final LogConfigManager logConfigManager;

    private final String serviceClass;

    private int useCount;

    private Object service;

    public ConfigurationServiceFactory(final LogConfigManager logConfigManager, final String serviceClass) {
        this.logConfigManager = logConfigManager;
        this.serviceClass = serviceClass;
    }

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        if (service == null) {
            useCount = 1;
            service = createInstance();
        } else {
            useCount++;
        }
        return service;
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        useCount--;
        if (useCount <= 0) {
            service = null;
        }
    }

    private Object createInstance() {
        try {
            Class<?> type = getClass().getClassLoader().loadClass(serviceClass);
            Object instance = type.newInstance();
            if (instance instanceof LogConfigurator) {
                ((LogConfigurator) instance).setLogConfigManager(logConfigManager);
            }
            return instance;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create " + serviceClass + " instance", t);
        }
    }
}
