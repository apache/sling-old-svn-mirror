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
package org.apache.sling.slingstart.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A feature is a collection of
 * - artifacts (through start levels)
 * - configurations
 * - settings
 *
 * A feature might be tied to run modes. Only if all run modes are active,
 * this feature is active.
 * In addition to custom, user defined run modes, special run modes exists.
 * A special run mode name starts with a colon.
 */
public class SSMFeature
    extends SSMTraceable
    implements Comparable<SSMFeature> {

    private final String[] runModes;

    private final List<SSMStartLevel> startLevels = new ArrayList<SSMStartLevel>();

    private final List<SSMConfiguration> configurations = new ArrayList<SSMConfiguration>();

    private final Map<String, String> settings = new HashMap<String, String>();

    public SSMFeature(final String[] runModes) {
        this.runModes = getSortedRunModesArray(runModes);
    }

    public static String[] getSortedRunModesArray(final String[] runModes) {
        // sort run modes
        if ( runModes != null ) {
            final List<String> list = new ArrayList<String>();
            for(final String m : runModes) {
                if ( m != null ) {
                    if ( !m.trim().isEmpty() ) {
                        list.add(m.trim());
                    }
                }
            }
            if ( list.size() > 0 ) {
                Collections.sort(list);
                return list.toArray(new String[list.size()]);
            }
        }
        return null;
    }

    public String[] getRunModes() {
        return this.runModes;
    }

    /**
     * Check if this feature is active wrt the given set of active run modes.
     */
    public boolean isActive(final Set<String> activeRunModes) {
        boolean active = true;
        if ( runModes != null ) {
            for(final String mode : runModes) {
                if ( !activeRunModes.contains(mode) ) {
                    active = false;
                    break;
                }
            }
        }
        return active;
    }

    /**
     * Check whether this feature is a special one
     */
    public boolean isSpecial() {
        if ( runModes != null && runModes.length == 1 && runModes[0].startsWith(":") ) {
            return true;
        }
        return false;
    }

    /**
     * Check if this feature is tied to a single specific run mode.
     */
    public boolean isRunMode(final String mode) {
        if ( mode == null && this.runModes == null ) {
            return true;
        }
        if ( mode != null
             && this.runModes != null
             && this.runModes.length == 1
             && this.runModes[0].equals(mode) ) {
            return true;
        }
        return false;
    }

    /**
     * Get or create a start level
     */
    public SSMStartLevel getOrCreateStartLevel(final int startLevel) {
        for(final SSMStartLevel sl : this.startLevels) {
            if ( sl.getLevel() == startLevel ) {
                return sl;
            }
        }
        final SSMStartLevel sl = new SSMStartLevel(startLevel);
        this.startLevels.add(sl);
        Collections.sort(this.startLevels);
        return sl;
    }

    /**
     * Search a configuration with a pid
     */
    public SSMConfiguration getConfiguration(final String pid) {
        for(final SSMConfiguration c : this.configurations) {
            if ( pid.equals(c.getPid()) ) {
                return c;
            }
        }
        return null;
    }

    public SSMConfiguration getOrCreateConfiguration(final String pid, final String factoryPid) {
        SSMConfiguration found = null;
        for(final SSMConfiguration current : this.configurations) {
            if ( factoryPid == null ) {
                if ( current.getFactoryPid() == null && current.getPid().equals(pid) ) {
                    found = current;
                    break;
                }
            } else {
                if ( factoryPid.equals(current.getFactoryPid()) && current.getPid().equals(pid) ) {
                    found = current;
                    break;
                }
            }
        }
        if ( found == null ) {
            found = new SSMConfiguration(pid, factoryPid);
            this.configurations.add(found);
        }
        return found;
    }

    public List<SSMStartLevel> getStartLevels() {
        return this.startLevels;
    }

    public List<SSMConfiguration> getConfigurations() {
        return this.configurations;
    }

    public Map<String, String> getSettings() {
        return this.settings;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( SSMFeature o2) {
        if ( this.runModes == null ) {
            if ( o2.runModes == null ) {
                return 0;
            }
            return -1;
        }
        if ( o2.runModes == null ) {
            return 1;
        }
        return Arrays.toString(this.runModes).compareTo(Arrays.toString(o2.runModes));
    }

    @Override
    public String toString() {
        return "SSMFeature [runModes=" + Arrays.toString(runModes)
                + ", startLevels=" + startLevels
                + ", configurations=" + configurations
                + ", settings=" + settings
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
