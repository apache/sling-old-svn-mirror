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
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;


/**
 * The <code>ClassLoaderFacade</code> is a facade
 * for the dynamic class loading.
 */
public class ClassLoaderFacade extends ClassLoader {

    private final DynamicClassLoaderManagerImpl manager;

    public ClassLoaderFacade(final DynamicClassLoaderManagerImpl manager) {
        this.manager = manager;
    }

    /**
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String name) {
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            final URL u = cl.getResource(name);
            if ( u != null ) {
                return u;
            }
        }
        return null;
    }

    /**
     * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String name) {
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            final InputStream i = cl.getResourceAsStream(name);
            if ( i != null ) {
                return i;
            }
        }
        return null;
    }

    /**
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    public Enumeration<URL> getResources(String name) throws IOException {
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            final Enumeration<URL> e = cl.getResources(name);
            if ( e != null && e.hasMoreElements() ) {
                return e;
            }
        }
        return null;
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        final ClassLoader[] loaders = manager.getDynamicClassLoaders();
        for(final ClassLoader cl : loaders) {
            try {
                final Class<?> c = cl.loadClass(name);
                return c;
            } catch (Exception cnfe) {
                // we just ignore this and try the next class loader
            }
        }
        throw new ClassNotFoundException("Class not found: " + name);
    }
}
