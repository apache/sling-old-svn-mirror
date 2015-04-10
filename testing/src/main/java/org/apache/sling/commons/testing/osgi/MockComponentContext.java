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
package org.apache.sling.commons.testing.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public class MockComponentContext implements ComponentContext {

    private Dictionary<Object, Object> properties = new Properties();

    private MockBundleContext mockBundleContext;

    private Servlet servlet;

    private Map<ServiceReference, Object> services = new HashMap<ServiceReference, Object>();

    public MockComponentContext(MockBundle bundle) {
        mockBundleContext = new MockBundleContext(bundle);
    }
    
    public MockComponentContext(MockBundleContext mockBundleContext) {
        this.mockBundleContext = mockBundleContext;
    }

    public MockComponentContext(MockBundle bundle, Servlet servlet) {
        mockBundleContext = new MockBundleContext(bundle);
        this.servlet = servlet;
    }
    
    public MockComponentContext(MockBundleContext mockBundleContext, Servlet servlet) {
        this.mockBundleContext = mockBundleContext;
        this.servlet = servlet;
    }

    public void addService(ServiceReference reference, Object service) {
        services.put(reference, service);
    }


    public void setProperty(Object key, Object value) {
        // noinspection unchecked
        this.properties.put(key, value);
    }

    public Dictionary<Object, Object> getProperties() {
        // noinspection ReturnOfCollectionOrArrayField
        return this.properties;
    }

    public Object locateService(String name, ServiceReference reference) {
        // the constant is from Sling Core, but we don't want to have a dep just because of this
        String referenceName = (String) reference.getProperty("sling.core.servletName");
        if (referenceName != null && referenceName.equals(name)) {
            return this.servlet;
        }

        return services.get(reference);
    }

    public BundleContext getBundleContext() {
        return mockBundleContext;
    }

    public void disableComponent(String name) {
    }

    public void enableComponent(String name) {
    }

    public ComponentInstance getComponentInstance() {
        return null;
    }

    public ServiceReference getServiceReference() {
        return null;
    }

    public Bundle getUsingBundle() {
        return null;
    }

    public Object locateService(String name) {
        return null;
    }

    public Object[] locateServices(String name) {
        return null;
    }
}
