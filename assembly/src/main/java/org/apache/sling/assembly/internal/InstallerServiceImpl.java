/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.assembly.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.sling.assembly.installer.BundleRepositoryAdmin;
import org.apache.sling.assembly.installer.Installer;
import org.apache.sling.assembly.installer.InstallerException;
import org.apache.sling.assembly.installer.InstallerService;
import org.apache.sling.assembly.installer.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


public class InstallerServiceImpl implements InstallerService,
        FrameworkListener {

    public static final String PROP_SLING_INSTALL_BUNDLES = "sling.install.bundles";
    public static final String PROP_SLING_INSTALL_PREFIX = "sling.install.";

    private BundleContext bundleContext;

    private RepositoryAdmin repositoryAdmin;

    private StartLevel startLevel;

    private PackageAdmin packageAdmin;

    private LogService log;

    private BundleRepositoryAdmin bundleRepositoryAdmin;

    // the currently issued lock - might need a way to unlock !!
    private Object lock;

    private boolean installationComplete;

    InstallerServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        bundleContext.addFrameworkListener(this);
    }

    // ---------- FrameworkListener --------------------------------------------

    public void frameworkEvent(FrameworkEvent event) {
        // as soon as the framework has started, we will try to install
        // and or update missing bundles
        if (event.getType() == FrameworkEvent.STARTED) {
            Thread t = new Thread("OBR Installation and Update") {
                public void run() {
                    InstallerServiceImpl.this.install();
                }
            };
            t.start();

            // don't care for any more framework events now
            this.bundleContext.removeFrameworkListener(this);
        }
    }

    // ---------- InstallerService interface -----------------------------------

    public Installer getInstaller() {
        return new InstallerImpl(this);
    }

    public BundleRepositoryAdmin getBundleRepositoryAdmin() {
        if (this.bundleRepositoryAdmin == null) {
            this.bundleRepositoryAdmin = new BundleRepositoryAdminImpl(this);
        }
        return this.bundleRepositoryAdmin;
    }

    // ---------- Concurrency support installers -------------------------------

    /**
     * @throws IllegalStateException If the lock cannot be acquired within the
     *             <code>timeout</code> number of milliseconds or if the
     *             thread waiting for the lock has been interrupted.
     */
    /* package */synchronized Object acquireLock(long timeout) {
        if (this.lock != null) {
            try {
                this.wait((timeout < 0) ? 0L : timeout);
            } catch (InterruptedException ie) {
                // have been interrupted, check whether the lock is free
            }

            // if the lock is still held, fail
            if (this.lock != null) {
                throw new IllegalStateException("Installer is locked");
            }
        }

        // create a lock and return it
        this.lock = new Object();
        return this.lock;
    }

    /* package */synchronized void releaseLock(Object object) {
        // if object is the lock, clear the lock and inform waiting threads
        if (object == this.lock) {
            this.lock = null;

            // inform other parties interested in acquiring the lock
            this.notifyAll();
        }
    }

    // ---------- internal -----------------------------------------------------

    private void install() {
        String keyList = this.bundleContext.getProperty(PROP_SLING_INSTALL_BUNDLES);
        if (keyList == null) {
            this.log(LogService.LOG_INFO, "No " + PROP_SLING_INSTALL_BUNDLES
                + " property. Nothing to install.");
            return;
        }

        Installer installer = this.getInstaller();

        StringTokenizer keys = new StringTokenizer(keyList, ",");
        while (keys.hasMoreTokens()) {
            String key = keys.nextToken().trim();
            String bundleList = this.bundleContext.getProperty(PROP_SLING_INSTALL_PREFIX + key);
            if (bundleList == null) {
                this.log(LogService.LOG_INFO, "Ignoring missing install key " + key);
                continue;
            }

            int startLevel = 1;
            try {
                startLevel = Integer.parseInt(key);
            } catch (NumberFormatException nfe) {
                // ignore (log)
            }

            // build a list of tokens
            // we use this to ignore delims inside a quote
            final List tokens = new ArrayList();
            String prefix = null;
            final StringTokenizer bundles = new StringTokenizer(bundleList, ",");
            while ( bundles.hasMoreTokens() ) {
                final String bundleToken = bundles.nextToken().trim();
                // now count quotes
                int count = 0;
                int start = 0;
                while ( start < bundleToken.length() ) {
                    start = bundleToken.indexOf('"', start);
                    if ( start != -1 ) {
                        // escaped?
                        if ( start == 0 || bundleToken.charAt(start-1) != '\\' ) {
                            count++;
                        }
                        start++;
                    } else {
                        start = bundleToken.length();
                    }
                }
                boolean foundToken = true;
                if ( count % 2 == 1 ) {
                    if ( prefix == null ) {
                        foundToken = false;
                    }
                } else {
                    if ( prefix != null ) {
                        foundToken = false;
                    }
                }
                if ( !foundToken ) {
                    prefix = (prefix == null ? bundleToken : prefix + ',' + bundleToken);
                } else {
                    String value = (prefix == null ? bundleToken : prefix + ',' + bundleToken);
                    tokens.add(value);
                    prefix = null;
                }
            }
            final Iterator i = tokens.iterator();
            while ( i.hasNext() ) {
                final String bundleToken = (String)i.next();
                int colon = bundleToken.indexOf(':');
                String name = (colon >= 0) ? bundleToken.substring(0, colon) : bundleToken;
                // ignore entry if name is empty
                if (name == null || name.length() == 0) {
                    continue;
                }

                String version = (colon >= 0) ? bundleToken.substring(colon+1) : null;
                if ( version != null ) {
                    if ( version.startsWith("\"") ) {
                        version = version.substring(1);
                    }
                    if ( version.endsWith("\"") ) {
                        version = version.substring(0, version.length() - 1);
                    }
                }
                // ignore entry if already present
                if (this.isBundleInstalled(name, version)) {
                    continue;
                }

                installer.addBundle(name, new VersionRange(version), startLevel);
            }
        }

        try {
            installer.install(true);
        } catch (InstallerException ie) {
            this.log(LogService.LOG_ERROR, "Installation failure", ie);
        } finally {
            // clean up installer service
            installer.dispose();
        }
    }

    void log(int level, String message) {
        this.log(level, message, null);
    }

    void log(int level, String message, Throwable t) {
        if (this.getLogService() != null) {
            this.getLogService().log(level, message, t);
        } else {
            System.err.println(message);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        }
    }

    private boolean isBundleInstalled(String name, String version) {
        if (this.getPackageAdmin() != null) {
            Bundle bundles[] = this.getPackageAdmin().getBundles(name, version);
            return bundles != null && bundles.length > 0;
        }

        Bundle bundles[] = this.bundleContext.getBundles();
        for (int i = 0; i < bundles.length; i++) {

            // ignore uninstalled bundles
            if (bundles[i].getState() == Bundle.UNINSTALLED) {
                continue;
            }

            if (name.equals(bundles[i].getSymbolicName())) {
                VersionRange required = new VersionRange(version);
                Version installed = Version.parseVersion((String) bundles[i].getHeaders().get(
                    Constants.BUNDLE_VERSION));

                if (required.isInRange(installed) ) {
                    return true;
                }
            }
        }

        // none found (or incorrect version)
        return false;
    }

    // ---------- SCR integration ----------------------------------------------

    protected BundleContext getBundleContext() {
        return this.bundleContext;
    }

    protected RepositoryAdmin getRepositoryAdmin() {
        if (this.repositoryAdmin == null) {
            this.repositoryAdmin = (RepositoryAdmin) this.getService(RepositoryAdmin.class);
        }
        return this.repositoryAdmin;
    }

    protected StartLevel getStartLevel() {
        if (this.startLevel == null) {
            this.startLevel = (StartLevel) this.getService(StartLevel.class);
        }
        return this.startLevel;
    }

    protected PackageAdmin getPackageAdmin() {
        if (this.packageAdmin == null) {
            this.packageAdmin = (PackageAdmin) this.getService(PackageAdmin.class);
        }
        return this.packageAdmin;
    }

    protected LogService getLogService() {
        if (this.log == null) {
            this.log = (LogService) this.getService(LogService.class);
        }
        return this.log;
    }

    private Object getService(Class serviceClass) {
        ServiceReference ref = this.bundleContext.getServiceReference(serviceClass.getName());
        if (ref != null) {
            return this.bundleContext.getService(ref);
        }

        return null;
    }
}
