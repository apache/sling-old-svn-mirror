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

import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.osgi.assembly.installer.BundleRepositoryAdmin;
import org.apache.sling.osgi.assembly.installer.Repository;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;


/**
 * The <code>BundleRepositoryAdminImpl</code> TODO
 */
class BundleRepositoryAdminImpl implements BundleRepositoryAdmin {

    private InstallerServiceImpl installerService;

    BundleRepositoryAdminImpl(InstallerServiceImpl installerService) {
        this.installerService = installerService;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.assembly.installer.BundleRepositoryAdmin#addRepository(java.net.URL)
     */
    public void addRepository(URL url) {
        Object lock = this.installerService.acquireLock(0);
        try {
            this.getRepositoryAdmin().addRepository(url);
        } catch (Exception e) {
            // TODO: log
        } finally {
            this.installerService.releaseLock(lock);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.assembly.installer.BundleRepositoryAdmin#getRepositories()
     */
    public Iterator<Repository> getRepositories() {
        Object lock = this.installerService.acquireLock(0);
        try {
            org.osgi.service.obr.Repository[] repos = this.getRepositoryAdmin().listRepositories();
            if (repos == null || repos.length == 0) {
                return Collections.EMPTY_LIST.iterator();
            }

            SortedSet<Repository> urlSet = new TreeSet<Repository>();
            for (int i = 0; i < repos.length; i++) {
                urlSet.add(new RepositoryImpl(repos[i]));
            }
            return urlSet.iterator();
        } finally {
            this.installerService.releaseLock(lock);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.assembly.installer.BundleRepositoryAdmin#getResources()
     */
    public Iterator<org.apache.sling.osgi.assembly.installer.Resource> getResources() {
        Object lock = this.installerService.acquireLock(0);
        try {
            org.osgi.service.obr.Repository[] repos = this.getRepositoryAdmin().listRepositories();
            if (repos == null || repos.length == 0) {
                return Collections.EMPTY_LIST.iterator();
            }

            SortedSet<org.apache.sling.osgi.assembly.installer.Resource> resSet = new TreeSet<org.apache.sling.osgi.assembly.installer.Resource>();
            for (int i = 0; i < repos.length; i++) {
                Resource[] resources = repos[i].getResources();
                if (resources != null) {
                    for (int j = 0; j < resources.length; j++) {
                        resSet.add(new ResourceImpl(resources[j]));
                    }
                }
            }
            return resSet.iterator();
        } finally {
            this.installerService.releaseLock(lock);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.assembly.installer.BundleRepositoryAdmin#refreshRepositories()
     */
    public void refreshRepositories() {
        // note: refreshing is implemented by re-adding the repositories
        Object lock = this.installerService.acquireLock(0);
        try {
            org.osgi.service.obr.Repository[] repos = this.getRepositoryAdmin().listRepositories();
            for (int i = 0; repos != null && i < repos.length; i++) {
                this.addRepository(repos[i].getURL());
            }
        } finally {
            this.installerService.releaseLock(lock);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.core.assembly.installer.BundleRepositoryAdmin#removeRepository(java.net.URL)
     */
    public void removeRepository(URL url) {
        Object lock = this.installerService.acquireLock(0);
        try {
            this.getRepositoryAdmin().removeRepository(url);
        } finally {
            this.installerService.releaseLock(lock);
        }
    }

    // ---------- internal class -----------------------------------------------

    private RepositoryAdmin getRepositoryAdmin() {
        return this.installerService.getRepositoryAdmin();
    }

    // ---------- internal classes ---------------------------------------------

    private static class ResourceImpl implements
            org.apache.sling.osgi.assembly.installer.Resource, Comparable<ResourceImpl> {

        private final Resource delegatee;

        ResourceImpl(Resource delegatee) {
            this.delegatee = delegatee;
        }

        public String getPresentationName() {
            return this.delegatee.getPresentationName();
        }

        public String getSymbolicName() {
            return this.delegatee.getSymbolicName();
        }

        public Version getVersion() {
            return this.delegatee.getVersion();
        }

        public String[] getCategories() {
            return this.delegatee.getCategories();
        }

        // ---------- Comparable -----------------------------------------------

        public int compareTo(ResourceImpl other) {
            if (this == other) {
                return 0;
            }

            if (this.getSymbolicName().equals(other.getSymbolicName())) {
                return this.getVersion().compareTo(other.getVersion());
            }

            return this.getSymbolicName().compareTo(other.getSymbolicName());
        }

        // ---------- Object overwrite -----------------------------------------

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ResourceImpl) {
                ResourceImpl other = (ResourceImpl) obj;
                return this.getSymbolicName().equals(other.getSymbolicName())
                    && this.getVersion().equals(other.getVersion());
            }

            return false;
        }

        public int hashCode() {
            return this.getSymbolicName().hashCode() * 17 + this.getVersion().hashCode()
                * 33;
        }
    }

    private static class RepositoryImpl implements
            Repository, Comparable<RepositoryImpl> {

        private final org.osgi.service.obr.Repository delegatee;

        RepositoryImpl(org.osgi.service.obr.Repository delegatee) {
            this.delegatee = delegatee;
        }

        public String getName() {
            return this.delegatee.getName();
        }

        public long getLastModified() {
            return this.delegatee.getLastModified();
        }

        public URL getURL() {
            return this.delegatee.getURL();
        }

        // ---------- Comparable -----------------------------------------------

        public int compareTo(RepositoryImpl other) {
            if (this == other) {
                return 0;
            }

            if (this.getName().equals(other.getName())) {
                return this.getURL().toString().compareTo(other.getURL().toString());
            }

            return this.getName().compareTo(other.getName());
        }

        // ---------- Object overwrite -----------------------------------------

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof RepositoryImpl) {
                RepositoryImpl other = (RepositoryImpl) obj;
                return this.getURL().equals(other.getURL());
            }

            return false;
        }

        public int hashCode() {
            return this.getURL().hashCode();
        }
    }
}
