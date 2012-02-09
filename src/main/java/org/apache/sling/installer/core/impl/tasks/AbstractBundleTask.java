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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * Abstract base class for bundle related tasks.
 */
public abstract class AbstractBundleTask extends AbstractInstallTask {

    private final BundleTaskCreator creator;

    public AbstractBundleTask(final TaskResourceGroup erl, final BundleTaskCreator creator) {
        super(erl);
        this.creator = creator;
    }

    protected PackageAdmin getPackageAdmin() {
        return this.creator.getPackageAdmin();
    }

    protected BundleContext getBundleContext() {
        return this.creator.getBundleContext();
    }

    protected StartLevel getStartLevel() {
        return this.creator.getStartLevel();
    }

    protected BundleTaskCreator getCreator() {
        return this.creator;
    }

    /**
     * Detect the start level for the resource.
     */
    protected int getBundleStartLevel() {
        int startLevel = 0;
        final Object providedLevel;

        if (this.getResource().getDictionary() != null) {
            if ( this.getResource().getDictionary().get(InstallableResource.BUNDLE_START_LEVEL) != null ) {
                providedLevel = this.getResource().getDictionary().get(InstallableResource.BUNDLE_START_LEVEL);
            } else {
                providedLevel = this.getResource().getDictionary().get(InstallableResource.INSTALLATION_HINT);
            }
        } else {
            providedLevel = null;
        }
        if ( providedLevel != null ) {
            if ( providedLevel instanceof Number ) {
                startLevel = ((Number)providedLevel).intValue();
            } else {
                try {
                    startLevel = Integer.valueOf(providedLevel.toString());
                } catch (final NumberFormatException nfe) {
                    // ignore this
                }
            }
        }
        return startLevel;
    }

    /**
     * Get sortable start level - low levels before high levels
     */
    protected String getSortableStartLevel() {
        final int startLevel = this.getBundleStartLevel();
        if ( startLevel == 0 ) {
            return "999";
        } else if ( startLevel < 10 ) {
            return "00" + String.valueOf(startLevel);
        } else if ( startLevel < 100 ) {
            return "0" + String.valueOf(startLevel);
        }
        return String.valueOf(startLevel);
    }

    /**
     * Check if the bundle is active.
     * This is true if the bundle has the active state or of the bundle
     * is in the starting state and has the lazy activation policy.
     * Or if the bundle is a fragment, it's considered active as well
     */
    protected boolean isBundleActive(final Bundle b) {
        if ( b.getState() == Bundle.ACTIVE ) {
            return true;
        }
        if ( b.getState() == Bundle.STARTING && isLazyActivatian(b) ) {
            return true;
        }
        return ( getFragmentHostHeader(b) != null );
    }

    /**
     * Gets the bundle's Fragment-Host header.
     */
    protected String getFragmentHostHeader(final Bundle b) {
        return (String) b.getHeaders().get( Constants.FRAGMENT_HOST );
    }

    /**
     * Check if the bundle has the lazy activation policy
     */
    private boolean isLazyActivatian(final Bundle b) {
        return Constants.ACTIVATION_LAZY.equals(b.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY));
    }

    /**
     * Refresh host bundle
     */
    protected void refreshHostBundle(final Bundle b) {
        final String fragmentHostHeader = getFragmentHostHeader(b);
        if (fragmentHostHeader != null) {
            this.getLogger().debug("Need to do a refresh of the bundle's host");
            for (final Bundle bundle : this.getBundleContext().getBundles()) {
                if (fragmentHostHeader.equals(bundle.getSymbolicName())) {
                    this.getLogger().debug("Found host bundle to refresh {}", bundle.getBundleId());
                    this.getPackageAdmin().refreshPackages(new Bundle[] { bundle });
                    break;
                }
            }
        }
    }
}
