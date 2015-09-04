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
 * A run mode is a collection of
 * - artifacts (through start levels)
 * - configurations
 * - settings
 *
 * Only if all run modes are active, this run mode is active.
 * In addition to custom, user defined run modes, special run modes exists.
 * A special run mode name starts with a colon.
 */
public class RunMode
    extends Traceable
    implements Comparable<RunMode> {

    /** The array of run mode names. */
    private final String[] names;

    /** The artifact groups. */
    private final List<ArtifactGroup> groups = new ArrayList<ArtifactGroup>();

    /** The configurations. */
    private final ItemList<Configuration> configurations = new ItemList<Configuration>();

    /** The settings. */
    private final KeyValueMap<String> settings = new KeyValueMap<String>();

    /**
     * Create a new run mode
     * @param names The run mode names
     */
    public RunMode(final String[] names) {
        this.names = getSortedRunModesArray(names);
    }

    /**
     * Get an alphabetical sorted array of the run mode names.
     * @param names The run mode names
     * @return The sorted run mode names
     */
    public static String[] getSortedRunModesArray(final String[] names) {
        // sort run modes
        if ( names != null ) {
            final List<String> list = new ArrayList<String>();
            for(final String m : names) {
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

    /**
     * Return the run mode names.
     * @return The array of run mode names or {@code null}.
     */
    public String[] getNames() {
        return this.names;
    }

    /**
     * Check if this run mode is active wrt the given set of active run modes.
     * @param activeRunModes The set of active run modes.
     * @return {@code true} if the run mode is active.
     */
    public boolean isActive(final Set<String> activeRunModes) {
        boolean active = true;
        if ( names != null ) {
            for(final String mode : names) {
                if ( !activeRunModes.contains(mode) ) {
                    active = false;
                    break;
                }
            }
        }
        return active;
    }

    /**
     * Check whether this run mode is a special one
     * @return {@code true} if it is special
     */
    public boolean isSpecial() {
        if ( names != null && names.length == 1 && names[0].startsWith(":") ) {
            return true;
        }
        return false;
    }

    /**
     * Check if this run mode is tied to a single specific run mode name.
     * @param mode The name of the run mode
     * @return {@code true} if this run mode is tied to exactly the single one.
     */
    public boolean isRunMode(final String mode) {
        if ( mode == null && this.names == null ) {
            return true;
        }
        if ( mode != null
             && this.names != null
             && this.names.length == 1
             && this.names[0].equals(mode) ) {
            return true;
        }
        return false;
    }

    /**
     * Find the artifact group.
     * @param startLevel the start level
     * @return The artifact group for that level or {@code null}.
     */
    public ArtifactGroup getArtifactGroup(final int startLevel) {
        for(final ArtifactGroup g : this.groups) {
            if ( g.getStartLevel() == startLevel ) {
                return g;
            }
        }
        return null;
    }

    /**
     * Get or create an artifact group
     * @param startLevel The start level
     * @return The artifact group.
     */
    public ArtifactGroup getOrCreateArtifactGroup(final int startLevel) {
        ArtifactGroup result = this.getArtifactGroup(startLevel);
        if ( result == null ) {
            result = new ArtifactGroup(startLevel);
            this.groups.add(result);
            Collections.sort(this.groups);
        }
        return result;
    }

    /**
     * Search a configuration with a pid
     * @param pid The configuration pid
     * @return The configuration or {@code null}
     */
    public Configuration getConfiguration(final String pid) {
        for(final Configuration c : this.configurations) {
            if ( pid.equals(c.getPid()) ) {
                return c;
            }
        }
        return null;
    }

    /**
     * Search a configuration with pid and factory pid
     * @param pid The pid
     * @param factoryPid The optional factory pid
     * @return The configuration or {@code null}.
     */
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

    /**
     * Get or create the configuration
     * @param pid The pid
     * @param factoryPid The optional factory pid
     * @return The configuration
     */
    public Configuration getOrCreateConfiguration(final String pid, final String factoryPid) {
        Configuration found = getConfiguration(pid, factoryPid);
        if ( found == null ) {
            found = new Configuration(pid, factoryPid);
            this.configurations.add(found);
            Collections.sort(this.configurations.items);
        }
        return found;
    }

    /**
     * Get all artifact groups
     * @return List of artifact groups
     */
    public List<ArtifactGroup> getArtifactGroups() {
        return this.groups;
    }

    /**
     * Get all configurations
     * @return List of configurations
     */
    public ItemList<Configuration> getConfigurations() {
        return this.configurations;
    }

    /**
     * Get the settings
     * @return Map with the settings.
     */
    public KeyValueMap<String> getSettings() {
        return this.settings;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( RunMode o2) {
        if ( this.names == null ) {
            if ( o2.names == null ) {
                return 0;
            }
            return -1;
        }
        if ( o2.names == null ) {
            return 1;
        }
        return Arrays.toString(this.names).compareTo(Arrays.toString(o2.names));
    }

    @Override
    public String toString() {
        return "RunMode [names=" + Arrays.toString(names) + ", groups="
                + groups + ", configurations=" + configurations + ", settings="
                + settings
                + "]";
    }

}
