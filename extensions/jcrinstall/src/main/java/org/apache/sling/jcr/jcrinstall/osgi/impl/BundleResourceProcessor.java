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

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.IGNORED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.INSTALLED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.UPDATED;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OsgiResourceProcessor for Bundles */
public class BundleResourceProcessor implements OsgiResourceProcessor,
        FrameworkListener {

    public static final String BUNDLE_EXTENSION = ".jar";

    /** {@link Storage} key for the bundle ID */
    public static final String KEY_BUNDLE_ID = "bundle.id";

    private final BundleContext ctx;
    private final PackageAdmin packageAdmin;
    private final StartLevel startLevel;

    /**
     * All bundles which were active before {@link #processResourceQueue()}
     * refreshes the packages. Bundles which may not be started after refreshing
     * the packages remain in this set. In addition bundles from the
     * {@link #installedBundles} bundles list will be added here to try to start
     * them in the next round.
     */
    private final Set<Long> activeBundles;
    
    /**
     * The list of bundles which have been updated or installed and which need
     * to be started in the next round. Bundles from this list, which fail
     * to start, are added to the {@link #activeBundles} set for them to be
     * started in the next round.
     */
    private final List<Long> installedBundles;
    
    /**
     * Flag set by {@link #installOrUpdate(String, Map, InputStream)} of a
     * bundle has been updated and by {@link #uninstall(String, Map)} if a
     * bundle has been removed. This causes the {@link #processResourceQueue()}
     * method to refresh the packages.
     * @see #processResourceQueue()
     */
    private boolean needsRefresh;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    BundleResourceProcessor(BundleContext ctx, PackageAdmin packageAdmin, StartLevel startLevel) {
        this.ctx = ctx;
        this.packageAdmin = packageAdmin;
        this.startLevel = startLevel;
        this.activeBundles = new HashSet<Long>();
        this.installedBundles = new ArrayList<Long>();

        // register as a framework listener
        ctx.addFrameworkListener(this);
    }

    public void dispose() {
        // unregister as a framework listener
        ctx.removeFrameworkListener(this);
    }

    // ---------- FrameworkListener

    /**
     * Handles the PACKAGES_REFRESHED framework event which is sent after
     * the PackageAdmin.refreshPackages has finished its work of refreshing
     * the packages. When packages have been refreshed all bundles which are
     * expected to be active (those active before refreshing the packages and
     * newly installed or updated bundles) are started. 
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
        	log.debug("FrameworkEvent.PACKAGES_REFRESHED");
            startBundles();
        }
    }

    // ---------- OsgiResourceProcessor

    /**
     * @throws BundleException
     * @see org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor#installOrUpdate(java.lang.String,
     *      java.util.Map, java.io.InputStream)
     */
    public int installOrUpdate(String uri, Map<String, Object> attributes,
            InstallableData installableData) throws BundleException, IOException {
    	
    	InputStream data = installableData.adaptTo(InputStream.class);
    	if(data == null) {
    		throw new IOException("InstallableData does not adapt to an InputStream: " + uri);
    	}
    	
        // Update if we already have a bundle id, else install
		Bundle b;
		boolean updated;
		try {
			b = null;
			updated = false;

			// check whether we know the bundle and it exists
			final Long longId = (Long) attributes.get(KEY_BUNDLE_ID);
			if (longId != null) {
			    b = ctx.getBundle(longId);
			}

			// either we don't know the bundle yet or it does not exist,
			// so check whether the bundle can be found by its symbolic name
			if (b == null) {
			    final BundleInfo info = getMatchingBundle(installableData);
			    if (info != null) {
			        final Version availableVersion = new Version(
			            (String) info.bundle.getHeaders().get(
			                Constants.BUNDLE_VERSION));
			        final Version newVersion = new Version(info.newVersion);
			        if (newVersion.compareTo(availableVersion) > 0) {
			            b = info.bundle;
			        } else {
			            log.debug(
			                "Ignore update of bundle {} from {} as the installed version is equal or higher.",
			                info.bundle.getSymbolicName(), uri);
			            return IGNORED;
			        }
			    }
			}

			if (b != null) {
			    b.stop();
			    b.update(data);
			    updated = true;
			    needsRefresh = true;
			} else {
			    uri = OsgiControllerImpl.getResourceLocation(uri);
			    final int level = installableData.getBundleStartLevel();
			    log.debug("No matching Bundle for uri {}, installing with start level {}", uri, level);
			    b = ctx.installBundle(uri, data);
			    startLevel.setBundleStartLevel(b, level);
			}
		} finally {
		    // data is never null here
		    try {
				data.close();
			} catch (IOException ioe) {
			}
		}

        // ensure the bundle id in the attributes, this may be overkill
        // in simple update situations, but is required for installations
        // and updates where there are no attributes yet
        attributes.put(KEY_BUNDLE_ID, b.getBundleId());

        synchronized (activeBundles) {
            installedBundles.add(b.getBundleId());
        }

        return updated ? UPDATED : INSTALLED;
    }

    /**
     * @see org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor#uninstall(java.lang.String,
     *      java.util.Map)
     */
    public void uninstall(String uri, Map<String, Object> attributes)
            throws BundleException {
        final Long longId = (Long) attributes.get(KEY_BUNDLE_ID);
        if (longId == null) {
            log.debug(
                "Bundle {} cannot be uninstalled, bundle id not found, ignored",
                uri);
        } else {
            final Bundle b = ctx.getBundle(longId);
            if (b == null) {
                log.debug("Bundle having id {} not found, cannot uninstall",
                    longId);
            } else {
                synchronized (installedBundles) {
                    installedBundles.remove(b.getBundleId());
                }
                b.uninstall();
                needsRefresh = true;
            }
        }
    }

    /**
     * @see org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor#canProcess(java.lang.String)
     */
    public boolean canProcess(String uri, InstallableData data) {
        return uri.endsWith(BUNDLE_EXTENSION);
    }

    /**
     * Refreshes packages with subsequence bundle start or directly starts
     * installed bundles. If since the last call to this method a bundle has
     * been updated or removed, the packages will be refreshed. If no update
     * or uninstallation has taken place, we still try to start all bundles
     * which we expect to be started (mostly bundles which have recently been
     * installed) but without refreshing the packages first.
     */
    public void processResourceQueue() {
        if (needsRefresh) {
        	
            // reset the flag
            needsRefresh = false;
            
            // gather bundles currently active
            for (Bundle bundle : ctx.getBundles()) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    synchronized (activeBundles) {
                        activeBundles.add(bundle.getBundleId());
                    }
                }
            }

        	log.debug("Processing resource queue in REFRESH mode, {} active bundles found, refreshing packages", 
        			activeBundles.size());
        	
            // refresh now
            packageAdmin.refreshPackages(null);
            
        } else {
            startBundles();
        }
    }

    /**
     * Returns a bundle with the same symbolic name as the bundle provided in
     * the installable data. If the installable data has no manifest file or the
     * manifest file does not have a <code>Bundle-SymbolicName</code> header,
     * this method returns <code>null</code>. <code>null</code> is also
     * returned if no bundle with the same symbolic name as provided by the
     * input stream is currently installed.
     * <p>
     * This method gets its own input stream from the installable data object
     * and closes it after haing read the manifest file.
     * 
     * @param installableData The installable data providing the bundle jar file
     *            from the input stream.
     * @return The installed bundle with the same symbolic name as the bundle
     *         provided by the input stream or <code>null</code> if no such
     *         bundle exists or if the input stream does not provide a manifest
     *         with a symbolic name.
     * @throws IOException If an error occurrs reading from the input stream.
     */
    private BundleInfo getMatchingBundle(InstallableData installableData) throws IOException {

        // open the stream to read the bundle manifest from
        InputStream ins = installableData.adaptTo(InputStream.class);
        if (ins == null) {
        	return null;
       	}
       	
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(ins);
            Manifest manifest = jis.getManifest();
            if (manifest != null) {

                String symbolicName = manifest.getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
                if (symbolicName != null) {

                    Bundle[] bundles = ctx.getBundles();
                    for (Bundle bundle : bundles) {
                        if (symbolicName.equals(bundle.getSymbolicName())) {
                            final BundleInfo info = new BundleInfo();
                            info.bundle = bundle;
                            info.newVersion = manifest.getMainAttributes().getValue(
                                Constants.BUNDLE_VERSION);
                            return info;
                        }
                    }

                }
            }

        } finally {

            // close the jar stream or the inputstream, if the jar
            // stream is set, we don't need to close the input stream
            // since closing the jar stream closes the input stream
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException ignore) {
                }
            } else {
                // ins is never null here
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }

        }

        // fall back to no bundle found for update
        return null;
    }

    protected static final class BundleInfo {
        public Bundle bundle;

        public String newVersion;
    }

    /**
     * Starts all bundles which have been stopped during package refresh and
     * all bundles, which have been freshly installed.
     */
    private void startBundles() {
        startBundles(activeBundles, "active");
        startBundles(installedBundles, "installed");
    }
    
    /**
     * Starts all bundles whose bundle ID is contained in the
     * <code>bundleCollection</code>. If a bundle fails to start, its bundle
     * ID is added to the list of active bundles again for the bundle to
     * be started next time the packages are refreshed or the resource queue is
     * processed.
     * 
     * @param bundleIdCollection The IDs of bundles to be started.
     */
    private void startBundles(Collection<Long> bundleIdCollection, String collectionName) {
        if(bundleIdCollection.size() > 0) {
        	log.debug("startBundles({}): {} bundles to process", 
        			collectionName, bundleIdCollection.size());
        }
    	
        // get the bundle ids and remove them from the collection
        Long[] bundleIds;
        synchronized (bundleIdCollection) {
            bundleIds = bundleIdCollection.toArray(new Long[bundleIdCollection.size()]);
            bundleIdCollection.clear();
        }

        for (Long bundleId : bundleIds) {
            Bundle bundle = ctx.getBundle(bundleId);
            if (bundle.getState() != Bundle.ACTIVE) {
                log.info("Starting Bundle {}/{} ", bundle.getSymbolicName(),
                    bundle.getBundleId());
                try {
                    bundle.start();
                } catch (BundleException be) {
                    log.info("Failed to start Bundle " 
                        + bundle.getSymbolicName() + "/" + bundle.getBundleId()
                        + ", rescheduling for start"
                        ,be);

                    // re-add the failed bundle to the activeBundles list
                    synchronized (activeBundles) {
                        activeBundles.add(bundleId);
                    }
                }
            }
        }
    }
}