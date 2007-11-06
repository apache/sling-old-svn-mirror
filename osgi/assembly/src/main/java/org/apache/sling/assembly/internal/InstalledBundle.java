/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.assembly.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Bundle;

/**
 * The <code>InstalledBundle</code> object represents a bundle, which has been
 * installed through an Assembly Bundle.
 * <p>
 * Usually, each bundle will be installed as requested by a single Assembly
 * Bundle. There may however be cases, where more than one Assembly Bundle
 * installed in the same framework refer to the same installed bundle. In these
 * cases the installed bundle object is shared amongst Assembly Bundles.
 * <p>
 * As a result an installed bundle will only be uninstalled by the Assembly
 * Manager if there are no more Assembly Bundles referrring to the installed
 * bundle.
 */
public class InstalledBundle {

    /**
     * The {@link BundleSpec bundle specification} from which the bundle was
     * installed.
     */
    private BundleSpec bundleSpec;

    /**
     * The bundle installed in the framework.
     */
    private Bundle bundle;

    /**
     * The {@link Assembly Assembly Bundle} which is considered the current
     * owner of this installed bundle.
     */
    private Assembly installer;

    /**
     * A set of {@link Assembly Assembly Bundles} which refer to this installed
     * bundle. This is <code>null</code> if there is only one Assembly Bundle
     * referring to this installed bundle.
     */
    private Set<Assembly> referents;

    /**
     * Creates an instance of this class for the given bundle specification,
     * bundle and owner Assembly Bundle.
     *
     * @param bundleSpec The {@link BundleSpec} leading to the installation of
     *            this installed bundle.
     * @param bundle The OSGi <code>Bundle</code> instance represented by this
     *            installed bundle object.
     * @param installer The {@link Assembly Assembly Bundle} first referring to
     *            this installed bundle.
     */
    InstalledBundle(BundleSpec bundleSpec, Bundle bundle, Assembly installer) {
        this.bundleSpec = bundleSpec;
        this.bundle = bundle;
        this.installer = installer;
    }

    /**
     * Returns the {@link BundleSpec} of this installed bundle.
     */
    BundleSpec getBundleSpec() {
        return this.bundleSpec;
    }

    /**
     * Returns the OSGi <code>Bundle</code> represented by this installed
     * bundle.
     */
    Bundle getBundle() {
        return this.bundle;
    }

    /**
     * Adds an {@link Assembly Assembly Bundle} to the set of Assembly Bundles
     * referring to this installed bundle.
     *
     * @param referent The {@link Assembly Assembly Bundle} to add.
     */
    void addReferent(Assembly referent) {
        if (this.installer != referent) {
            if (this.referents == null) {
                this.referents = new HashSet<Assembly>();
            }
            this.referents.add(referent);
        }
    }

    /**
     * Removes a {@link Assembly Assembly Bundle} from the set of Assembly
     * Bundles referring to this installed bundle.
     *
     * @param referent The {@link Assembly Assembly Bundle} to remove.
     */
    void removeReferent(Assembly referent) {
        if (this.installer == referent) {
            if (this.referents != null) {
                Iterator<Assembly> ri = this.referents.iterator();
                this.installer = ri.next();
                ri.remove();
            } else {
                this.installer = null;
            }
        } else {
            this.referents.remove(referent);
        }

        // if there are not more referents, remove the set
        if (this.referents != null && this.referents.isEmpty()) {
            this.referents = null;
        }
    }

    /**
     * Returns <code>true</code> if at least one
     * {@link Assembly Assembly Bundle} still refers to this installed bundle.
     */
    boolean isReferredTo() {
        return this.installer != null;
    }

    // ---------- Object overwrites --------------------------------------------

    /**
     * Returns the <code>int</code> representation of the bundle ID of the
     * installed OSGi bundle as the hash code of this installed bundle.
     */
    public int hashCode() {
        return (int) this.bundle.getBundleId();
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is this installed
     * bundle or if <code>obj</code> is an installed bundle referring to the
     * same OSGi <code>Bundle</code>.
     *
     * @param obj The object to compare this installed bundle to.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof InstalledBundle) {
            return this.bundle.getBundleId() == ((InstalledBundle) obj).bundle.getBundleId();
        }

        return false;
    }

    /**
     * Returns a string representation of this installed bundle consisting of
     * the bundle symbolic name and bundle location.
     */
    public String toString() {
        return "Installed Bundle " + this.bundle.getSymbolicName() + "/"
            + this.bundle.getLocation();
    }
}
