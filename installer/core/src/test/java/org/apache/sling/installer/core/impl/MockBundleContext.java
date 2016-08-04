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
package org.apache.sling.installer.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class MockBundleContext implements BundleContext {

    @Override
    public boolean ungetService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public File getDataFile(String filename) {
        try {
            if ( "installer".equals(filename) ) {
                final File dir = this.getDataFile("test").getParentFile();
                dir.deleteOnExit();
                return new File(dir, filename);
            }
            final File f = File.createTempFile(filename, ".data");
            f.deleteOnExit();
            return f;
        } catch (final IOException ioe) {
            return null;
        }
    }

    @Override
    public Bundle[] getBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle() {
        return new Bundle() {

            @Override
            public int getState() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void start(int options) throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void start() throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void stop(int options) throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void stop() throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void update(InputStream input) throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void update() throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            public void uninstall() throws BundleException {
                // TODO Auto-generated method stub

            }

            @Override
            @SuppressWarnings("rawtypes")
            public Dictionary getHeaders() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long getBundleId() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getLocation() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ServiceReference[] getRegisteredServices() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ServiceReference[] getServicesInUse() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean hasPermission(Object permission) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public URL getResource(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Dictionary getHeaders(String locale) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getSymbolicName() {
                return "test-bundle";
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Class loadClass(String name) throws ClassNotFoundException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Enumeration getResources(String name) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Enumeration getEntryPaths(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public URL getEntry(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long getLastModified() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Enumeration findEntries(String path, String filePattern,
                    boolean recurse) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public BundleContext getBundleContext() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Map getSignerCertificates(int signersType) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Version getVersion() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <A> A adapt(Class<A> type) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Override
    public ServiceRegistration registerService(Class clazz, Object service, Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference getServiceReference(Class clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection getServiceReferences(Class clazz, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(String location) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }
}