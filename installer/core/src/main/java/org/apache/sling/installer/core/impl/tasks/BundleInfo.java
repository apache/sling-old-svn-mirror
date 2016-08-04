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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Holds the bundle info that we need, makes it easier to test
 * without an OSGi framework.
 */
public class BundleInfo {

    private static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    final public String symbolicName;
    final public Version version;
    final public int state;
    final public long id;

    public BundleInfo(String symbolicName, Version version, int state, long id) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.state = state;
        this.id = id;
    }

    private BundleInfo(Bundle b) {
        this.symbolicName = b.getSymbolicName();
        this.version = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
        this.state = b.getState();
        this.id = b.getBundleId();
    }

    public static BundleInfo getBundleInfo(final BundleContext bundleContext,
            final String symbolicName, final String version) {
        final Bundle b = getMatchingBundle(bundleContext, symbolicName, version);
        if (b == null) {
            return null;
        }
        return new BundleInfo(b);
    }

    /**
     * Finds the bundle with given symbolic name in our bundle context.
     */
    public static Bundle getMatchingBundle(final BundleContext bundleContext,
            final String bundleSymbolicName, final String version) {
        Bundle match = null;
        if (bundleSymbolicName != null) {
            // check if this is the system bundle
            if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(bundleSymbolicName) ) {
                return bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            }
            final List<Bundle> matchingBundles = new ArrayList<Bundle>();
            final Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
                    matchingBundles.add(bundle);
                }
            }
            if ( matchingBundles.size() > 0 ) {
                final Version searchVersion = (version == null ? null : new Version(version));
                if ( searchVersion == null || searchVersion.compareTo(getBundleVersion(matchingBundles.get(0))) == 0 ) {
                    match = matchingBundles.get(0);
                }
                for(int i=1; i<matchingBundles.size(); i++) {
                    final Bundle current = matchingBundles.get(i);
                    if ( searchVersion == null ) {
                        if ( getBundleVersion(match).compareTo(getBundleVersion(current)) < 0 ) {
                            match = current;
                        }
                    } else {
                        if ( searchVersion.compareTo(getBundleVersion(current)) == 0 ) {
                            match = current;
                            break;
                        }
                    }
                }
            }
        }
        return match;
    }

    private static Version getBundleVersion(final Bundle b) {
        return new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
    }

    /**
     * Check if the version is a snapshot version
     */
    public static boolean isSnapshot(final Version v) {
        return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
    }
}
