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
package org.apache.sling.installer.provider.jcr.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;


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
     * @see org.apache.sling.installer.api.OsgiInstaller#updateResources(java.lang.String, org.apache.sling.installer.api.InstallableResource[], java.lang.String[])
     */
    public void updateResources(final String scheme,
            final InstallableResource[] resources, final String[] ids) {
        if ( resources != null ) {
            for(final InstallableResource d : resources) {
                urls.add(scheme + ':' + d.getId());
                recordCall("add", scheme, d);
            }
        }
        if ( ids != null ) {
            for(final String id : ids ) {
                urls.remove(scheme + ':' + id);
                synchronized ( this) {
                    recordedCalls.add("remove:" + scheme + ':' + id + ":100");
                }
            }
        }
    }

    /**
     * @see org.apache.sling.installer.api.OsgiInstaller#registerResources(java.lang.String, org.apache.sling.installer.api.InstallableResource[])
     */
    public void registerResources(String urlScheme, final InstallableResource[] data) {
        // Sort the data to allow comparing the recorded calls reliably
        final List<InstallableResource> sorted = new LinkedList<InstallableResource>();
        for(final InstallableResource r : data) {
            sorted.add(r);
        }
        Collections.sort(sorted, new InstallableResourceComparator());
        for(InstallableResource r : sorted) {
        	urls.add(urlScheme + ':' + r.getId());
            recordCall("register", urlScheme, r);
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
