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
package org.apache.sling.fsprovider.internal;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The <code>FsResourceProvider</code> is a resource provider which maps
 * file system files and folders into the virtual resource tree. The provider is
 * implemented in terms of a component factory, that is multiple instances of
 * this provider may be created by creating respective configuration.
 * <p>
 * Each provider instance is configured with two properties: The location in the
 * resource tree where resources are provided ({@link ResourceProvider#ROOTS})
 * and the file system path from where files and folders are mapped into the
 * resource ({@link #PROP_PROVIDER_FILE}).
 */
@Component(name="org.apache.sling.fsprovider.internal.FsResourceProvider",
           service=ResourceProvider.class,
           configurationPolicy=ConfigurationPolicy.REQUIRE,
           property={
                   Constants.SERVICE_DESCRIPTION + "=Sling Filesystem Resource Provider",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
@Designate(ocd=FsResourceProvider.Config.class, factory=true)
public class FsResourceProvider extends ResourceProvider<Object> {

    @ObjectClassDefinition(name = "Apache Sling Filesystem Resource Provider",
            description = "Configure an instance of the filesystem " +
                          "resource provider in terms of provider root and filesystem location")
    public @interface Config {
        /**
         * The name of the configuration property providing file system path of
         * files and folders mapped into the resource tree (value is
         * "provider.file").
         */
        @AttributeDefinition(name = "Filesystem Root",
                description = "Filesystem directory mapped to the virtual " +
                        "resource tree. This property must not be an empty string. If the path is " +
                        "relative it is resolved against sling.home or the current working directory. " +
                        "The path may be a file or folder. If the path does not address an existing " +
                        "file or folder, an empty folder is created.")
        String provider_file();

        /**
         * The name of the configuration property providing the check interval
         * for file changes (value is "provider.checkinterval").
         */
        @AttributeDefinition(name = "Check Interval",
                             description = "If the interval has a value higher than 100, the provider will " +
             "check the file system for changes periodically. This interval defines the period in milliseconds " +
             "(the default is 1000). If a change is detected, resource events are sent through the event admin.")
        long provider_checkinterval() default 1000;

        @AttributeDefinition(name = "Provider Root",
                description = "Location in the virtual resource tree where the " +
                "filesystem resources are mapped in. This property must not be an empty string.")
        String provider_root();
        
        /**
         * Internal Name hint for web console.
         */
        String webconsole_configurationFactory_nameHint() default "Root path: {" + ResourceProvider.PROPERTY_ROOT + "}";
    }

    // The location in the resource tree where the resources are mapped
    private String providerRoot;

    // providerRoot + "/" to be used for prefix matching of paths
    private String providerRootPrefix;

    // The "root" file or folder in the file system
    private File providerFile;

    /** The monitor to detect file changes. */
    private FileMonitor monitor;

    /**
     * Returns a resource wrapping a file system file or folder for the given
     * path. If the <code>path</code> is equal to the configured resource tree
     * location of this provider, the configured file system file or folder is
     * used for the resource. Otherwise the configured resource tree location
     * prefix is removed from the path and the remaining relative path is used
     * to access the file or folder. If no such file or folder exists, this
     * method returns <code>null</code>.
     */
    @Override
    public Resource getResource(final ResolveContext<Object> ctx,
            final String path,
            final ResourceContext resourceContext,
            final Resource parent) {
        return getResource(ctx.getResourceResolver(), path, getFile(path));
    }

    /**
     * Returns an iterator of resources.
     */
    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Object> ctx, final Resource parent) {
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
                                parent.getResourceResolver(), providerRoot,
                                providerFile);
                        if (res != null) {
                            return Collections.singletonList(res).iterator();
                        }
                    }
                }

                // no children here
                return null;
            }
        }

        final File[] children = parentFile.listFiles();

        if (children != null && children.length > 0) {
            final ResourceResolver resolver = parent.getResourceResolver();
            final String parentPath = parent.getPath();
            return new Iterator<Resource>() {
                int index = 0;

                Resource next = seek();

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Resource next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    Resource result = next;
                    next = seek();
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                private Resource seek() {
                    while (index < children.length) {
                        File file = children[index++];
                        String path = parentPath + "/" + file.getName();
                        Resource result = getResource(resolver, path, file);
                        if (result != null) {
                            return result;
                        }
                    }

                    // nothing found any more
                    return null;
                }
            };
        }

        // no children
        return null;
    }

    // ---------- SCR Integration
    @Activate
    protected void activate(BundleContext bundleContext, final Config config) {
        String providerRoot = config.provider_root();
        if (providerRoot == null || providerRoot.length() == 0) {
            throw new IllegalArgumentException("provider.root property must be set");
        }

        String providerFileName = config.provider_file();
        if (providerFileName == null || providerFileName.length() == 0) {
            throw new IllegalArgumentException("provider.file property must be set");
        }

        this.providerRoot = providerRoot;
        this.providerRootPrefix = providerRoot.concat("/");
        this.providerFile = getProviderFile(providerFileName, bundleContext);
        // start background monitor if check interval is higher than 100
        if ( config.provider_checkinterval() > 100 ) {
            this.monitor = new FileMonitor(this, config.provider_checkinterval());
        }
    }

    @Deactivate
    protected void deactivate() {
        if ( this.monitor != null ) {
            this.monitor.stop();
            this.monitor = null;
        }
        this.providerRoot = null;
        this.providerRootPrefix = null;
        this.providerFile = null;
    }

    File getRootFile() {
        return this.providerFile;
    }

    String getProviderRoot() {
        return this.providerRoot;
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

    private Resource getResource(ResourceResolver resourceResolver,
            String resourcePath, File file) {

        if (file != null) {

            // if the file exists, but is not a directory or no repository entry
            // exists, return it as a resource
            if (file.exists()) {
                return new FsResource(resourceResolver, resourcePath, file);
            }

        }

        // not applicable or not an existing file path
        return null;
    }

    public ObservationReporter getObservationReporter() {
        final ProviderContext ctx = this.getProviderContext();
        if ( ctx != null ) {
            return ctx.getObservationReporter();
        }
        return null;
    }
}
