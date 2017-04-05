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
package org.apache.sling.models.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * Helper methods for simulating sling models bundle events
 * for registering sling model classes in ModelAdapterFactory.
 */
public final class ModelAdapterFactoryUtil {
    
    private ModelAdapterFactoryUtil() {
        // static methods only
    }

    /**
     * Scan classpaths for given package name (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageName Java package name
     */
    public static void addModelsForPackage(BundleContext bundleContext, Class... classes) {
        Bundle bundle = new ModelsPackageBundle(classes, Bundle.ACTIVE, bundleContext);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(bundleContext, event);
    }

    private static class ModelsPackageBundle implements Bundle {

        private final Class[] classes;
        private final int state;
        private final BundleContext bundleContext;

        public ModelsPackageBundle(Class[] classes, int state, BundleContext bundleContext) {
            this.classes = classes;
            this.state = state;
            this.bundleContext = bundleContext;
        }

        @Override
        public int getState() {
            return this.state;
        }

        @Override
        public Dictionary getHeaders() {
            Dictionary<String, Object> headers = new Hashtable<String, Object>();
            headers.put("Sling-Model-Packages", "dummy.package");
            return headers;
        }

        @Override
        public Enumeration findEntries(String path, String filePattern, boolean recurse) {
            Vector<URL> urls = new Vector<URL>(); // NOPMD
            for (int i = 0; i < classes.length; i++) {
                try {
                    urls.add(new URL("file:/" + classes[i].getName().replace('.', '/') + ".class"));
                }
                catch (MalformedURLException ex) {
                    throw new RuntimeException("Malformed URL.", ex);
                }
            }
            return urls.elements();
        }

        @Override
        public Class loadClass(String name) throws ClassNotFoundException {
            return getClass().getClassLoader().loadClass(name);
        }

        @Override
        public BundleContext getBundleContext() {
            return bundleContext;
        }

        @Override
        public void start(int options) throws BundleException {
            // do nothing
        }

        @Override
        public void start() throws BundleException {
            // do nothing
        }

        @Override
        public void stop(int options) throws BundleException {
            // do nothing
        }

        @Override
        public void stop() throws BundleException {
            // do nothing
        }

        @Override
        public void update(InputStream input) throws BundleException {
            // do nothing
        }

        @Override
        public void update() throws BundleException {
            // do nothing
        }

        @Override
        public void uninstall() throws BundleException {
            // do nothing
        }

        @Override
        public long getBundleId() {
            return 0;
        }

        @Override
        public String getLocation() {
            return null;
        }

        @Override
        public ServiceReference[] getRegisteredServices() { // NOPMD
            return null;
        }

        @Override
        public ServiceReference[] getServicesInUse() { // NOPMD
            return null;
        }

        @Override
        public boolean hasPermission(Object permission) {
            return false;
        }

        @Override
        public URL getResource(String name) {
            return null;
        }

        @Override
        public Dictionary getHeaders(String locale) {
            return null;
        }

        @Override
        public String getSymbolicName() {
            return null;
        }

        @Override
        public Enumeration getResources(String name) throws IOException {
            return null;
        }

        @Override
        public Enumeration getEntryPaths(String path) {
            return null;
        }

        @Override
        public URL getEntry(String path) {
            return null;
        }

        @Override
        public long getLastModified() {
            return 0;
        }

    }

}
