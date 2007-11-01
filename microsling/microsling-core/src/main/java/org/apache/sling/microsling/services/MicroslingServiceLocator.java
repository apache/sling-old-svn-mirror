/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.services;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.services.ServiceNotAvailableException;

/**
 * Poor man's ServiceLocator (no, poorer than that) which uses a static list of
 * services. This is mostly meant to introduce the ServiceLocator interface in
 * microsling. See Sling OSGi for the real McCoy.
 */

public class MicroslingServiceLocator implements ServiceLocator {

    protected final Map<Class<?>, Object> services = new HashMap<Class<?>, Object>();

    public void registerService(Class<?> serviceType, Object service) {
        services.put(serviceType, service);
    }

    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        // unchecked cast:
        return (ServiceType) services.get(type);
    }

    public <ServiceType> ServiceType getRequiredService(Class<ServiceType> type)
            throws ServiceNotAvailableException {
        ServiceType service = getService(type);
        if (service != null) {
            return service;
        }

        throw new ServiceNotAvailableException(type.getName());
    }

    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(
            Class<ServiceType> serviceType, String filter) {
        if (serviceType != null) {
            ServiceType service = getService(serviceType);
            if (service != null) {
                // unchecked cast:
                ServiceType[] services = (ServiceType[]) Array.newInstance(
                    serviceType, 1);
                services[0] = service;
                return services;
            }
        }

        return null;
    }

}
