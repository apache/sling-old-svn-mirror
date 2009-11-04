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
package org.apache.sling.adapter.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public class MockComponentContext implements ComponentContext {

    private Map<ServiceReference, Object> services = new HashMap<ServiceReference, Object>();

    public void addService(ServiceReference reference, Object service) {
        services.put(reference, service);
    }

    public Object locateService(String name, ServiceReference reference) {
        return services.get(reference);
    }

    public Object locateService(String name) {
        return null;
    }

    public Object[] locateServices(String name) {
        return null;
    }

    public void disableComponent(String name) {
    }

    public void enableComponent(String name) {
    }

    public BundleContext getBundleContext() {
        return new MockBundleContext();
    }

    public ComponentInstance getComponentInstance() {
        return null;
    }

    public Dictionary<?, ?> getProperties() {
        return null;
    }

    public ServiceReference getServiceReference() {
        return null;
    }

    public Bundle getUsingBundle() {
        return null;
    }

}
