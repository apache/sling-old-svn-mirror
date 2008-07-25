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
import java.util.Dictionary;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
        File file;
        if (path.equals(providerRoot)) {
            file = providerFile;
        } else {
            String relPath = path.substring(providerRoot.length() + 1);
            file = new File(providerFile, relPath);
        }

        if (file.exists()) {
            return new FsResource(resourceResolver, path, file);
        }

        // not applicable or not an existing file path
        return null;
    }

    /**
     * Returns an iterator of resources.
     */
    public Iterator<Resource> listChildren(Resource parent) {
        File parentFile = parent.adaptTo(File.class);
        if (parentFile == null) {
            // not a FsResource, try to create one from the resource
            parent = getResource(parent.getResourceResolver(), parent.getPath());
            if (parent != null) {
                parentFile = parent.adaptTo(File.class);
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
        this.providerFile = getProviderFile(providerFileName,
            context.getBundleContext());
    }

    protected void deactivate(ComponentContext context) {
        this.providerRoot = null;
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
}
