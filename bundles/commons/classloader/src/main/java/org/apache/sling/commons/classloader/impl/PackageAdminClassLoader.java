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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The <code>PackageAdminClassLoader</code> loads
 * classes and resources through the package admin service.
 */
class PackageAdminClassLoader extends ClassLoader {

    /** The package admin service. */
    private final PackageAdmin packageAdmin;

    /** The manager factory. */
    private final DynamicClassLoaderManagerFactory factory;

    /** A cache for resolved classes. */
    private Map<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

    /** Negative class cache. */
    private Set<String> negativeClassCache = Collections.synchronizedSet(new HashSet<String>());

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
    private Bundle findBundleForPackage(final String pckName) {
        final ExportedPackage exportedPackage = this.packageAdmin.getExportedPackage(pckName);
        Bundle bundle = null;
        if (exportedPackage != null && !exportedPackage.isRemovalPending() ) {
            bundle = exportedPackage.getExportingBundle();
            if ( !this.isBundleActive(bundle) ) {
                bundle = null;
            }
        }
        return bundle;
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
     * @param resource The class name.
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
            final Bundle bundle = this.findBundleForPackage(getPackageFromResource(name));
            if ( bundle != null ) {
                e = bundle.getResources(name);
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
            final Bundle bundle = this.findBundleForPackage(getPackageFromResource(name));
            if ( bundle != null ) {
                url = bundle.getResource(name);
                if ( url != null ) {
                    urlCache.put(name, url);
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
        Class<?> clazz = null;
        try {
            clazz = super.findClass(name);
        } catch (ClassNotFoundException cnfe) {
            final Bundle bundle = this.findBundleForPackage(getPackageFromClassName(name));
            if ( bundle != null ) {
                clazz = bundle.loadClass(name);
                this.factory.addUsedBundle(bundle);
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
        Class<?> clazz = null;
        try {
            clazz = super.loadClass(name, resolve);
        } catch (final ClassNotFoundException cnfe) {
            final String pckName = getPackageFromClassName(name);
            final Bundle bundle = this.findBundleForPackage(pckName);
            if ( bundle != null ) {
                try {
                    clazz = bundle.loadClass(name);
                    this.factory.addUsedBundle(bundle);
                } catch (final ClassNotFoundException inner) {
                    negativeClassCache.add(name);
                    this.factory.addUnresolvedPackage(pckName);
                    throw inner;
                }
            }
        }
        if ( clazz == null ) {
            negativeClassCache.add(name);
            final String pckName = getPackageFromClassName(name);
            this.factory.addUnresolvedPackage(pckName);
            throw new ClassNotFoundException("Class not found " + name);
        }
        this.classCache.put(name, clazz);
        return clazz;
    }
}
