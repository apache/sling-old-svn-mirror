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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.sling.assembly.installer.Installer;
import org.apache.sling.assembly.installer.InstallerException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;


/**
 * The <code>Assembly</code> class represents an Assembly Bundle, which has
 * been installed into the system.
 */
class Assembly {

    /**
     * The name of the bundle manifest header providing the specification(s) of
     * the bundle(s) to be installed along with this Assembly Bundle (value is
     * "Assembly-Bundles").
     */
    public static final String ASSEMBLY_BUNDLES = "Assembly-Bundles";

    /**
     * The name of the bundle manifest header providing the source of the
     * bundles to be installed (value is "Assembly-BundleRepository").
     * <p>
     * This header may take various values:
     * <dl>
     * <dt><code>embedded</code></dt>
     * <dd>Bundles listed in the {@link #ASSEMBLY_BUNDLES} header are embedded
     * with the Assembly bundle as children of the
     * {@link #EMBEDDED_BUNDLE_LOCATION} folder. This value is also the default
     * if this header is empty or missing. </dd>
     * <dt><code>obr</code></dt>
     * <dd>Bundles listed in the {@link #ASSEMBLY_BUNDLES} header are retrieved
     * from an OSGi Bundle Repository using the RepositoryAdmin service.</dd>
     * <dt>Comma separated list of URLs</dt>
     * <dd>Bundles listed in the {@link #ASSEMBLY_BUNDLES} header are retrieved
     * from an OSGi Bundle Repository where the URLs listed will be temporarily
     * added to the RepositoryAdmin service first. The URLs must refer to a
     * valid repository descriptor as specified in <a
     * href="http://www2.osgi.org/div/rfc-0112_BundleRepository.pdf">OSGi RFC
     * 112 Bundle Repository</a</dd>
     * </dl>
     */
    public static final String ASSEMBLY_BUNDLEREPOSITORY = "Assembly-BundleRepository";

    /**
     * The value if the {@link #ASSEMBLY_BUNDLEREPOSITORY} manifest header
     * indicating the bundles to be installed are embedded in the Assembly
     * Bundle (value is "embedded").
     * <p>
     * If embedded bundles are to be installed, they are found as children of
     * the {@link #EMBEDDED_BUNDLE_LOCATION} folder entry as file entries whose
     * name is constructed from the bundle symbolic name with the
     * {@link #EMBEDDED_BUNDLE_EXTENSION}.
     */
    public static final String ASSEMBLY_BUNDLEREPOSITORY_EMBEDDED = "embedded";

    /**
     * The value if the {@link #ASSEMBLY_BUNDLEREPOSITORY} manifest header
     * indicating the bundles to be installed are to be retrieved through
     * the RepositoryAdmin service (value is "obr").
     */
    public static final String ASSEMBLY_BUNDLEREPOSITORY_OBR = "obr";

    /**
     * The location in the Assembly Bundle of embedded bundles to install (value
     * is "OSGI-INF/bundles/").
     */
    public static final String EMBEDDED_BUNDLE_LOCATION = "OSGI-INF/bundles/";

    /**
     * The extension of embedded bundle entries in the Assembly Bundle (value is
     * ".jar").
     */
    private static final String EMBEDDED_BUNDLE_EXTENSION = ".jar";

    /**
     * Initial state of this assembly after the instance has been created by the
     * constructor (value is 0).
     */
    public static final int STATE_NONE = 0;

    /**
     * State of this assembly after the Assembly Bundle has been installed by
     * the OSGi framework and the <code>BundleEvent.INSTALLED</code> event has
     * been processed by the {@link #install()} method (value is 1). This is
     * also the state of Assembly instances after the assembly bundle as been
     * unresolved by the {@link #unresolve()} method.
     */
    public static final int STATE_INSTALLED = 1;

    /**
     * State of this assembly after the Assembly Bundle has been started by the
     * OSGi framework and the <code>BundleEvent.STARTED</code> event has been
     * processed by the {@link #start()} method (value is 2).
     */
    public static final int STATE_STARTED = 2;

