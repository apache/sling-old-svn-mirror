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
package org.apache.sling.fsprovider;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>FsResourceProvider</code> is a resource provider which maps
 * filesystem files and folders into the virtual resource tree. The provider is
 * implemented in terms of a component factory, that is multiple instances of
 * this provider may be created by creating respective configuration.
 * <p>
 * Each provider instance is configured with to properties: The location in the
 * resource tree where resources are provided ({@link ResourceProvider#ROOTS})
 * and the file system path from where files and folders are mapped into the
 * resource ({@link #PROP_PROVIDER_FILE}).
 * 
 * @scr.component label="%resource.resolver.name"
 *                description="%resource.resolver.description"
 *                factory="org.apache.sling.fsprovider.FsResourceProviderFactory"
 * @scr.service
 * @scr.property name="service.description" value="Sling Filesystem Resource
 *               Provider"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property nameRef="ResourceProvider.ROOTS"
 * @scr.property nameRef="PROP_PROVIDER_FILE"
 */
public class FsResourceProvider implements ResourceProvider {

    /**
     * The name of the configuration property providing file system path of
     * files and folders mapped into the resource tree (value is
     * "provider.file").
     */
    public static final String PROP_PROVIDER_FILE = "provider.file";

    // The location in the resource tree where the resources are mapped
    private String providerRoot;

    // providerRoot + "/" to be used for prefix matching of paths
    private String providerRootPrefix;

    // The "root" file or folder in the file system
    private File providerFile;

    /**
     * Same as {@link #getResource(ResourceResolver, String)}, i.e. the
     * <code>request</code> parameter is ignored.
     * 
     * @see #getResource(ResourceResolver, String)
     */
    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    /**
     * Returns a resource wrapping a filesystem file or folder for the given
     * path. If the <code>path</code> is equal to the configured resource tree
     * location of this provider, the configured file system file or folder is
     * used for the resource. Otherwise the configured resource tree location
     * prefix is removed from the path and the remaining relative path is used
     * to access the file or folder. If no such file or folder exists, this
     * method returns <code>null</code>.
     */
    public Resource getResource(ResourceResolver resourceResolver, String path) {

        // convert the path to a file
        File file = getFile(path);
        if (file != null) {

            // if the file is a directory, and a repository item exists for
            // the path, do not return the directory here
            if (file.isDirectory()) {
                Session session = resourceResolver.adaptTo(Session.class);
                if (session != null) {
                    try {
                        if (session.itemExists(path)) {
                            return null;
                        }
                    } catch (RepositoryException re) {
                        // don't care
                    }
                }
            }

            // if the file exists, but is not a directory or no repository entry
            // exists, return it as a resource
            if (file.exists()) {
                return new FsResource(resourceResolver, path, file);
            }

        }

        // not applicable or not an existing file path
        return null;
    }

    /**
     * Returns an iterator of resources.
     */
    public Iterator<Resource> listChildren(Resource parent) {
        File parentFile = parent.adaptTo(File.class);

        // not a FsResource, try to create one from the resource
        if (parentFile == null) {
            // if the parent path is at or below the provider root, get
            // the respective file
            parentFile = getFile(parent.getPath());
            
            // if the parent path is actually the parent of the provider
            // root, return a single element iterator just containing the
            // provider file, unless the provider file is a directory and
            // a repository item with the same path actually exists
            if (parentFile == null) {
                
                String parentPath = parent.getPath().concat("/");
                if (providerRoot.startsWith(parentPath)) {
                    String relPath = providerRoot.substring(parentPath.length());
                    if (relPath.indexOf('/') < 0) {
                        Resource res = getResource(
                            parent.getResourceResolver(), providerRoot);
                        if (res != null) {
                            return Collections.singletonList(res).iterator();
                        }
                    }
                }

                // no children here
                return null;
            }
        }

        if (parentFile != null) {

            final File[] children = parentFile.listFiles();

            if (children != null && children.length > 0) {
                final ResourceResolver resolver = parent.getResourceResolver();
                final String parentPath = parent.getPath();
                return new Iterator<Resource>() {
                    int index = 0;

                    public boolean hasNext() {
                        return index < children.length;
                    }

                    public Resource next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        File file = children[index];
                        index++;

                        return new FsResource(resolver, parentPath + "/"
                            + file.getName(), file);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }
        }

        // no children
        return null;
    }

    // ---------- SCR Integration

    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        String providerRoot = (String) props.get(ROOTS);
        if (providerRoot == null || providerRoot.length() == 0) {
            throw new IllegalArgumentException(ROOTS + " property must be set");
        }

        String providerFileName = (String) props.get(PROP_PROVIDER_FILE);
        if (providerFileName == null || providerFileName.length() == 0) {
            throw new IllegalArgumentException(PROP_PROVIDER_FILE
                + " property must be set");
        }

        this.providerRoot = providerRoot;
        this.providerRootPrefix = providerRoot.concat("/");
        this.providerFile = getProviderFile(providerFileName,
            context.getBundleContext());
    }

    protected void deactivate(ComponentContext context) {
        this.providerRoot = null;
        this.providerRootPrefix = null;
        this.providerFile = null;
    }

    // ---------- internal

    private File getProviderFile(String providerFileName,
            BundleContext bundleContext) {

        // the file object from the plain name
        File providerFile = new File(providerFileName);

        // resolve relative file name against sling.home or current
        // working directory
        if (!providerFile.isAbsolute()) {
            String home = bundleContext.getProperty("sling.home");
            if (home != null && home.length() > 0) {
                providerFile = new File(home, providerFileName);
            }
        }

        // resolve the path
        providerFile = providerFile.getAbsoluteFile();

        // if the provider file does not exist, create an empty new folder
        if (!providerFile.exists() && !providerFile.mkdirs()) {
            throw new IllegalArgumentException(
                "Cannot create provider file root " + providerFile);
        }

        return providerFile;
    }

    /**
     * Returns a file corresponding to the given absolute resource tree path. If
     * the path equals the configured provider root, the provider root file is
     * returned. If the path starts with the configured provider root, a file is
     * returned relative to the provider root file whose relative path is the
     * remains of the resource tree path without the provider root path.
     * Otherwise <code>null</code> is returned.
     */
    private File getFile(String path) {
        if (path.equals(providerRoot)) {
            return providerFile;
        }

        if (path.startsWith(providerRootPrefix)) {
            String relPath = path.substring(providerRootPrefix.length());
            return new File(providerFile, relPath);
        }

        return null;
    }
}
