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
package org.apache.sling.launchpad.base.impl.bootstrapcommands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * A Command that uninstalls a bundle, see
 * {@link UninstallBundleCommandTest} for examples
 */
class UninstallBundleCommand implements Command {

    private static String CMD_PREFIX = "uninstall ";
    private final String bundleSymbolicName;
    private final VersionRange versionRange;

    /** Used to create a prototype object, for parsing */
    UninstallBundleCommand() {
        bundleSymbolicName = null;
        versionRange = null;
    }

    /**
     * Used to create an uninstall command with symbolic name and version range
     */
    private UninstallBundleCommand(final String bundleSymbolicName, String versionRangeStr) {
        this.bundleSymbolicName = bundleSymbolicName;

        // If versionRangeStr is not a range, make it strict
        if (!versionRangeStr.contains(",")) {
            versionRangeStr = "[" + versionRangeStr + "," + versionRangeStr + "]";
        }
        this.versionRange = VersionRange.parse(versionRangeStr);
    }

    /**
     * Used to create an uninstall command with symbolic name
     */
    private UninstallBundleCommand(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
        this.versionRange = null;
    }

    /**
     * Gets the bundle's Fragment-Host header.
     */
    private static String getFragmentHostHeader(final Bundle b) {
        return b.getHeaders().get( Constants.FRAGMENT_HOST );
    }

    /**
     * Check whether this is a system bundle fragment.
     * In this case the symbolic name alias "system.bundle" is used.
     * @param fragmentHeader The fragment header
     * @return {@code true} if system bundle fragment.
     */
    private boolean isSystemBundleFragment(final String fragmentHeader) {
        final int pos = fragmentHeader.indexOf(";");
        final String symbolicName = (pos == -1 ? fragmentHeader : fragmentHeader.substring(0, pos));

        return "system.bundle".equals(symbolicName.trim());
    }

    /**
     * @see org.apache.sling.launchpad.base.impl.bootstrapcommands.Command#execute(org.apache.felix.framework.Logger, org.osgi.framework.BundleContext)
     */
    @Override
    public boolean execute(final Logger logger, final BundleContext ctx) throws Exception {
        final Set<String> refreshBundles = new HashSet<String>();
        // Uninstall all instances of our bundle within our version range
        boolean refreshSystemBundle = false;
        for(final Bundle b : ctx.getBundles()) {
            if (b.getSymbolicName().equals(bundleSymbolicName)) {
                if (versionRange == null || versionRange.isInRange(b.getVersion())) {
                    logger.log(Logger.LOG_INFO,
                            this + ": uninstalling bundle version " + b.getVersion());
                    final String fragmentHostHeader = getFragmentHostHeader(b);
                    if (fragmentHostHeader != null) {
                        if ( isSystemBundleFragment(fragmentHostHeader) ) {
                            logger.log(Logger.LOG_INFO, this + ": Need to do a system bundle refresh");
                            refreshSystemBundle = true;
                        } else {
                            logger.log(Logger.LOG_INFO, this + ": Need to do a refresh of the bundle's host: " + fragmentHostHeader);
                            refreshBundles.add(fragmentHostHeader);
                        }
                    }

                    b.uninstall();
                } else {
                    logger.log(Logger.LOG_INFO,
                            this + ": bundle version (" + b.getVersion()+ " not in range, ignored");
                }
            }
        }
        if ( refreshBundles.size() > 0 ) {
            final List<Bundle> bundles = new ArrayList<Bundle>();
            for(final Bundle b : ctx.getBundles() ) {
                if ( refreshBundles.contains(b.getSymbolicName()) ) {
                    logger.log(Logger.LOG_INFO, this + ": Found host bundle to refresh " + b.getBundleId());
                    bundles.add(b);
                }
            }
            if ( bundles.size() > 0 ) {
                final ServiceReference<PackageAdmin> paRef = ctx.getServiceReference(PackageAdmin.class);
                if ( paRef != null ) {
                    try {
                        final PackageAdmin pa = ctx.getService(paRef);
                        if ( pa != null ) {
                            pa.refreshPackages(bundles.toArray(new Bundle[bundles.size()]));
                        }
                    } finally {
                        ctx.ungetService(paRef);
                    }
                }
            }
        }
        return refreshSystemBundle;
    }

    @Override
    public Command parse(String commandLine) throws ParseException {
        if(commandLine.startsWith(CMD_PREFIX)) {
            final String [] s = commandLine.split(" ");
            if (s.length == 3) {
                return new UninstallBundleCommand(s[1].trim(), s[2].trim());
            } else if ( s.length == 2 ) {
                return new UninstallBundleCommand(s[1].trim());
            }
            throw new Command.ParseException("Syntax error: '" + commandLine + "'");
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + bundleSymbolicName + " " + (versionRange != null ? versionRange : "");
    }
}