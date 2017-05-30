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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class BundleResourceProvider extends ResourceProvider<Object> {

    public static final String PROP_BUNDLE = BundleResourceProvider.class.getName();

    /** The bundle providing the resources */
    private final BundleResourceCache bundle;

    /** The root path */
    private final MappedPath root;

    @SuppressWarnings("rawtypes")
    private volatile ServiceRegistration<ResourceProvider> serviceRegistration;

    public static MappedPath[] getRoots(final Bundle bundle, final String rootList) {
        List<MappedPath> prefixList = new ArrayList<>();

        final ManifestHeader header = ManifestHeader.parse(rootList);
        for (final ManifestHeader.Entry entry : header.getEntries()) {
            final String resourceRoot = entry.getValue();
            final String pathDirective = entry.getDirectiveValue("path");
            if (pathDirective != null) {
                prefixList.add(new MappedPath(resourceRoot, pathDirective));
            } else {
                prefixList.add(MappedPath.create(resourceRoot));
            }
        }
       return prefixList.toArray(new MappedPath[prefixList.size()]);
    }

    /**
     * Creates Bundle resource provider accessing entries in the given Bundle an
     * supporting resources below root paths given by the rootList which is a
     * comma (and whitespace) separated list of absolute paths.
     */
    public BundleResourceProvider(final Bundle bundle, final MappedPath root) {
        this.bundle = new BundleResourceCache(bundle);
        this.root = root;
    }

    //---------- Service Registration

    long registerService(final BundleContext context) {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Provider of bundle based resources");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(ResourceProvider.PROPERTY_ROOT, getRoot());
        props.put(PROP_BUNDLE, this.bundle.getBundle().getBundleId());

        serviceRegistration = context.registerService(ResourceProvider.class, this, props);
        return (Long) serviceRegistration.getReference().getProperty(
            Constants.SERVICE_ID);
    }

    void unregisterService() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
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
            return BundleResource.getResource(ctx.getResourceResolver(), bundle,
                mappedPath, path);
        }

        return null;
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
     	if (parent instanceof BundleResource && ((BundleResource)parent).getBundle() == this.bundle) {
            // bundle resources can handle this request directly when the parent
    		//  resource is in the same bundle as this provider.
            return ((BundleResource) parent).listChildren();
    	}

        // ensure this provider may have children of the parent
        String parentPath = parent.getPath();
        MappedPath mappedPath = getMappedPath(parentPath);
        if (mappedPath != null) {
            return new BundleResourceIterator(parent.getResourceResolver(),
                bundle, mappedPath, parentPath);
        }

        // the parent resource cannot have children in this provider,
        // though this is basically not expected, we still have to
        // be prepared for such a situation
        return Collections.<Resource> emptyList().iterator();
    }

    // ---------- Web Console plugin support

    BundleResourceCache getBundleResourceCache() {
        return bundle;
    }

    MappedPath getMappedPath() {
        return root;
    }

    // ---------- internal

    /** Returns the root path */
    private String getRoot() {
        return this.root.getResourceRoot();
    }

    private MappedPath getMappedPath(String resourcePath) {
        if (this.root.isChild(resourcePath)) {
            return root;
        }

        return null;
    }

}
