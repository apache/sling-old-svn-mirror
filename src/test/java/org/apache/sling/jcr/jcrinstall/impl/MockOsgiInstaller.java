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
package org.apache.sling.jcr.jcrinstall.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;


class MockOsgiInstaller implements OsgiInstaller {

    static class InstallableResourceComparator implements Comparator<InstallableResource> {
        public int compare(InstallableResource a, InstallableResource b) {
            return a.getId().compareTo(b.getId());
        }

    }

    /** Keep track of our method calls, for verification */
    private final List<String> recordedCalls = new LinkedList<String>();

    /** Keep track of registered URLS */
    private final Set<String> urls = new HashSet<String>();

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#addResource(java.lang.String, org.apache.sling.osgi.installer.InstallableResource)
     */
    public void addResource(final String scheme, InstallableResource d) {
    	urls.add(scheme + ':' + d.getId());
        recordCall("add", scheme, d);
    }

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#registerResources(java.lang.String, java.util.Collection)
     */
    public void registerResources(String urlScheme, Collection<InstallableResource> data) {
        // Sort the data to allow comparing the recorded calls reliably
        final List<InstallableResource> sorted = new LinkedList<InstallableResource>();
        sorted.addAll(data);
        Collections.sort(sorted, new InstallableResourceComparator());
        for(InstallableResource r : data) {
        	urls.add(urlScheme + ':' + r.getId());
            recordCall("register", urlScheme, r);
        }
    }

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#removeResource(java.lang.String, String)
     */
    public void removeResource(String urlScheme, String url) {
    	urls.remove(urlScheme + ':' + url);
    	synchronized ( this) {
            recordedCalls.add("remove:" + urlScheme + ':' + url + ":100");
    	}
    }

    private synchronized void recordCall(String prefix, String scheme, InstallableResource r) {
        recordedCalls.add(prefix + ":" + scheme + ":" + r.getId() + ":" + r.getPriority());
    }

    synchronized void clearRecordedCalls() {
        recordedCalls.clear();
    }

    List<String> getRecordedCalls() {
        return recordedCalls;
    }

    boolean isRegistered(String urlScheme, String path) {
        final String url = urlScheme + ':' + path;
    	return urls.contains(url);
    }
}