    /**
     * State of this assembly after the Assembly Bundle has been uninstalled by
     * the OSGi framework and the <code>BundleEvent.UNINSTALLED</code> event
     * has been processed by the {@link #uninstall()} method (value is 4).
     */
    public static final int STATE_UNINSTALLED = 4;

    /**
     * The {@link AssemblyManager} taking care of this assembly instance.
     */
    private AssemblyManager manager;

    /**
     * The OSGi <code>Bundle</code> instance representing this Assembly
     * Bundle.
     */
    private Bundle bundle;

    /**
     * The list of {@link BundleSpec bundle specifications} extracted from the
     * {@link #ASSEMBLY_BUNDLES} bundle manifest header.
     */
    private BundleSpec[] bundleSpecs;

    /**
     * The map of {@link InstalledBundle installed bundle instances} indexed by
     * the {@link BundleSpec#getCommonLocation() common location} of the
     * respective bundle specification.
     */
    private Map bundles;

    /**
     * The current state of this instance. This is one of the
     * <code>STATE_*</code> constants.
     */
    int state;

    /**
     * Creates an instance of this class reresenting the Assembly Bundle backed
     * by the given OSGi <code>bundle</code>.
     *
     * @param manager The {@link AssemblyManager} taking care of this assembly.
     * @param bundle The OSGi <code>Bundle</code> representing this Assembly
     *            Bundle.
     */
    Assembly(AssemblyManager manager, Bundle bundle) {
        this.manager = manager;
        this.bundle = bundle;
        this.state = STATE_NONE;
    }

    /**
     * Installs any bundles contained in this Assembly Bundle. The bundles will
     * be installed from embedded entries or a OSGi Bundle Repository as
     * declared in the {@link #ASSEMBLY_BUNDLEREPOSITORY} manifest header.
     * <p>
     * Along with the installation of the bundles, the start levels of the
     * bundles are set according to the bundle specifications if the StartLevel
     * service is available. If no StartLevel service is available, the start
     * levels of the bundles will not be set.
     * <p>
     * After this method completes successfully, the assembly is in
     * {@link #STATE_INSTALLED}.
     *
     * @throws IllegalStateException If this Asssembly Bundle has already been
     *             uninstalled.
     */
    void install() {
        this.ensureInstalledBundles();
    }

    /**
     * Starts all bundles contained in this Assembly Bundle. If this assembly
     * bundle is not yet in the {@link #STATE_RESOLVED resolved state}, the
     * referred to bundles are resolved first as if the {@link #resolve()}
     * method is called. Consequently, the bundles may also be installed if no
     * yet done.
     * <p>
     * Bundles contained in this Assembly Bundle may not actually be started
     * when this method is called, because:
     * <ul>
     * <li>If the StartLevel service is available in the system and the current
     * system start level is below the defined start level of a bundle, the
     * bundle is only permanently marked to be started but not actually started.
     * The bundle will be started as soon as the system start level reaches the
     * bundle's configured start level.
     * <li>If the bundle specification sets the <code>linked</code> parameter
     * to <code>false</code>, the bundle is not started, when the Assembly
     * Bundle is started.
     * </ul>
     * <p>
     * After this method completes successfully, the assembly is in
     * {@link #STATE_STARTED}.
     *
     * @throws IllegalStateException If this Asssembly Bundle has already been
     *             uninstalled.
     */
    void start() {
        this.ensureStarted();
    }

    /**
     * Stops all linked bundles contained in this Assembly Bundle. If this
     * assembly is not in the {@link #STATE_STARTED started state} this method
     * has no effect.
     * <p>
     * Bundles contained in this Assembly Bundle may not actually be stopped
     * when this Assembly Bundle is stopped if the bundle's specification sets
     * the <code>linked</code> parameter to <code>false</code>.
     * <p>
     * After this method completes successfully, the assembly is in
     * {@link #STATE_RESOLVED}.
     *
     * @throws IllegalStateException If this Asssembly Bundle has already been
     *             uninstalled.
     */
    void stop() {
        this.ensureStopped();
    }

