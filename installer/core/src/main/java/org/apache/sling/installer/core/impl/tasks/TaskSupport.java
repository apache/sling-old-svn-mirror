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

import org.apache.sling.installer.core.impl.util.BundleRefresher;
import org.apache.sling.installer.core.impl.util.PABundleRefresher;
import org.apache.sling.installer.core.impl.util.WABundleRefresher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Support class for the tasks.
 */
public class TaskSupport {

    /** Interface of the package admin */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** Interface of the start level */
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    /** Tracker for the package admin. */
    private final ServiceTracker packageAdminTracker;

    /** Tracker for the start level service. */
    private final ServiceTracker startLevelTracker;

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** Checked for wire admin? */
    private Boolean checkedWireAdmin;

    public TaskSupport(final BundleContext bc) {
        this.bundleContext = bc;

        // create and start tracker
        this.packageAdminTracker = new ServiceTracker(bc, PACKAGE_ADMIN_NAME, null);
        this.packageAdminTracker.open();
        this.startLevelTracker = new ServiceTracker(bc, START_LEVEL_NAME, null);
        this.startLevelTracker.open();
    }

    /**
     * Deactivate this helper.
     */
    public void deactivate() {
        if ( this.packageAdminTracker != null ) {
            this.packageAdminTracker.close();
        }
        if ( this.startLevelTracker != null ) {
            this.startLevelTracker.close();
        }
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public StartLevel getStartLevel() {
        return (StartLevel) this.startLevelTracker.getService();
    }

    public BundleRefresher getBundleRefresher() {
        if ( checkedWireAdmin == null ) {
            try {
                this.bundleContext.getBundle(0).adapt(FrameworkWiring.class);
                checkedWireAdmin = true;
            } catch (final Throwable t) {
                checkedWireAdmin = false;
            }
        }
        if ( checkedWireAdmin.booleanValue() ) {
            return new WABundleRefresher(this.bundleContext.getBundle(0).adapt(FrameworkWiring.class),
                    this.bundleContext);
        } else {
            return new PABundleRefresher((PackageAdmin) this.packageAdminTracker.getService(),
                    this.bundleContext);
        }
    }

}
