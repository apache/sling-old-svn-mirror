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
import java.util.ListIterator;

import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;

public abstract class BaseStartLevel {

    private static final String BOOT_MARKER = "boot";

    public abstract List<Bundle> getBundles();

    private int startLevel;

    public boolean removeBundle(Bundle bundle, boolean compareVersions) {
        for (ListIterator<Bundle> it = getBundles().listIterator(); it.hasNext();) {
            if (isSameArtifact(bundle, it.next(), compareVersions)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean containsBundle(Bundle bundle, boolean compareVersions) {
        for (Bundle compare : getBundles()) {
            return isSameArtifact(bundle, compare, compareVersions);
        }
        return false;
    }

    public Bundle getBundle(Bundle bundle, boolean compareVersions) {
        for (Bundle compare : getBundles()) {
            if (isSameArtifact(bundle, compare, compareVersions)) {
                return compare;
            }
        }
        return null;
    }

    private boolean isSameArtifact(Bundle bundle1, Bundle bundle2, boolean compareVersions) {
        boolean result = compareVersions ? bundle1.getVersion().equals(bundle2) : true;
        return result && bundle1.getArtifactId().equals(bundle2.getArtifactId())
                && bundle1.getGroupId().equals(bundle2.getGroupId()) && bundle1.getType().equals(bundle2.getType());
    }

    /**
     * Set the level field.
     *
     * @param level
     */
    public void setLevel( final String level ) {
        if ( BOOT_MARKER.equalsIgnoreCase(level) ) {
            this.startLevel = -1;
        } else {
            this.startLevel = Integer.valueOf(level);
            if ( this.startLevel < 0 ) {
                throw new IllegalArgumentException("Start level must either be '" + BOOT_MARKER + "' or non-negative: " + level);
            }
        }
    }

    public void setRawLevel( final int level ) {
        this.startLevel = level;
    }

    public String getLevel() {
        return (this.startLevel == -1 ? BOOT_MARKER : String.valueOf(this.startLevel));
    }

    public int getStartLevel() {
        return this.startLevel;
    }
}