    /**
     * Uninstalls all bundles contained in this Assembly Bundle. Some bundles
     * may actually not be installed by this methods if there is at least one
     * other Assembly Bundle still referring to such bundles.
     * <p>
     * This method has no effect if this assembly has already been uninstalled.
     * <p>
     * After this method completes successfully, the assembly is in
     * {@link #STATE_UNINSTALLED}.
     */
    void uninstall() {
        this.ensureUninstalled();
    }

    /**
     * Updates all bundles contained in this Assembly Bundle as follows:
     * <ul>
     * <li>If a bundle has been installed by the previous version of the
     * assembly but is not contained in the new version, the bundle is
     * uninstalled unless it is still referred to by at least one other Assembly
     * Bundle.
     * <li>If the bundle is contained in the previous and the new version, it
     * is updated unless the bundle version of the bundle contained in the new
     * assembly is smaller or equal to the version of the bundle already
     * installed. That is, there is no bundle downgrade.
     * <li>If the bundle has not been contained in the previous version of the
     * assembly it is simply installed.
     * </ul>
     * <p>
     * This method does not change the state of this assembly.
     * <p>
     * This method is not yet implemented.
     * <p>
     *
     * @throws IllegalStateException If this Asssembly Bundle has already been
     *             uninstalled.
     */
    void update() {
        this.checkUninstalled();

        // TODO:
        // - get new bundle specs
        // - compare to existing bundle specs
        // - remove bundles for specs not existing any more
        // - update bundles for specs still existing
        // - install bundles for new specs
    }

    // ---------- Object overwrites --------------------------------------------

    /**
     * Returns the <code>int</code> representation of the bundle ID of the
     * Assembly Bundle as the hash code of this assembly.
     */
    public int hashCode() {
        return (int) this.bundle.getBundleId();
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is this assembly or if
     * <code>obj</code> is an assembly backed by the same OSGi
     * <code>Bundle</code>.
     *
     * @param obj The object to compare this assembly to.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Assembly) {
            return this.bundle.getBundleId() == ((Assembly) obj).bundle.getBundleId();
        }

