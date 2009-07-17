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
package org.apache.sling.commons.fsclassloader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.DynamicClassLoaderProvider;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>FSClassLoaderProvider</code> is a dynamic class loader privder
 * which uses the file system to store and read class files from.
 *
 * @scr.component metatype="false"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Provides FileSystem ClassLoaders"
 */
public class FSClassLoaderProvider
    implements DynamicClassLoaderProvider, ClassLoaderWriter {

    /** File root */
    private File root;

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoaderProvider#getClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader(final ClassLoader parent) {
        try {
            return new URLClassLoader(new URL[] {this.root.toURL()}, parent);
        } catch (MalformedURLException e) {
            // this should never happen, but who knows
            throw new RuntimeException(e);
        }
    }

    //---------- SCR Integration ----------------------------------------------

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(String name) {
        final String path = cleanPath(name);
        final File file = new File(path);
        if ( file.exists() ) {
            return file.delete();
        }
        // file does not exist so we return false
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(String name) {
        final String path = cleanPath(name);
        final File file = new File(path);
        final File parentDir = file.getParentFile();
        if ( !parentDir.exists() ) {
            parentDir.mkdirs();
        }
        try {
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldName, String newName) {
        final String oldPath = cleanPath(oldName);
        final String newPath = cleanPath(newName);
        final File old = new File(oldPath);
        return old.renameTo(new File(newPath));
    }

    /**
     * Clean the path by converting slashes to the correct format
     * and prefixing the root directory.
     * @param path The path
     * @return The file path
     */
    private String cleanPath(String path) {
        // replace backslash by slash
        path = path.replace('\\', '/');

        // cut off trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if ( File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }
        return this.root.getAbsolutePath() + path;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String name)
    throws IOException {
        final String path = cleanPath(name);
        final File file = new File(path);
        return new FileInputStream(file);
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(String name) {
        final String path = cleanPath(name);
        final File file = new File(path);
        if ( file.exists() ) {
            return file.lastModified();
        }

        // fallback to "non-existant" in case of problems
        return -1;
    }

    /**
     * Activate this component.
     * Create the root directory.
     * @param componentContext
     */
    protected void activate(ComponentContext componentContext) {
        // get the file root
        this.root = new File(componentContext.getBundleContext().getDataFile(""), "classes");
        this.root.mkdirs();
    }

    /**
     * Deactivate this component.
     * Create the root directory.
     * @param componentContext
     */
    protected void deactivate(ComponentContext componentContext) {
        this.root = null;
    }
}
