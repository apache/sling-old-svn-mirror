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

    public boolean ungetService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    public void removeFrameworkListener(FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    public void removeBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle installBundle(String location) throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference getServiceReference(String clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getProperty(String key) {
        return null;
    }

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

    public Bundle[] getBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle getBundle(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle getBundle() {
        return new Bundle() {

            public int getState() {
                // TODO Auto-generated method stub
                return 0;
            }

            public void start(int options) throws BundleException {
                // TODO Auto-generated method stub

            }

            public void start() throws BundleException {
                // TODO Auto-generated method stub

            }

            public void stop(int options) throws BundleException {
                // TODO Auto-generated method stub

            }

            public void stop() throws BundleException {
                // TODO Auto-generated method stub

            }

            public void update(InputStream input) throws BundleException {
                // TODO Auto-generated method stub

            }

            public void update() throws BundleException {
                // TODO Auto-generated method stub

            }

            public void uninstall() throws BundleException {
                // TODO Auto-generated method stub

            }

            @SuppressWarnings("rawtypes")
            public Dictionary getHeaders() {
                // TODO Auto-generated method stub
                return null;
            }

            public long getBundleId() {
                // TODO Auto-generated method stub
                return 0;
            }

            public String getLocation() {
                // TODO Auto-generated method stub
                return null;
            }

            public ServiceReference[] getRegisteredServices() {
                // TODO Auto-generated method stub
                return null;
            }

            public ServiceReference[] getServicesInUse() {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean hasPermission(Object permission) {
                // TODO Auto-generated method stub
                return false;
            }

            public URL getResource(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @SuppressWarnings("rawtypes")
            public Dictionary getHeaders(String locale) {
                // TODO Auto-generated method stub
                return null;
            }

            public String getSymbolicName() {
                return "test-bundle";
            }

            @SuppressWarnings("rawtypes")
            public Class loadClass(String name) throws ClassNotFoundException {
                // TODO Auto-generated method stub
                return null;
            }

            @SuppressWarnings("rawtypes")
            public Enumeration getResources(String name) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @SuppressWarnings("rawtypes")
            public Enumeration getEntryPaths(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            public URL getEntry(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            public long getLastModified() {
                // TODO Auto-generated method stub
                return 0;
            }

            @SuppressWarnings("rawtypes")
            public Enumeration findEntries(String path, String filePattern,
                    boolean recurse) {
                // TODO Auto-generated method stub
                return null;
            }

            public BundleContext getBundleContext() {
                // TODO Auto-generated method stub
                return null;
            }

            @SuppressWarnings("rawtypes")
            public Map getSignerCertificates(int signersType) {
                // TODO Auto-generated method stub
                return null;
            }

            public Version getVersion() {
                // TODO Auto-generated method stub
                return null;
            }

            public <A> A adapt(Class<A> type) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    public void addServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub

    }

    public void addFrameworkListener(FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    public void addBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }
}