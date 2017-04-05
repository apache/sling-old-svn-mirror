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
package org.apache.sling.caconfig.impl.metadata;

import static org.apache.sling.caconfig.impl.ConfigurationNameConstants.CONFIGURATION_CLASSES_HEADER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Helper methods for simulating events when deploying bundles with configuration annotation classes.
 */
final class BundleEventUtil {
    
    private static final AtomicLong BUNDLE_COUNTER = new AtomicLong(); 
    
    private BundleEventUtil() {
        // static methods only
    }

    /**
     * Simulate a bundle STARTED event with a given set of classes simulated to be found in the bundle's classpath. 
     */
    public static Bundle startDummyBundle(BundleContext bundleContext, Class... classes) {
        DummyBundle bundle = new DummyBundle(bundleContext, classes);
        bundle.setState(Bundle.ACTIVE);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(bundleContext, event);
        return bundle;
    }

    /**
     * Simulate a bundle STARTED event with a given set of classes simulated to be found in the bundle's classpath. 
     */
    public static void stopDummyBundle(Bundle bundle) {
        ((DummyBundle)bundle).setState(Bundle.RESOLVED);
        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);
        MockOsgi.sendBundleEvent(bundle.getBundleContext(), event);
    }

    private static class DummyBundle implements Bundle {

        private final BundleContext bundleContext;
        private final Class[] classes;
        private final Long bundleId;
        private int state = Bundle.UNINSTALLED;
        private final String classNames;

        public DummyBundle(BundleContext bundleContext, Class[] classes) {
            this.bundleContext = bundleContext;
            this.classes = classes;
            this.bundleId = BUNDLE_COUNTER.incrementAndGet();
            
            StringBuilder sb = new StringBuilder();
            for (Class clazz : classes) {
                sb.append(clazz.getName()).append(",");
            }
            classNames = sb.toString();
        }

        @Override
        public int getState() {
            return this.state;
        }
        
        public void setState(int state) {
            this.state = state;
        }

        @Override
        public Dictionary<String,String> getHeaders() {
            Dictionary<String,String> headers = new Hashtable<>();
            headers.put(CONFIGURATION_CLASSES_HEADER, classNames);
            return headers;
        }

        @Override
        public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
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
        public Class<?> loadClass(String name) throws ClassNotFoundException {
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
            return bundleId;
        }

        @Override
        public String getLocation() {
            return null;
        }

        @Override
        public ServiceReference<?>[] getRegisteredServices() { // NOPMD
            return null;
        }

        @Override
        public ServiceReference<?>[] getServicesInUse() { // NOPMD
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
        public Dictionary<String,String> getHeaders(String locale) {
            return null;
        }

        @Override
        public String getSymbolicName() {
            return "DummyBundle" + bundleId;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return null;
        }

        @Override
        public Enumeration<String> getEntryPaths(String path) {
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

        @Override
        public int compareTo(Bundle obj) {
            if (obj instanceof DummyBundle) {
                return bundleId.compareTo(((DummyBundle)obj).bundleId);
            }
            return -1;
        }

        @Override
        public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
            return null;
        }

        @Override
        public Version getVersion() {
            return null;
        }

        @Override
        public <A> A adapt(Class<A> type) {
            return null;
        }

        @Override
        public File getDataFile(String filename) {
            return null;
        }

        @Override
        public int hashCode() {
            return bundleId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DummyBundle) {
                return bundleId.equals(((DummyBundle)obj).bundleId);
            }
            return false;
        }

        @Override
        public String toString() {
            return getSymbolicName();
        }

    }

}