        return false;
    }

    /**
     * Returns a string representation of this assembly consisting of the bundle
     * symbolic name and bundle location.
     */
    public String toString() {
        return "Assembly " + this.bundle.getSymbolicName() + "/"
            + this.bundle.getLocation();
    }

    // ---------- internal -----------------------------------------------------

    private BundleSpec[] getBundleSpecs() {
        if (this.bundleSpecs == null) {
            // parse header
            String spec = (String) this.bundle.getHeaders().get(ASSEMBLY_BUNDLES);
            if (spec == null) {
                // this is not expected ...
                this.bundleSpecs = new BundleSpec[0];
            } else {
                spec = spec.trim();
                List specs = new ArrayList();
                boolean quoted = false;
                int start = 0;
                for (int i = 0; i < spec.length(); i++) {
                    char c = spec.charAt(i);
                    if (quoted) {
                        if (c == '\\') {
                            // escape skip to next
                            i++;
                        } else if (c == '"') {
                            quoted = false;
                        }
                    } else {
                        if (c == '"') {
                            // start quoting
                            quoted = true;
                        } else if (c == ',') {
                            // spec separation
                            specs.add(new BundleSpec(spec.substring(start, i)));
                            start = i + 1;
                        }
                    }
                }
                if (start < spec.length()) {
                    specs.add(new BundleSpec(spec.substring(start)));
                }
                this.bundleSpecs = (BundleSpec[]) specs.toArray(new BundleSpec[specs.size()]);
            }
        }

        return this.bundleSpecs;
    }

    /**
     * Checks whether this assembly is in {@link #STATE_UNINSTALLED} or not.
     *
     * @throws IllegalStateException If this assembly is in the uninstalled
     *             state.
     */
    private void checkUninstalled() {
        if (this.state == STATE_UNINSTALLED) {
            throw new IllegalStateException("Assembly "
                + this.bundle.getSymbolicName() + " is uninstalled");
        }
    }

    private void ensureInstalledBundles() {
        // do nothing if already installed, throw if uninstalled
        this.checkUninstalled();
        if (this.state >= STATE_INSTALLED) {
            return;
        }

        Map resources = new HashMap();

        Map bundles = this.getBundles();
        BundleSpec[] bundleSpecs = this.getBundleSpecs();
        for (int i = 0; i < bundleSpecs.length; i++) {
            String loc = bundleSpecs[i].getCommonLocation();
            if (bundles.get(loc) == null) {
                InstalledBundle ib = this.manager.getInstalledBundle(loc);
                if (ib != null) {
                    ib.addReferent(this);
                } else {
                    // find bundle
                    Bundle bundle = this.findBundle(loc);
                    if (bundle == null) {
                        bundle = this.findBundle(bundleSpecs[i].getObrLocation());
                    }
                    if (bundle != null) {
                        // check version
                        String version = (String) bundle.getHeaders().get(
                            Constants.BUNDLE_VERSION);
                        Version v = Version.parseVersion(version);
                        if (!bundleSpecs[i].getVersion().isInRange(v)) {
                            // TODO: have to "reinstall" or "update"
                            this.manager.log(LogService.LOG_WARNING, "Bundle "
                                + bundle.getSymbolicName()
                                + " has wrong version " + version
                                + ", expected: " + v);
                        }
                        // claim to be the installer
                        ib = new InstalledBundle(bundleSpecs[i], bundle, this);
                        this.manager.putInstalledBundle(loc, ib);
                    } else {
                        resources.put(bundleSpecs[i].getSymbolicName(), bundleSpecs[i]);
                    }
                }

                // register locally
                if (ib != null) {
                    bundles.put(loc, ib);
                }
            }
        }

        // assume all bundles have already been installed, nothing to do
        if (!resources.isEmpty()) {

            Installer installer = this.manager.getInstaller();

            this.addTemporaryRepositories(installer);

            for (Iterator ei = resources.values().iterator(); ei.hasNext();) {
                BundleSpec bundleSpec = (BundleSpec) ei.next();
                bundleSpec.install(this.bundle, installer);
            }

            Bundle[] installedBundles;
            try {
                installedBundles = installer.install(false);
            } catch (InstallerException ie) {
                this.manager.log(LogService.LOG_ERROR, "Failed to install bundles", ie);
                installedBundles = null;
            } finally {
                installer.dispose();
            }

            // loop through the bundles to find the ones, we have to start as
            // they are indirect dependencies
            for (int i=0; installedBundles != null && i < installedBundles.length; i++) {
                BundleSpec bs = (BundleSpec) resources.get(installedBundles[i].getSymbolicName());
                if (bs != null) {
                    // declared bundle, register internally
                    InstalledBundle ib = new InstalledBundle(bs, installedBundles[i], this);
                    this.manager.putInstalledBundle(bs.getCommonLocation(), ib);
                    this.getBundles().put(bs.getCommonLocation(), ib);
                } else {
                    // automatically installed dependency, start here
                    try {
                        installedBundles[i].start();
                    } catch (BundleException be) {
                        this.manager.log(LogService.LOG_ERROR, "Failed to start bundle "
                            + installedBundles[i].getSymbolicName(), be);
                    }
                }
            }
        }

        // mark as installed now
        this.state = STATE_INSTALLED;
    }

    private void ensureStarted() {
        // do nothing if already started or if the system is starting up
        if (this.state >= STATE_STARTED
            || manager.getBundleContext().getBundle(0).getState() != Bundle.ACTIVE) {
            return;
        }

        // make sure we are installed and resolved
        this.ensureInstalledBundles();

        // now make sure the bundles are marked started
        for (Iterator ii = this.getBundles().values().iterator(); ii.hasNext();) {
            InstalledBundle ib = (InstalledBundle) ii.next();
            if (ib.getBundleSpec().isLinked()) {
                Bundle bundle = ib.getBundle();

                // if the bundle is in uninstalled state, we cannot do anything
                if (bundle.getState() == Bundle.UNINSTALLED) {
                    this.manager.log(LogService.LOG_ERROR, "Cannot start bundle "
                        + bundle.getSymbolicName() + ", already uninstalled");

                // otherwise start the bundle now
                } else {
                    try {
                        bundle.start();
                    } catch (BundleException be) {
                        this.manager.log(LogService.LOG_ERROR,
                            "Failed to start bundle "
                                + bundle.getSymbolicName(), be);
                    }
                }
            }
        }

        this.state = STATE_STARTED;
    }

    private void ensureStopped() {
        // make sure this is not unsinstalled
        this.checkUninstalled();

        // already stopped (or never started) or the system is shutting down
        if (this.state < STATE_STARTED
            || manager.getBundleContext().getBundle(0).getState() != Bundle.ACTIVE) {
            return;
        }

        // now make sure the bundles are marked started
        for (Iterator ii = this.getBundles().values().iterator(); ii.hasNext();) {
            InstalledBundle ib = (InstalledBundle) ii.next();
            if (ib.getBundleSpec().isLinked()) {
                Bundle bundle = ib.getBundle();

                // if the bundle is in uninstalled state, we cannot do anything
                if (bundle.getState() == Bundle.UNINSTALLED) {
                    this.manager.log(LogService.LOG_INFO, "Bundle "
                        + bundle.getSymbolicName() + " already uninstalled");

                // otherwise stop the bundle now
                } else {
                    try {
                        bundle.stop();
                    } catch (BundleException be) {
                        this.manager.log(
                            LogService.LOG_ERROR,
                            "Failed to stop bundle " + bundle.getSymbolicName(),
                            be);
                    }
                }
            }
        }

        this.state = STATE_INSTALLED;
    }

    private void ensureUninstalled() {
        // already uninstalled
        if (this.state == STATE_UNINSTALLED) {
            return;
        }

        // directly uninstall, not needed to go by stopped/unresolved
        for (Iterator bi = this.getBundles().values().iterator(); bi.hasNext();) {
            InstalledBundle ib = (InstalledBundle) bi.next();

            ib.removeReferent(this);
            bi.remove();

            if (!ib.isReferredTo()) {
                if (ib.getBundle().getState() == Bundle.UNINSTALLED) {
                    this.manager.log(LogService.LOG_INFO, "Bundle "
                        + ib.getBundle().getSymbolicName()
                        + " already uninstalled");
                } else {
                    try {
                        ib.getBundle().uninstall();
                    } catch (BundleException be) {
                        this.manager.log(LogService.LOG_ERROR,
                            "Failed to uninstall bundle "
                                + ib.getBundle().getSymbolicName(), be);
                    }
                }

                // remove from the manager
                this.manager.removeInstalledBundle(ib.getBundleSpec().getCommonLocation());
            }
        }

        this.state = STATE_UNINSTALLED;
    }

    private void addTemporaryRepositories(Installer installer) {
        String obrList = (String) this.bundle.getHeaders().get(ASSEMBLY_BUNDLEREPOSITORY);
        if (obrList != null && obrList.length() > 0
                && !ASSEMBLY_BUNDLEREPOSITORY_OBR.equals(obrList)
                && !ASSEMBLY_BUNDLEREPOSITORY_EMBEDDED.equals(obrList)) {
            StringTokenizer tokener = new StringTokenizer(obrList, ",");
            while (tokener.hasMoreTokens()) {
                String token = tokener.nextToken().trim();
                if (token.length() >= 0) {
                    try {
                        installer.addTemporaryRepository(new URL(token));
                    } catch (Exception mue) {
                        this.manager.log(LogService.LOG_WARNING, "Invalid URL "
                            + obrList + ", trying without");
                    }
                }
            }
        }
    }

    private Map getBundles() {
        if (this.bundles == null) {
            this.bundles = new HashMap();
        }

        return this.bundles;
    }

    private Bundle[] getInstalledBundles() {
        Map bundles = this.getBundles();
        Bundle[] installedBundles = new Bundle[bundles.size()];
        int i = 0;
        for (Iterator bi = bundles.values().iterator(); bi.hasNext();) {
            installedBundles[i++] = ((InstalledBundle) bi.next()).getBundle();
        }
        return installedBundles;
    }

    private Bundle findBundle(String loc) {
        Bundle[] bundles = this.manager.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getLocation().startsWith(loc)) {
                return bundles[i];
            }
        }
        return null;
    }
}
