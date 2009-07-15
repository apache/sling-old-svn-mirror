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
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The <code>PackageAdminClassLoader</code>
 */
class PackageAdminClassLoader extends ClassLoader {

    private final PackageAdmin packageAdmin;

    public PackageAdminClassLoader(final PackageAdmin pckAdmin, final ClassLoader parent) {
        super(parent);
        this.packageAdmin = pckAdmin;
    }

    private Bundle findBundleForClassOrResource(final String name) {
        final int lastDot = name.lastIndexOf('.');
        final String pckName = (lastDot == -1 ? "" : name.substring(0, lastDot));

        final ExportedPackage exportedPackage = this.packageAdmin.getExportedPackage(pckName);
        return (exportedPackage == null ? null : exportedPackage.getExportingBundle());
    }

    private String getPackageFromResource(final String resource) {
        final int lastSlash = resource.lastIndexOf('/');
        String pck = resource.substring(0, lastSlash + 1).replace('/', '.');
        return pck + "Dummy";
    }
    /**
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Enumeration<URL> getResources(String name) throws IOException {
        final Bundle bundle = this.findBundleForClassOrResource(getPackageFromResource(name));
        if ( bundle == null ) {
            return super.getResources(name);
        }
        return bundle.getResources(name);
    }

    /**
     * @see java.lang.ClassLoader#findResource(java.lang.String)
     */
    public URL findResource(String name) {
        final Bundle bundle = this.findBundleForClassOrResource(getPackageFromResource(name));
        if ( bundle == null ) {
            return super.findResource(name);
        }
        return bundle.getResource(name);
    }

    /**
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    public Class<?> findClass(String name) throws ClassNotFoundException {
        final Bundle bundle = this.findBundleForClassOrResource(name);
        if ( bundle == null ) {
            return super.findClass(name);
        }
        return bundle.loadClass(name);
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Bundle bundle = this.findBundleForClassOrResource(name);
        if ( bundle == null ) {
            return super.loadClass(name, resolve);
        }
        return bundle.loadClass(name);
    }
}
