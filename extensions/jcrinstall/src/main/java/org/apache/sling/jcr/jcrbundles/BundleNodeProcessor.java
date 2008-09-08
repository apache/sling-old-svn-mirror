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
package org.apache.sling.jcr.jcrbundles;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.apache.sling.jcr.jcrbundles.JcrBundlesConstants.JCRBUNDLES_LOCATION_PREFIX;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Process nodes that look like bundles, based on their node name
 */
class BundleNodeProcessor extends AbstractNodeProcessor {
    private final ComponentContext context;
    private final PackageAdmin padmin;
    private final JcrBundlesManager mgr;

    public BundleNodeProcessor(JcrBundlesManager mgr, ComponentContext ctx, PackageAdmin pa) {
        super("[a-zA-Z0-9].*\\.jar$");
        this.mgr = mgr;
        context = ctx;
        padmin = pa;
    }

    public void process(Node n, Map<String, Boolean> flags) throws RepositoryException {

        // Do we have file data?
        InputStream is = null;
        try {
            is = getInputStream(n);
        } catch (Exception e) {
            log.info("Unable to get InputStream on Node {}, will be ignored ({})", n.getPath(), e);
            return;
        }

        // We have data - install, update or do nothing with bundle
        // TODO handle deletes (store a list of bundles that were installed)
        final String location = getBundleLocation(n.getPath());
        try {
            final Node status = getStatusNode(n, true);
            final Bundle oldBundle = getBundleByLocation(location);
            final Calendar lastModified = getLastModified(n);
            Bundle newBundle = null;
            boolean changed = false;

            if (oldBundle == null) {
                // Bundle not installed yet, install it
                changed = true;
                newBundle = getBundleContext().installBundle(location, is);
                if (!isFragment(newBundle)) {
                    mgr.addPendingBundle(location, newBundle);
                } else {
                    log.info("Fragment bundle {} successfully installed", location);
                }
                status.setProperty("status", "installed");
                flags.put("refresh.packages", Boolean.TRUE);

            } else {
                // Bundle already installed, did it change?
                Calendar savedLastModified = null;
                if (status.hasProperty(JCR_LAST_MODIFIED)) {
                    savedLastModified = status.getProperty(JCR_LAST_MODIFIED).getDate();
                }

                changed = savedLastModified == null
                        || lastModified == null
                        || !(lastModified.equals(savedLastModified));

                if (changed) {
                    try {
                        oldBundle.update(is);
                        log.info("Bundle {} successfully updated", location);
                        status.setProperty("status", "updated");
                    } catch (BundleException e) {
                        mgr.addPendingBundle(location, oldBundle);
                        log.info("Bundle {} could not be updated. added to list of pending bundles", location, e);
                    }
                } else {
                    log.debug("Bundle {} unchanged, no update needed", location);
                }
            }

            if (changed) {
                flags.put("refresh.packages", Boolean.TRUE);
                if (lastModified != null) {
                    status.setProperty(JCR_LAST_MODIFIED, lastModified);
                }
                n.getSession().save();
            }

        } catch (Exception e) {
            log.warn("Exception while processing bundle {}", e);

        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                log.error("IOException on closing InputStream", ioe);
            }
        }
    }

    /**
     * Check if the given statusNode still has an equivalent in the main tree, and if not
     * uninstall the corresponding bundle, if found. Do the same thing for statusNodes' children
     */
    public void checkDeletions(Node statusNode, Map<String, Boolean> flags) throws Exception {
        final Node mainNode = getMainNode(statusNode);
        if (mainNode == null) {
            final String mainPath = getMainNodePath(statusNode.getPath());
            final String location = getBundleLocation(mainPath);
            final Bundle b = getBundleByLocation(location);
            // make sure it's removed from the pending list
            mgr.removePendingBundle(location);
            if (b == null) {
                log.info("Node {} has been deleted, but corresponding bundle {} not found - deleting status node only",
                        mainPath, location);
            } else {
                try {
                    b.uninstall();
                    flags.put("refresh.packages", Boolean.TRUE);
                    log.info("Node {} has been deleted, bundle {} uninstalled", mainPath, location);
                } catch (Exception e) {
                    log.error("Exception while trying to uninstall bundle " + location, e);
                }
            }

            statusNode.remove();
            statusNode.getSession().save();
        }
    }

    private boolean isFragment(Bundle bundle) {
        return padmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    private BundleContext getBundleContext() {
        return context.getBundleContext();
    }

    protected Bundle getBundleByLocation(String location) {
        Bundle bundles[] = getBundleContext().getBundles();
        for (Bundle b : bundles) {
            if (location.equals(b.getLocation())) {
                return b;
            }
        }
        return null;
    }

    protected String getBundleLocation(String bundleNodePath) {
        return JCRBUNDLES_LOCATION_PREFIX + bundleNodePath;
    }
}