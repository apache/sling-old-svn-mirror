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

import org.osgi.framework.Bundle;

/**
 * The <code>BundleProxyClassLoader</code> is a class loader
 * delegating to a bundle.
 * We don't need to cache as the {@link ClassLoaderFacade} is
 * already doing this.
 */
public class BundleProxyClassLoader extends ClassLoader {

    /** The bundle. */
    private final Bundle bundle;

    public BundleProxyClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    // Note: Both ClassLoader.getResources(...) and bundle.getResources(...) consult
    // the boot classloader. As a result, BundleProxyClassLoader.getResources(...)
    // might return duplicate results from the boot classloader. Prior to Java 5
    // Classloader.getResources was marked final. If your target environment requires
    // at least Java 5 you can prevent the occurence of duplicate boot classloader
    // resources by overriding ClassLoader.getResources(...) instead of
    // ClassLoader.findResources(...).
    @SuppressWarnings("unchecked")
    public Enumeration<URL> findResources(String name) throws IOException {
        return this.bundle.getResources(name);
    }

    public URL findResource(String name) {
        return this.bundle.getResource(name);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        return this.bundle.loadClass(name);
    }

    public URL getResource(String name) {
        return this.findResource(name);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = this.findClass(name);
        if (resolve) {
            super.resolveClass(clazz);
        }
        return clazz;
    }
}
