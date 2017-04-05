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
package org.apache.sling.testing.mock.sling.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.testing.mock.osgi.ManifestScanner;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.reflections.Reflections;

/**
 * Helper methods for registering Sling Models from the classpath.
 */
final class ModelAdapterFactoryUtil {
    
    private static final String PACKAGE_HEADER = "Sling-Model-Packages";
    private static final String CLASSES_HEADER = "Sling-Model-Classes";
    
    private static final String[] MODELS_PACKAGES_FROM_MANIFEST;
    private static final String[] MODELS_CLASSES_FROM_MANIFEST;
    
    private static final ConcurrentMap<String, List<URL>> MODEL_URLS_FOR_PACKAGES = new ConcurrentHashMap<String, List<URL>>();
    private static final ConcurrentMap<String, List<URL>> MODEL_URLS_FOR_CLASSES = new ConcurrentHashMap<String, List<URL>>();
    
    static {
        // suppress log entries from Reflections library
        Reflections.log = null;

        // scan classpath for models bundle header entries only once
        MODELS_PACKAGES_FROM_MANIFEST = toArray(ManifestScanner.getValues(PACKAGE_HEADER));
        MODELS_CLASSES_FROM_MANIFEST = toArray(ManifestScanner.getValues(CLASSES_HEADER));
    }
    
    private ModelAdapterFactoryUtil() {
        // static methods only
    }

    private static String[] toArray(Collection<String> values) {
        return values.toArray(new String[values.size()]);
    }
        
    /**
     * Search classpath for given java package names (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param bundleContext Bundle context
     * @param packageNames Java package names
     */
    public static void addModelsForPackages(BundleContext bundleContext, String... packageNames) {
        Bundle bundle = new RegisterModelsBundle(bundleContext, Bundle.ACTIVE, packageNames, null);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(bundleContext, event);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void addModelsForClasses(BundleContext bundleContext, String... classNames) {
        Bundle bundle = new RegisterModelsBundle(bundleContext, Bundle.ACTIVE, null, classNames);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(bundleContext, event);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void addModelsForClasses(BundleContext bundleContext, Class... classes) {
        String[] classNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classNames[i] = classes[i].getName();
        }
        addModelsForClasses(bundleContext, classNames);
    }
    
    /**
     * Scan MANIFEST.MF in the classpath and automatically register all sling model classes found.
     * @param bundleContext Bundle context
     */
    public static void addModelsForManifestEntries(BundleContext bundleContext) {
        if (MODELS_PACKAGES_FROM_MANIFEST.length > 0) {
            addModelsForPackages(bundleContext, MODELS_PACKAGES_FROM_MANIFEST);
        }
        if (MODELS_CLASSES_FROM_MANIFEST.length > 0) {
            addModelsForClasses(bundleContext, MODELS_CLASSES_FROM_MANIFEST);
        }
    }
    
    /**
     * Get model classes in list of packages (and subpackages), and cache result in static map.
     * @param packageNames Package names
     * @return List of URLs
     */
    private static Collection<URL> getModelClassUrlsForPackages(String packageNames) {
        List<URL> urls = MODEL_URLS_FOR_PACKAGES.get(packageNames);
        if (urls == null) {
            urls = new ArrayList<URL>();
            String[] packageNameArray = StringUtils.split(packageNames, ",");
            // add "." to each package name because it's a prefix, not a package name
            Object[] prefixArray = new Object[packageNameArray.length];
            for (int i = 0; i < packageNameArray.length; i++) {
                prefixArray[i] = packageNameArray[i] + ".";
            }
            Reflections reflections = new Reflections(prefixArray);
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Model.class);
            for (Class<?> clazz : classes) {
                urls.add(classToUrl(clazz));
            }
            MODEL_URLS_FOR_PACKAGES.putIfAbsent(packageNames, urls);
        }
        return urls;
    }
    
    /**
     * Get model classes in list of class names, and cache result in static map.
     * @param packageNames Class names
     * @return List of URLs
     */
    private static Collection<URL> getModelClassUrlsForClasses(String classNames) {
        List<URL> urls = MODEL_URLS_FOR_CLASSES.get(classNames);
        if (urls == null) {
            urls = new ArrayList<URL>();
            String[] packageNameArray = StringUtils.split(classNames, ",");
            for (String className : packageNameArray) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Model.class)) {
                        urls.add(classToUrl(clazz));
                    }
                }
                catch (ClassNotFoundException e) {
                    // ignore
                }
            }
            MODEL_URLS_FOR_CLASSES.putIfAbsent(classNames, urls);
        }
        return urls;
    }
    
    private static URL classToUrl(Class clazz) {
        try {
            return new URL("file:/" + clazz.getName().replace('.', '/') + ".class");
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Malformed URL.", ex);
        }
    }


    private static class RegisterModelsBundle implements Bundle {
        
        private static final String MAGIC_STRING = "MOCKS-YOU-KNOW-WHAT-TO-SCAN";

        private final BundleContext bundleContext;
        private final int state;
        private final String packageNames;
        private final String classNames;

        public RegisterModelsBundle(BundleContext bundleContext, int state, String[] packageNames, String[] classNames) {
            this.bundleContext = bundleContext;
            this.state = state;
            this.packageNames = normalizeValueList(packageNames);
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
            headers.put(PACKAGE_HEADER, MAGIC_STRING);
            return headers;
        }

        @Override
        public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
            Vector<URL> urls = new Vector<URL>(); // NOPMD
            if (packageNames != null) {
                urls.addAll(getModelClassUrlsForPackages(packageNames));
            }
            if (classNames != null) {
                urls.addAll(getModelClassUrlsForClasses(classNames));
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
