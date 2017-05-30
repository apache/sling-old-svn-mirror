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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, BundleListener {

    /**
     * The name of the bundle manifest header listing the resource provider root
     * paths provided by the bundle (value is "Sling-Bundle-Resources").
     */
    public static final String BUNDLE_RESOURCE_ROOTS = "Sling-Bundle-Resources";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Long, BundleResourceProvider[]> bundleResourceProviderMap = new HashMap<>();

    private volatile BundleContext bundleContext;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {

        this.bundleContext = context;

        context.addBundleListener(this);

        try {
            final Bundle[] bundles = context.getBundles();
            for (final Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    // add bundle resource provider for active bundles
                    addBundleResourceProvider(bundle);
                }
            }
        } catch (Throwable t) {
            log.error(
                "activate: Problem while registering bundle resources for existing bundles",
                t);
        }

        BundleResourceWebConsolePlugin.initPlugin(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        BundleResourceWebConsolePlugin.destroyPlugin();

        context.removeBundleListener(this);
        this.bundleContext = null;
    }

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                // register resource provider for the started bundle
                addBundleResourceProvider(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove resource provider after the bundle has stopped
                removeBundleResourceProvider(event.getBundle());
                break;
        }
    }

    // ---------- Bundle provided resources -----------------------------------

    private void addBundleResourceProvider(final Bundle bundle) {
        final String prefixes = bundle.getHeaders().get(
            BUNDLE_RESOURCE_ROOTS);
        if (prefixes != null) {
            log.debug(
                "addBundleResourceProvider: Registering resources '{}' for bundle {}/{} as service ",
                new Object[] { prefixes, bundle.getSymbolicName(),
                    bundle.getBundleId() });

            final MappedPath[] roots = BundleResourceProvider.getRoots(bundle, prefixes);
            final BundleResourceProvider[] providers = new BundleResourceProvider[roots.length];

            int index = 0;
            for(final MappedPath path : roots) {
                final BundleResourceProvider brp = new BundleResourceProvider(bundle, path);
                final long id = brp.registerService(bundleContext);
                providers[index] = brp;
                bundleResourceProviderMap.put(bundle.getBundleId(), providers);

                log.debug("addBundleResourceProvider: Service ID = {}", id);
                index++;
            }
        }
    }

    private void removeBundleResourceProvider(final Bundle bundle) {
        final BundleResourceProvider[] brp = bundleResourceProviderMap.remove(bundle.getBundleId());
        if (brp != null) {
            log.debug(
                "removeBundleResourceProvider: Unregistering resources for bundle {}/{}",
                new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
            for(final BundleResourceProvider provider : brp) {
                provider.unregisterService();
            }
        }
    }
}
