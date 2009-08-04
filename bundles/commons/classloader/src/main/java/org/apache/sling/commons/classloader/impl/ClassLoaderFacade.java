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
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The <code>ClassLoaderFacade</code> is a facade
 * for the dynamic class loading.
 * This class loader is returned to the clients of the
 * dynamic class loader manager.
 * This class loader delegates to other class loaders
 * but caches its result for performance.
 */
public class ClassLoaderFacade extends ClassLoader {

    private final DynamicClassLoaderManagerImpl manager;

    /** A cache for resolved classes. */
    private Map<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

    /** A cache for resolved urls. */
    private Map<String, URL> urlCache = new ConcurrentHashMap<String, URL>();

    public ClassLoaderFacade(final DynamicClassLoaderManagerImpl manager) {
        this.manager = manager;
    }

    /**
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String name) {
        if ( !this.manager.isActive() ) {
            throw new RuntimeException("Dynamic class loader has already been deactivated.");
        }
        final URL cachedURL = urlCache.get(name);
        if ( cachedURL != null ) {
            return cachedURL;
        }
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            if ( cl != null ) {
                final URL u = cl.getResource(name);
                if ( u != null ) {
                    urlCache.put(name, u);
                    return u;
                }
            }
        }
        return null;
    }

    /**
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    public Enumeration<URL> getResources(String name) throws IOException {
        if ( !this.manager.isActive() ) {
            throw new RuntimeException("Dynamic class loader has already been deactivated.");
        }
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            if ( cl != null ) {
                final Enumeration<URL> e = cl.getResources(name);
                if ( e != null && e.hasMoreElements() ) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if ( !this.manager.isActive() ) {
            throw new RuntimeException("Dynamic class loader has already been deactivated.");
        }
        final Class<?> cachedClass = this.classCache.get(name);
        if ( cachedClass != null ) {
            return cachedClass;
        }
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            if ( cl != null ) {
                try {
                    final Class<?> c = cl.loadClass(name);
                    this.classCache.put(name, c);
                    return c;
                } catch (Exception cnfe) {
                    cnfe.printStackTrace();
                    // we just ignore this and try the next class loader
                }
            }
        }
        throw new ClassNotFoundException("Class not found: " + name);
    }
}
