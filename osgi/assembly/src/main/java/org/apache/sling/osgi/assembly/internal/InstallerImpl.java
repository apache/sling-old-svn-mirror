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
package org.apache.sling.osgi.assembly.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.osgi.assembly.installer.Installer;
import org.apache.sling.osgi.assembly.installer.InstallerException;
import org.apache.sling.osgi.assembly.installer.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.osgi.service.startlevel.StartLevel;


/**
 * The <code>InstallerImpl</code> TODO
 */
public class InstallerImpl implements Installer {

    private static final long LOCK_ACQUIRE_TIMEOUT = 60L * 1000L; // 60

    // seconds,
    // may be
    // too long
    // ??

    private InstallerServiceImpl controller;

    private Set<URL> repositoryURLs;

    private List<BundleDescriptor> bundleDescriptors;

    private Object lock;

    InstallerImpl(InstallerServiceImpl controller) {
        this.controller = controller;

        this.repositoryURLs = null; // no added repositories
    }

    public void addBundle(String location, URL source, int startLevel) {
        this.addBundleDescriptor(new LocalBundleDescriptor(location, source,
            startLevel));
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#addBundle(java.lang.String, java.io.InputStream, int)
     */
    public void addBundle(String location, InputStream source, int startLevel) {
        this.addBundleDescriptor(new LocalBundleDescriptor(location, source,
            startLevel));
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#addBundle(java.lang.String, org.apache.sling.osgi.assembly.installer.VersionRange, int)
     */
    public void addBundle(String symbolicName, VersionRange versionRange,
            int startLevel) {
        this.addBundleDescriptor(new RepositoryBundleDescriptor(symbolicName,
            versionRange, startLevel));
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#addTemporaryRepository(java.net.URL)
     */
    public void addTemporaryRepository(URL url) {
        if (this.repositoryURLs == null) {
            this.repositoryURLs = new HashSet<URL>();
        }

        this.repositoryURLs.add(url);
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#dispose()
     */
    public void dispose() {
        if (this.lock != null) {
            // controller.releaseLock(lock);
        }

        if (this.repositoryURLs != null) {
            this.repositoryURLs.clear();
            this.repositoryURLs = null;
        }

        if (this.bundleDescriptors != null) {
            for (Iterator<BundleDescriptor> bi = this.bundleDescriptors.iterator(); bi.hasNext();) {
                (bi.next()).dispose();
                bi.remove();
            }
            this.bundleDescriptors = null;
        }
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#install(boolean)
     */
    public Bundle[] install(boolean start) throws InstallerException {

        // quick check for work
        if (this.bundleDescriptors == null || this.bundleDescriptors.isEmpty()) {
            return null;
        }

        Set<URL> addedRepos = null;

        try {
            this.lock = this.controller.acquireLock(LOCK_ACQUIRE_TIMEOUT);

            addedRepos = this.addRepositories();

            List<Bundle> installedBundles = new LinkedList<Bundle>();
            for (Iterator<BundleDescriptor> di = this.bundleDescriptors.iterator(); di.hasNext();) {
                BundleDescriptor bd = di.next();

                try {
                    bd.install(this.controller, installedBundles);
                } catch (BundleException be) {
                    this.controller.log(LogService.LOG_ERROR, be.getMessage());
                }
            }

            boolean doResolve = false;
            if (doResolve) {
                RepositoryAdmin ra = this.controller.getRepositoryAdmin();
                if (ra != null) {
                    Resolver r = ra.resolver();
                    System.err.println("Missing Resources:");
                    if (!r.resolve()) {
                        Requirement[] unsatisfied = r.getUnsatisfiedRequirements();
                        for (int j = 0; j < unsatisfied.length; j++) {
                            System.err.println(" Unsatisfied: " + unsatisfied[j].getFilter());
                        }

                    }

                    Resource[] res = r.getRequiredResources();
                    for (int i=0; res != null && i < res.length; i++) {
                        System.err.println(" Required   : " + res[i].getPresentationName());
                    }
                    res = r.getOptionalResources();
                    for (int i=0; res != null && i < res.length; i++) {
                        System.err.println(" Optional   : " + res[i].getPresentationName());
                    }
                }
            }

            Bundle[] bundles = installedBundles.toArray(new Bundle[installedBundles.size()]);

            if (start) {
                for (int i = 0; i < bundles.length; i++) {
                    try {
                        bundles[i].start();
                    } catch (BundleException be) {
                        this.controller.log(LogService.LOG_WARNING,
                            "Cannot start bundle "
                                + bundles[i].getSymbolicName() + "(id:"
                                + bundles[i].getBundleId() + ")", be);
                    }
                }
            }

            return bundles;

        } catch (IllegalStateException ise) {
            throw new InstallerException(ise.getMessage(), ise);

        } catch (Throwable t) {
            throw new InstallerException(
                "Unexpected Problem during installation", t);

        } finally {

            // release temporary repositories
            this.removeRepositories(addedRepos);

            // release the lock, if we have it
            if (this.lock != null) {
                this.controller.releaseLock(this.lock);
                this.lock = null;
            }
        }
    }

    /**
     * @see org.apache.sling.osgi.assembly.installer.Installer#setDefaultStartLevel(int)
     */
    public void setDefaultStartLevel(int startLevel) {
        // TODO Auto-generated method stub

    }

    // ---------- internal -----------------------------------------------------

    private void addBundleDescriptor(BundleDescriptor bundleDescriptor) {
        if (this.bundleDescriptors == null) {
            this.bundleDescriptors = new LinkedList<BundleDescriptor>();
        }

        this.bundleDescriptors.add(bundleDescriptor);
    }

    private Set<URL> addRepositories() {
        // nothing to do if there are no URLs
        if (this.repositoryURLs == null || this.repositoryURLs.isEmpty()) {
            return null;
        }

        // check for the Repository Admin Service
        RepositoryAdmin ra = this.controller.getRepositoryAdmin();
        if (ra == null) {
            return null;
        }

        // remove all URLs from the set, which allready are defined
        Set<URL> tmp = new HashSet<URL>(this.repositoryURLs);
        Repository[] repos = ra.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++) {
            if (tmp.contains(repos[i].getURL())) {
                tmp.remove(repos[i].getURL());
            }
        }

        // register all URLs with the repository admin service
        for (Iterator<URL> ui = tmp.iterator(); ui.hasNext();) {
            URL url = ui.next();
            try {
                ra.addRepository(url);
            } catch (Exception e) {
                // manager.log(LogService.LOG_WARNING, "Failed to register
                // repository " + url + ", ignoring", e);
                ui.remove();
            }
        }

        return tmp.isEmpty() ? null : tmp;
    }

    private void removeRepositories(Set<URL> urls) {
        RepositoryAdmin ra = this.controller.getRepositoryAdmin();
        if (ra != null && urls != null) {
            for (Iterator<URL> ui = urls.iterator(); ui.hasNext();) {
                ra.removeRepository(ui.next());
            }
        }
    }

    // ---------- internal class -----------------------------------------------

    private static abstract class BundleDescriptor {
        private int startLevel;

        protected BundleDescriptor(int startLevel) {
            this.startLevel = startLevel;
        }

        public int getStartLevel() {
            return this.startLevel;
        }

        public void dispose() {
        }

        public abstract void install(InstallerServiceImpl controller,
                List<Bundle> installedBundles) throws BundleException;

        protected void setStartLevel(InstallerServiceImpl controller,
                Bundle bundle) {
            StartLevel sl = controller.getStartLevel();
            if (sl != null) {
                if (this.startLevel <= 0) {
                    // startLevel = getDefaultStartLevel();
                }

                if (this.startLevel > 0) {
                    sl.setBundleStartLevel(bundle, this.startLevel);
                }
            }
        }
    }

    private static class LocalBundleDescriptor extends BundleDescriptor {
        private String location;

        private URL sourceURL;

        private InputStream source;

        public LocalBundleDescriptor(String location, URL sourceURL,
                int startLevel) {
            super(startLevel);

            this.location = location;
            this.sourceURL = sourceURL;
            this.source = null;
        }

        public LocalBundleDescriptor(String location, InputStream source,
                int startLevel) {
            super(startLevel);

            this.location = location;
            this.sourceURL = null;
            this.source = source;
        }

        public void install(InstallerServiceImpl controller,
                List<Bundle> installedBundles) throws BundleException {

            if (this.source == null) {
                try {
                    this.source = this.sourceURL.openStream();
                } catch (IOException ioe) {
                    throw new BundleException(
                        "Cannot open bundle stream from URL " + this.sourceURL, ioe);
                }
            }

            Bundle bundle = controller.getBundleContext().installBundle(
                this.location, this.source);
            this.setStartLevel(controller, bundle);

            installedBundles.add(bundle);
        }

        public void dispose() {
            super.dispose();

            if (this.source != null) {
                try {
                    this.source.close();
                } catch (IOException ignore) {
                    // ignore
                }
                this.source = null;
            }
        }

        public String toString() {
            return "LocalBundle: " + this.location;
        }
    }

    private static class RepositoryBundleDescriptor extends BundleDescriptor {
        private String symbolicName;

        private VersionRange versionRange;

        public RepositoryBundleDescriptor(String symbolicName,
                VersionRange versionRange, int startLevel) {
            super(startLevel);

            /*
             * Use the given <code>VersionRange</code> if an upper version
             * limit is specified. Otherwise a <code>VersionRange</code> is
             * returned whose upper limit has the same major and minor version
             * as the lower version limit and the micro version is incremented
             * by one but not included in the version range.
             * <p>
             * Example: If <code>range</code> is <em>a.b.c</em> a new
             * version range <em>[a.b.c,a.b.d)</em> is returned where
             * <em>d = c + 1</em>.
             */
            if (versionRange != null && versionRange.getHigh() == null
                && !VersionRange.DEFAULT.equals(versionRange)) {
                
                Version low = versionRange.getLow();
                Version high = new Version(low.getMajor(), low.getMinor(),
                    low.getMicro() + 1);
                versionRange = new VersionRange(low,
                    versionRange.isLowInclusive(), high, false);
            }

            this.symbolicName = symbolicName;
            this.versionRange = versionRange;
        }

        public void install(InstallerServiceImpl controller,
                List<Bundle> installedBundles) throws BundleException {
            RepositoryAdmin ra = controller.getRepositoryAdmin();
            if (ra == null) {
                throw new BundleException("Cannot install bundle "
                    + this.symbolicName + ": Missing RepositoryAdmin Service");
            }

            String filter;
            if (this.versionRange == null
                || (this.versionRange.getHigh() == null && this.versionRange.getLow().equals(
                    Version.emptyVersion))) {
                filter = "(symbolicName=" + this.symbolicName + ")";
            } else {
                filter = "(&(symbolicName=" + this.symbolicName + ")"
                    + this.versionRange.getFilter() + ")";
            }

            Resource[] res = ra.discoverResources(filter);
            if (res == null || res.length == 0) {
                throw new BundleException("Bundle " + this.symbolicName
                    + " (version " + this.versionRange
                    + ") not found in the Bundle Repository");
            }

            // if more than one resources match the filter, select the
            // highest version number
            if (res.length > 1) {
                Resource selected = res[0];
                for (int i=1; i < res.length; i++) {
                    if (res[i].getVersion().compareTo(selected.getVersion()) > 0) {
                        selected = res[i];
                    }
                }

                // substitute the first item with the selected one
                res[0] = selected;
            }

            Resolver resolver = ra.resolver();
            resolver.add(res[0]);

            if (!resolver.resolve()) {
                StringBuffer msg = new StringBuffer("Cannot install bundle "
                    + this.symbolicName + " (version " + this.versionRange
                    + ") due to missing requirements:");
                Requirement[] unsatisfied = resolver.getUnsatisfiedRequirements();
                for (int j = 0; j < unsatisfied.length; j++) {
                    if (j > 0) msg.append(", ");
                    msg.append(unsatisfied[j].getFilter());
                }
                throw new BundleException(msg.toString());
            }

            resolver.deploy(false);

            // after deployment, find the bundles
            final Bundle[] bundles = controller.getBundleContext().getBundles();
            List<Bundle> bundleList = new ArrayList<Bundle>();
            this.getBundles(bundleList, bundles, resolver.getAddedResources());
            this.getBundles(bundleList, bundles, resolver.getRequiredResources());
            this.getBundles(bundleList, bundles, resolver.getOptionalResources());

            for (Iterator<Bundle> bi = bundleList.iterator(); bi.hasNext();) {
                Bundle bundle = bi.next();
                this.setStartLevel(controller, bundle);
                installedBundles.add(bundle);
            }
        }

        private void getBundles(List<Bundle> bundleList, final Bundle[] bundles,
                Resource[] res) {
            if (res == null || res.length == 0) {
                return;
            }

            for (int i = 0; i < res.length; i++) {
                String name = res[i].getSymbolicName();
                Version version = res[i].getVersion();

                // scan bundles from back to front assuming newly installed
                // bundles are towards the end of the list
                for (int j = bundles.length - 1; j >= 0; j--) {
                    if (bundles[j].getSymbolicName().equals(name)
                        && Version.parseVersion(
                            (String) bundles[j].getHeaders().get(
                                Constants.BUNDLE_VERSION)).equals(version)) {
                        bundleList.add(bundles[j]);
                        break;
                    }
                }
            }
        }

        public String toString() {
            return "RepositoryBundle: " + this.symbolicName + " (version:" + this.versionRange + ")";
        }
    }
}
