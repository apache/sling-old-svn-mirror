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

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class MockBundleContext implements BundleContext {
    private MockBundle bundle;

    public MockBundleContext(MockBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String getProperty(String s) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Bundle getBundle() {
        return bundle;

    }

    @Override
    public Bundle installBundle(String s) throws BundleException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Bundle installBundle(String s, InputStream inputStream)
            throws BundleException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Bundle getBundle(long l) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Bundle[] getBundles() {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void addServiceListener(ServiceListener serviceListener, String s)
            throws InvalidSyntaxException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void addServiceListener(ServiceListener serviceListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void removeServiceListener(ServiceListener serviceListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void addBundleListener(BundleListener bundleListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void removeBundleListener(BundleListener bundleListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void addFrameworkListener(FrameworkListener frameworkListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public void removeFrameworkListener(FrameworkListener frameworkListener) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public ServiceRegistration registerService(String[] strings, Object o,
            Dictionary dictionary) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public ServiceRegistration registerService(String s, Object o,
            Dictionary dictionary) {
        return new MockServiceRegistration();
    }

    @Override
    public ServiceReference[] getServiceReferences(String s, String s1)
            throws InvalidSyntaxException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public ServiceReference[] getAllServiceReferences(String s, String s1)
            throws InvalidSyntaxException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public ServiceReference getServiceReference(String s) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Object getService(ServiceReference serviceReference) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public boolean ungetService(ServiceReference serviceReference) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public File getDataFile(String s) {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public Filter createFilter(String s) throws InvalidSyntaxException {
        throw new UnsupportedOperationException("Not implemented");

    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory,
            Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(String location) {
        // TODO Auto-generated method stub
        return null;
    }
}
