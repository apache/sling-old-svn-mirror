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
package org.apache.sling.testing.mock.caconfig;

import static org.apache.sling.caconfig.impl.ConfigurationNameConstants.CONFIGURATION_CLASSES_HEADER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.ManifestScanner;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Helper methods for registering Configuration annotation classes from the classpath.
 */
final class ConfigurationMetadataUtil {
    
    private static final String[] CONFIGURATION_CLASSES_FROM_MANIFEST;
    
    static {
        // scan classpath for configuration classes bundle header entries only once
        CONFIGURATION_CLASSES_FROM_MANIFEST = toArray(ManifestScanner.getValues(CONFIGURATION_CLASSES_HEADER));
    }
    
    private ConfigurationMetadataUtil() {
        // static methods only
    }

    private static String[] toArray(Collection<String> values) {
        return values.toArray(new String[values.size()]);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(BundleContext bundleContext, String... classNames) {
        Bundle bundle = new RegisterConfigurationMetadataBundle(bundleContext, Bundle.ACTIVE, classNames);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(bundleContext, event);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(BundleContext bundleContext, Class... classes) {
        String[] classNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classNames[i] = classes[i].getName();
        }
        registerAnnotationClasses(bundleContext, classNames);
    }
    
    /**
     * Scan MANIFEST.MF in the classpath and automatically register all Configuration annotation classes found.
     * @param bundleContext Bundle context
     */
    public static void addAnnotationClassesForManifestEntries(BundleContext bundleContext) {
        if (CONFIGURATION_CLASSES_FROM_MANIFEST.length > 0) {
            registerAnnotationClasses(bundleContext, CONFIGURATION_CLASSES_FROM_MANIFEST);
        }
    }
    

    private static class RegisterConfigurationMetadataBundle implements Bundle {
        
        private final BundleContext bundleContext;
        private final int state;
        private final String classNames;

        public RegisterConfigurationMetadataBundle(BundleContext bundleContext, int state, String[] classNames) {
            this.bundleContext = bundleContext;
            this.state = state;
            this.classNames = normalizeValueList(classNames);
        }
        
        private String normalizeValueList(String[] values) {
            if (values == null || values.length == 0) {
                return null;
            }
            return StringUtils.join(values, ",");
        }

        @Override
        public int getState() {
            return this.state;
        }

        @Override
        public Dictionary<String,String> getHeaders() {
            Dictionary<String, String> headers = new Hashtable<String, String>();
            headers.put(CONFIGURATION_CLASSES_HEADER, classNames);
            return headers;
        }

        @Override
        public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
            return new Vector<URL>().elements();
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
            return 0;
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
            return null;
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
        public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
            return null;
        }

        @Override
        public Version getVersion() {
            return null;
        }

        @Override
        public int compareTo(Bundle o) {
            return 0;
        }

        @Override
        public <A> A adapt(Class<A> type) {
            return null;
        }

        @Override
        public File getDataFile(String filename) {
            return null;
        }
        
    }

}
