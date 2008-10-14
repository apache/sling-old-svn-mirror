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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.INSTALLED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.UPDATED;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OsgiResourceProcessor for Bundles */
public class BundleResourceProcessor implements OsgiResourceProcessor {

    public static final String BUNDLE_EXTENSION = ".jar";

    /** {@link Storage} key for the bundle ID */
    public static final String KEY_BUNDLE_ID = "bundle.id";

    private final BundleContext ctx;
    private final PackageAdmin packageAdmin;
    private final Map<Long, Bundle> pendingBundles;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Object refreshLock = new Object();

    BundleResourceProcessor(BundleContext ctx, PackageAdmin packageAdmin) {
        this.ctx = ctx;
        this.packageAdmin = packageAdmin;
        pendingBundles = new HashMap<Long, Bundle>();
    }

    public int installOrUpdate(String uri, Map<String, Object> attributes, InputStream data) throws Exception {
        // Update if we already have a bundle id, else install
        Bundle b = null;
        boolean updated = false;

        // check whether we know the bundle and it exists
        final Long longId = (Long)attributes.get(KEY_BUNDLE_ID);
        if(longId != null) {
            b = ctx.getBundle(longId);
        }

        // either we don't know the bundle yet or it does not exist,
        // so check whether the bundle can be found by its symbolic name
        if (b == null) {
            // ensure we can mark and reset to read the manifest
            if (!data.markSupported()) {
                data = new BufferedInputStream(data);
            }
            b = getMatchingBundle(data);
        }

        if (b != null) {
            b.update(data);
            updated = true;
        } else {
            uri = OsgiControllerImpl.getResourceLocation(uri);
            log.debug("No matching Bundle for uri {}, installing", uri);
            b = ctx.installBundle(uri, data);
        }

        // ensure the bundle id in the attributes, this may be overkill
        // in simple update situations, but is required for installations
        // and updates where there are no attributes yet
        attributes.put(KEY_BUNDLE_ID, b.getBundleId());

        synchronized(pendingBundles) {
            pendingBundles.put(b.getBundleId(), b);
        }

        return updated ? UPDATED : INSTALLED;
    }

    public void uninstall(String uri, Map<String, Object> attributes) throws BundleException {
        final Long longId = (Long)attributes.get(KEY_BUNDLE_ID);
        if(longId == null) {
            log.debug("No {} in metadata, bundle cannot be uninstalled");
        } else {
            final Bundle b = ctx.getBundle(longId);
            if(b == null) {
                log.debug("Bundle having id {} not found, cannot uninstall");
            } else {
                synchronized(pendingBundles) {
                    pendingBundles.remove(b.getBundleId());
                }
                b.uninstall();
            }
        }
    }

    public boolean canProcess(String uri) {
        return uri.endsWith(BUNDLE_EXTENSION);
    }

    public void processResourceQueue() {

        if(pendingBundles.isEmpty()) {
            return;
        }

        final List<Long> toRemove = new LinkedList<Long>();
        final List<Long> idList = new LinkedList<Long>();
        synchronized(pendingBundles) {
            idList.addAll(pendingBundles.keySet());
        }

        for(Long id : idList) {
            final Bundle bundle = ctx.getBundle(id);
            if(bundle == null) {
                log.debug("Bundle id {} disappeared (bundle removed from framework?), removed from pending bundles queue");
                toRemove.add(id);
                continue;
            }
            final int state = bundle.getState();

            switch ( state ) {
                case Bundle.ACTIVE :
                    log.info("Bundle {} is active, removed from pending bundles queue", bundle.getLocation());
                    toRemove.add(id);
                    break;
                case Bundle.STARTING :
                    log.info("Bundle {} is starting.", bundle.getLocation());
                    break;
                case Bundle.STOPPING :
                    log.info("Bundle {} is stopping.", bundle.getLocation());
                    break;
                case Bundle.UNINSTALLED :
                    log.info("Bundle {} is uninstalled, removed from pending bundles queue", bundle.getLocation());
                    toRemove.add(id);
                    break;
                case Bundle.INSTALLED :
                    log.debug("Bundle {} is installed but not resolved.", bundle.getLocation());
                    if ( !packageAdmin.resolveBundles(new Bundle[] {bundle}) ) {
                        log.debug("Bundle {} is installed, failed to resolve.", bundle.getLocation());
                        break;
                    }
                    // fall through to RESOLVED to start the bundle
                case Bundle.RESOLVED :
                    log.info("Bundle {} is resolved, trying to start it.", bundle.getLocation());
                    try {
                        bundle.start();
                    } catch (BundleException e) {
                        log.error("Exception during bundle start of Bundle " + bundle.getLocation(), e);
                    }
                    break;
            }
        }

        synchronized(refreshLock) {
            packageAdmin.refreshPackages(null);
        }

        synchronized(pendingBundles) {
            pendingBundles.keySet().removeAll(toRemove);
        }
    }

    /**
     * Returns a bundle with the same symbolic name as the bundle provided in
     * the input stream. If the input stream has no manifest file or the
     * manifest file does not have a <code>Bundle-SymbolicName</code> header,
     * this method returns <code>null</code>. <code>null</code> is also
     * returned if no bundle with the same symbolic name as provided by the
     * input stream is currently installed.
     * <p>
     * This method reads from the input stream and uses the
     * <code>InputStream.mark</code> and <code>InputStream.reset</code>
     * methods to reset the stream to where it started reading. The caller must
     * make sure, the input stream supports the marking as reported by
     * <code>InputStream.markSupported</code>.
     *
     * @param data The mark supporting <code>InputStream</code> providing the
     *            bundle whose symbolic name is to be matched against installed
     *            bundles.
     * @return The installed bundle with the same symbolic name as the bundle
     *         provided by the input stream or <code>null</code> if no such
     *         bundle exists or if the input stream does not provide a manifest
     *         with a symbolic name.
     * @throws IOException If an error occurrs reading from the input stream.
     */
    private Bundle getMatchingBundle(InputStream data) throws IOException {
        // allow 2KB, this should be enough for the manifest
        data.mark(2048);

        JarInputStream jis = null;
        try {
            // we cose the JarInputStream at the end, so wrap the actual
            // input stream to not propagate this to the actual input
            // stream, because we still need it
            InputStream nonClosing = new FilterInputStream(data) {
                @Override
                public void close() {
                    // don't really close
                }
            };

            jis = new JarInputStream(nonClosing);
            Manifest manifest = jis.getManifest();
            if (manifest != null) {

                String symbolicName = manifest.getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
                if (symbolicName != null) {

                    Bundle[] bundles = ctx.getBundles();
                    for (Bundle bundle : bundles) {
                        if (symbolicName.equals(bundle.getSymbolicName())) {
                            return bundle;
                        }
                    }

                }
            }

        } finally {

            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException ignore) {
                }
            }

            // reset the input to where we started
            data.reset();
        }

        // fall back to no bundle found for update
        return null;
    }
}