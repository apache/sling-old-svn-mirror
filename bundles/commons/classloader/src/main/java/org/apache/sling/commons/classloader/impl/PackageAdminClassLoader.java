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
package org.apache.sling.commons.classloader.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>PackageAdminClassLoader</code> loads
 * classes and resources through the package admin service.
 */
class PackageAdminClassLoader extends ClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageAdminClassLoader.class);

    /** The package admin service. */
    private final PackageAdmin packageAdmin;

    /** The manager factory. */
    private final DynamicClassLoaderManagerFactory factory;

    /** A cache for resolved classes. */
    private Map<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

    /** Negative class cache. */
    private Set<String> negativeClassCache = Collections.synchronizedSet(new HashSet<String>());

    private Map<String, Bundle> packageProviders = new ConcurrentHashMap<>();

    /** A cache for resolved urls. */
    private Map<String, URL> urlCache = new ConcurrentHashMap<String, URL>();

    public PackageAdminClassLoader(final PackageAdmin pckAdmin,
                                   final ClassLoader parent,
                                   final DynamicClassLoaderManagerFactory factory) {
        super(parent);
        this.packageAdmin = pckAdmin;
        this.factory = factory;
    }

    /**
     * Returns <code>true</code> if the <code>bundle</code> is to be considered
     * active from the perspective of declarative services.
     * <p>
     * As of R4.1 a bundle may have lazy activation policy which means a bundle
     * remains in the STARTING state until a class is loaded from that bundle
     * (unless that class is declared to not cause the bundle to start).
     *
     * @param bundle The bundle check
     * @return <code>true</code> if <code>bundle</code> is not <code>null</code>
     *          and the bundle is either active or has lazy activation policy
     *          and is in the starting state.
     */
    private boolean isBundleActive( final Bundle bundle ) {
        if ( bundle != null ) {
            if ( bundle.getState() == Bundle.ACTIVE ) {
                return true;
            }

            if ( bundle.getState() == Bundle.STARTING ) {
                // according to the spec the activationPolicy header is only
                // set to request a bundle to be lazily activated. So in this
                // simple check we just verify the header is set to assume
                // the bundle is considered a lazily activated bundle
                return bundle.getHeaders().get( Constants.BUNDLE_ACTIVATIONPOLICY ) != null;
            }
        }

        // fall back: bundle is not considered active
        return false;
    }

    /**
     * Find the bundle for a given package.
     * @param pckName The package name.
     * @return The bundle or <code>null</code>
     */
    private Set<Bundle> findBundlesForPackage(final String pckName) {
        final ExportedPackage[] exportedPackages = this.packageAdmin.getExportedPackages(pckName);
        Set<Bundle> bundles = new LinkedHashSet<>();
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (!exportedPackage.isRemovalPending()) {
                    Bundle bundle = exportedPackage.getExportingBundle();
                    if (isBundleActive(bundle)) {
                        bundles.add(bundle);
                    }
                }
            }
        }
        return bundles;
    }

    /**
     * Return the package from a resource.
     * @param resource The resource path.
     * @return The package name.
     */
    private String getPackageFromResource(final String resource) {
        final int lastSlash = resource.lastIndexOf('/');
        final String pckName = (lastSlash == -1 ? "" : resource.substring(0, lastSlash).replace('/', '.'));
        return pckName;
    }

    /**
     * Return the package from a class.
     * @param name The class name.
     * @return The package name.
     */
    private String getPackageFromClassName(final String name) {
        final int lastDot = name.lastIndexOf('.');
        final String pckName = (lastDot == -1 ? "" : name.substring(0, lastDot));
        return pckName;
    }

    /**
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Enumeration<URL> getResources(final String name) throws IOException {
        Enumeration<URL> e = super.getResources(name);
        if ( e == null || !e.hasMoreElements() ) {
            String packageName = getPackageFromResource(name);
            Bundle providingBundle = packageProviders.get(packageName);
            if (providingBundle == null) {
                for (Bundle bundle : findBundlesForPackage(getPackageFromResource(name))) {
                    e = bundle.getResources(name);
                    if (e != null) {
                        packageProviders.put(packageName, bundle);
                        LOGGER.debug("Marking bundle {}:{} as the provider for API package {}.", bundle.getSymbolicName(), bundle
                                .getVersion().toString(), packageName);
                        return e;
                    }
                }
            } else {
                e = providingBundle.getResources(name);
                if (e == null) {
                    LOGGER.debug("Cannot find resources {} in bundle {}:{} which was marked as the provider for package {}.", name,
                            providingBundle.getSymbolicName(), providingBundle.getVersion().toString(), packageName);
                }
            }
        }
        return e;
    }

    /**
     * @see java.lang.ClassLoader#findResource(java.lang.String)
     */
    public URL findResource(final String name) {
        final URL cachedURL = urlCache.get(name);
        if ( cachedURL != null ) {
            return cachedURL;
        }
        URL url = super.findResource(name);
        if ( url == null ) {
            String packageName = getPackageFromResource(name);
            Bundle providingBundle = packageProviders.get(packageName);
            if (providingBundle == null) {
                Set<Bundle> bundles = findBundlesForPackage(getPackageFromResource(name));
                for (Bundle bundle : bundles) {
                    url = bundle.getResource(name);
                    if (url != null) {
                        urlCache.put(name, url);
                        packageProviders.put(packageName, bundle);
                        LOGGER.debug("Marking bundle {}:{} as the provider for API package {}.", bundle.getSymbolicName(), bundle
                                .getVersion().toString(), packageName);
                        return url;
                    }
                }
            } else {
                url = providingBundle.getResource(name);
                if (url == null) {
                    LOGGER.debug("Cannot find resource {} in bundle {}:{} which was marked as the provider for package {}.", name,
                            providingBundle.getSymbolicName(), providingBundle.getVersion().toString(), packageName);
                }
            }
        }
        return url;
    }

    /**
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        final Class<?> cachedClass = this.classCache.get(name);
        if ( cachedClass != null ) {
            return cachedClass;
        }
        Class<?> clazz;
        try {
            clazz = super.findClass(name);
        } catch (ClassNotFoundException cnfe) {
            try {
                clazz = getClassFromBundles(name);
            } catch (ClassNotFoundException innerCNFE) {
                throw innerCNFE;
            }
        }
        if ( clazz == null ) {
            throw new ClassNotFoundException("Class not found " + name);
        }
        this.classCache.put(name, clazz);
        return clazz;
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        final Class<?> cachedClass = this.classCache.get(name);
        if ( cachedClass != null ) {
            return cachedClass;
        }
        if ( negativeClassCache.contains(name) ) {
            throw new ClassNotFoundException("Class not found " + name);
        }
        String packageName = getPackageFromClassName(name);
        Class<?> clazz;
        try {
            clazz = super.loadClass(name, resolve);
        } catch (final ClassNotFoundException cnfe) {
            try {
                clazz = getClassFromBundles(name);
            } catch (ClassNotFoundException innerCNFE) {
                negativeClassCache.add(name);
                this.factory.addUnresolvedPackage(packageName);
                throw innerCNFE;
            }
        }
        if ( clazz == null ) {
            negativeClassCache.add(name);
            this.factory.addUnresolvedPackage(packageName);
            throw new ClassNotFoundException("Class not found " + name);
        }
        this.classCache.put(name, clazz);
        return clazz;
    }

    private Class<?> getClassFromBundles(String name) throws ClassNotFoundException {
        Class<?> clazz = null;
        String packageName = getPackageFromClassName(name);
        Bundle providingBundle = packageProviders.get(packageName);
        if (providingBundle == null) {
            Set<Bundle> bundles = findBundlesForPackage(packageName);
            for (Bundle bundle : bundles) {
                try {
                    clazz = bundle.loadClass(name);
                    this.factory.addUsedBundle(bundle);
                    packageProviders.put(packageName, bundle);
                    LOGGER.debug("Marking bundle {}:{} as the provider for API package {}.", bundle.getSymbolicName(), bundle
                            .getVersion().toString(), packageName);
                    break;
                } catch (ClassNotFoundException innerCNFE) {
                    // do nothing; we need to loop over the bundles providing the class' package
                }
            }
        } else {
            try {
                clazz = providingBundle.loadClass(name);
            } catch (ClassNotFoundException icnfe) {
                throw new ClassNotFoundException(String.format("Cannot find class %s in bundle %s:%s which was marked as the provider for" +
                        " package %s.", name, providingBundle.getSymbolicName(), providingBundle.getVersion().toString(), packageName), icnfe);
            }
        }
        return clazz;
    }
}
