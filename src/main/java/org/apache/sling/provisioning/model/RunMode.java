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
package org.apache.sling.provisioning.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class RunMode
    extends Traceable
    implements Comparable<RunMode> {

    private final String[] runModes;

    private final List<ArtifactGroup> groups = new ArrayList<ArtifactGroup>();

    private final ItemList<Configuration> configurations = new ItemList<Configuration>();

    private final KeyValueMap<String> settings = new KeyValueMap<String>();

    public RunMode(final String[] runModes) {
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
     * Find the artifact group.
     */
    public ArtifactGroup findArtifactGroup(final int startLevel) {
        for(final ArtifactGroup g : this.groups) {
            if ( g.getLevel() == startLevel ) {
                return g;
            }
        }
        return null;
    }

    /**
     * Get or create an artifact group
     */
    public ArtifactGroup getOrCreateArtifactGroup(final int startLevel) {
        ArtifactGroup result = this.findArtifactGroup(startLevel);
        if ( result == null ) {
            result = new ArtifactGroup(startLevel);
            this.groups.add(result);
            Collections.sort(this.groups);
        }
        return result;
    }

    /**
     * Search a configuration with a pid
     */
    public Configuration getConfiguration(final String pid) {
        for(final Configuration c : this.configurations) {
            if ( pid.equals(c.getPid()) ) {
                return c;
            }
        }
        return null;
    }

    public Configuration getConfiguration(final String pid, final String factoryPid) {
        Configuration found = null;
        for(final Configuration current : this.configurations) {
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
        return found;
    }

    public Configuration getOrCreateConfiguration(final String pid, final String factoryPid) {
        Configuration found = getConfiguration(pid, factoryPid);
        if ( found == null ) {
            found = new Configuration(pid, factoryPid);
            this.configurations.add(found);
        }
        return found;
    }

    public List<ArtifactGroup> getArtifactGroups() {
        return this.groups;
    }

    public ItemList<Configuration> getConfigurations() {
        return this.configurations;
    }

    public KeyValueMap<String> getSettings() {
        return this.settings;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( RunMode o2) {
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
        return "RunMode [runModes=" + Arrays.toString(runModes) + ", groups="
                + groups + ", configurations=" + configurations + ", settings="
                + settings
                + "]";
    }

}
