/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.projectsupport.bundlelist;

import java.util.List;

import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.osgi.framework.Version;

public abstract class BaseBundleList {

    public abstract List<StartLevel> getStartLevels();

    public Bundle get(Bundle bundle, boolean compareVersions) {
        for (StartLevel sl : getStartLevels()) {
            Bundle foundBundle = sl.getBundle(bundle, compareVersions);
            if (foundBundle != null) {
                return foundBundle;
            }
        }
        return null;
    }

    public boolean remove(Bundle bundle, boolean compareVersions) {
        for (StartLevel sl : getStartLevels()) {
            if (sl.removeBundle(bundle, compareVersions)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Merge the current bundle list with an additional list.
     * @see #add(Bundle)
     *
     * @param bundleList the new bundle list
     */
    public void merge(BundleList bundleList) {
        for (StartLevel sl : bundleList.getStartLevels()) {
            for (Bundle bnd : sl.getBundles()) {
                add(sl, bnd);
            }
        }
    }

    /**
     * Add an artifact definition. If it already exists, update the version, but
     * do not change the start level.
     *
     * @param newBnd the bundle to add
     */
    public void add(Bundle newBnd) {
       add(null, newBnd);
    }

    /**
     * Merge bundle into a start level using the supplied level if present.
     * @param mergeStartLevel
     * @param newBnd
     */
    private void add(StartLevel mergeStartLevel, Bundle newBnd) {
        Bundle current = get(newBnd, false);
        if (current != null) {
            final Maven2OsgiConverter converter = new DefaultMaven2OsgiConverter();

            // compare versions, the highest will be used
            final Version newVersion = new Version(converter.getVersion(newBnd.getVersion()));
            final Version oldVersion = new Version(converter.getVersion(current.getVersion()));
            if ( newVersion.compareTo(oldVersion) > 0 ) {
                current.setVersion(newBnd.getVersion());
            }
        } else {
            StartLevel startLevel = null;
            if ( mergeStartLevel == null || newBnd.getStartLevel() != 0) {
                startLevel = getOrCreateStartLevel(newBnd.getStartLevel());
            } else {
                startLevel = getOrCreateStartLevel(mergeStartLevel.getStartLevel());
            }
            startLevel.getBundles().add(newBnd);
        }
    }

    private StartLevel getOrCreateStartLevel(int startLevel) {
        for (StartLevel sl : getStartLevels()) {
            if (sl.getStartLevel() == startLevel) {
                return sl;
            }
        }

        StartLevel sl = new StartLevel();
        getStartLevels().add(sl);
        sl.setRawLevel(startLevel);
        return sl;
    }
}
