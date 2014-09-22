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
import java.util.Comparator;
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
public class SSMFeature {

    public static final String RUN_MODE_BASE = ":base";

    public static final String RUN_MODE_BOOT = ":boot";

    public static final String RUN_MODE_WEBAPP = ":webapp";

    public static final String RUN_MODE_STANDALONE = ":standalone";

    public String[] runModes;

    public final List<SSMStartLevel> startLevels = new ArrayList<SSMStartLevel>();

    public final List<SSMConfiguration> configurations = new ArrayList<SSMConfiguration>();

    public SSMSettings settings;

    /**
     * validates the object and throws an IllegalStateException
     *
     * @throws IllegalStateException
     */
    public void validate() {
        if ( this.runModes != null ) {
            boolean hasSpecial = false;
            final List<String> modes = new ArrayList<String>();
            for(String m : this.runModes) {
                if ( m != null ) m = m.trim();
                if ( m != null && !m.isEmpty()) {
                    modes.add(m);
                    if ( m.startsWith(":") ) {
                        if ( hasSpecial ) {
                            throw new IllegalStateException("Invalid modes " + Arrays.toString(this.runModes));
                        }
                        hasSpecial = true;
                    }
                }
            }
            if ( modes.size() == 0 ) {
                this.runModes = null;
            } else {
                this.runModes = modes.toArray(new String[modes.size()]);
            }
        }
        for(final SSMStartLevel sl : this.startLevels) {
            sl.validate();
        }
        for(final SSMConfiguration c : this.configurations) {
            c.validate();
        }
        if( settings != null ) {
            if (!this.isSpecial() ) {
                throw new IllegalStateException("Settings not allowed for custom run modes");
            }
            settings.validate();
        }
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
            if ( sl.level == startLevel ) {
                return sl;
            }
        }
        final SSMStartLevel sl = new SSMStartLevel();
        sl.level = startLevel;
        this.startLevels.add(sl);
        Collections.sort(this.startLevels, new Comparator<SSMStartLevel>() {

            @Override
            public int compare(SSMStartLevel o1, SSMStartLevel o2) {
                if ( o1.level < o2.level ) {
                    return -1;
                } else if ( o1.level > o2.level ) {
                    return 1;
                }
                return 0;
            }
        });
        return sl;
    }

    /**
     * Merge another feature with this one.
     */
    public void merge(final SSMFeature mode) {
        for(final SSMStartLevel sl : mode.startLevels) {
            // search for duplicates in other start levels
            for(final SSMArtifact artifact : sl.artifacts) {
                for(final SSMStartLevel mySL : this.startLevels) {
                    if ( mySL.level == sl.level ) {
                        continue;
                    }
                    final SSMArtifact myArtifact = mySL.search(artifact);
                    if ( myArtifact != null ) {
                        mySL.artifacts.remove(myArtifact);
                    }
                }
            }

            final SSMStartLevel mergeSL = this.getOrCreateStartLevel(sl.level);
            mergeSL.merge(sl);
        }
        for(final SSMConfiguration config : mode.configurations) {
            SSMConfiguration found = null;
            for(final SSMConfiguration current : this.configurations) {
                if ( config.factoryPid == null ) {
                    if ( current.factoryPid == null && current.pid.equals(config.pid) ) {
                        found = current;
                        break;
                    }
                } else {
                    if ( config.factoryPid.equals(current.factoryPid) && current.pid.equals(config.pid) ) {
                        found = current;
                        break;
                    }
                }
            }
            if ( found != null ) {
                found.properties = config.properties;
            } else {
                this.configurations.add(config);
            }
        }
        if ( this.settings == null && mode.settings != null ) {
            this.settings = new SSMSettings();
        }
        if ( mode.settings != null ) {
            this.settings.merge(mode.settings);
        }
    }

    /**
     * Search a configuration with a pid
     */
    public SSMConfiguration getConfiguration(final String pid) {
        for(final SSMConfiguration c : this.configurations) {
            if ( pid.equals(c.pid) ) {
                return c;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SSMFeature [runModes=" + Arrays.toString(runModes)
                + ", startLevels=" + startLevels + ", configurations="
                + configurations + ", settings=" + settings + "]";
    }
}
