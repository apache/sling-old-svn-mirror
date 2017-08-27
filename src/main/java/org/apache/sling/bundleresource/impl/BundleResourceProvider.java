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
package org.apache.sling.bundleresource.impl;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class BundleResourceProvider extends ResourceProvider<Object> {

    public static final String PROP_BUNDLE = BundleResourceProvider.class.getName();

    /** The cache with the bundle providing the resources */
    private final BundleResourceCache cache;

    /** The root path */
    private final MappedPath root;

    @SuppressWarnings("rawtypes")
    private volatile ServiceRegistration<ResourceProvider> serviceRegistration;

    /**
     * Creates Bundle resource provider accessing entries in the given Bundle an
     * supporting resources below root paths given by the rootList which is a
     * comma (and whitespace) separated list of absolute paths.
     */
    public BundleResourceProvider(final Bundle bundle, final MappedPath root) {
        this.cache = new BundleResourceCache(bundle);
        this.root = root;
    }

    //---------- Service Registration

    long registerService() {
        final Bundle bundle = this.cache.getBundle();
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Provider of bundle based resources from bundle " + String.valueOf(bundle.getBundleId()));
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(ResourceProvider.PROPERTY_ROOT, this.root.getResourceRoot());
        props.put(PROP_BUNDLE,bundle.getBundleId());

        serviceRegistration = bundle.getBundleContext().registerService(ResourceProvider.class, this, props);
        return (Long) serviceRegistration.getReference().getProperty(Constants.SERVICE_ID);
    }

    void unregisterService() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    // ---------- ResourceProvider interface

    /**
     * Returns a BundleResource for the path if such an entry exists in the
     * bundle of this provider.
     */
    @Override
    public Resource getResource(final ResolveContext<Object> ctx,
            final String path,
            final ResourceContext resourceContext,
            final Resource parent) {
        final MappedPath mappedPath = getMappedPath(path);
        if (mappedPath != null) {
            final String entryPath = mappedPath.getEntryPath(path);

            // first try, whether the bundle has an entry with a trailing slash
            // which would be a folder. In this case we check whether the
            // repository contains an item with the same path. If so, we
            // don't create a BundleResource but instead return null to be
            // able to return an item-based resource
            URL entry = cache.getEntry(entryPath.concat("/"));
            final boolean isFolder = entry != null;

            // if there is no entry with a trailing slash, try plain name
            // which would then of course be a file
            if (entry == null) {
                entry = cache.getEntry(entryPath);
                if ( entry == null && this.root.getJSONPropertiesExtension() != null ) {
                    entry = cache.getEntry(entryPath + this.root.getJSONPropertiesExtension());
                }
            }

            // here we either have a folder for which no same-named item exists
            // or a bundle file
            if (entry != null) {
                // check if a JSON props file is directly requested
                // if so, we deny the access
                if ( this.root.getJSONPropertiesExtension() == null
                     || !entryPath.endsWith(this.root.getJSONPropertiesExtension()) ) {

                    String propsPath = null;
                    if ( this.root.getJSONPropertiesExtension() != null ) {
                        propsPath = entryPath.concat(this.root.getJSONPropertiesExtension());
                    }
                    return new BundleResource(ctx.getResourceResolver(),
                            cache,
                            mappedPath,
                            path,
                            propsPath,
                            null,
                            isFolder);
                }
            }

            // the bundle does not contain the path
            // if JSON is enabled check for any parent
            if ( this.root.getJSONPropertiesExtension() != null ) {
                String resourcePath = ResourceUtil.getParent(path);
                while ( resourcePath != null ) {
                    final Resource rsrc = getResource(ctx, resourcePath, resourceContext, null);
                    if (rsrc != null ) {
                        final Resource childResource = ((BundleResource)rsrc).getChildResource(path.substring(resourcePath.length() + 1));
                        if ( childResource != null ) {
                            return childResource;
                        }
                    }
                    resourcePath = ResourceUtil.getParent(resourcePath);
                }
            }
        }

        return null;
    }

    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Object> ctx, final Resource parent) {
     	if (parent instanceof BundleResource && ((BundleResource)parent).getBundle() == this.cache) {
            // bundle resources can handle this request directly when the parent
    		    // resource is in the same bundle as this provider.
            return ((BundleResource) parent).listChildren();
      	}

        // ensure this provider may have children of the parent
        String parentPath = parent.getPath();
        MappedPath mappedPath = getMappedPath(parentPath);
        if (mappedPath != null) {
            return new BundleResourceIterator(parent.getResourceResolver(),
                cache, mappedPath, parentPath);
        }

        // the parent resource cannot have children in this provider,
        // though this is basically not expected, we still have to
        // be prepared for such a situation
        return null;
    }

    // ---------- Web Console plugin support

    BundleResourceCache getBundleResourceCache() {
        return cache;
    }

    MappedPath getMappedPath() {
        return root;
    }

    // ---------- internal

    private MappedPath getMappedPath(final String resourcePath) {
        if (this.root.isChild(resourcePath)) {
            return root;
        }

        return null;
    }
}
