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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A feature is a collection of
 * - variables
 * - run modes
 */
public class Feature
    extends Traceable
    implements Comparable<Feature> {

    /** All run modes. */
    private final List<RunMode> runModes = new ArrayList<RunMode>();

    /** Variables. */
    private final Map<String, String> variables = new HashMap<String, String>();

    private final String name;

    /**
     * Construct a new feature.
     * @param name The feature name
     */
    public Feature(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the feature.
     * @return The name or {@code null} for an anonymous feature.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get all variables
     * @return The set of variables
     */
    public Map<String, String> getVariables() {
        return this.variables;
    }

    public List<RunMode> getRunModes() {
        return this.runModes;
    }

    /**
     * Find the run mode if available
     * @param runModes
     * @return The feature or null.
     */
    public RunMode findRunMode(final String[] runModes) {
        final String[] sortedRunModes = RunMode.getSortedRunModesArray(runModes);
        RunMode result = null;
        for(final RunMode current : this.runModes) {
            if ( Arrays.equals(sortedRunModes, current.getRunModes()) ) {
                result = current;
                break;
            }
        }
        return result;
    }

    /**
     * Find the run mode if available
     * @param runModes
     * @return The feature or null.
     */
    public RunMode findRunMode(final String runMode) {
        return this.findRunMode(new String[] {runMode});
    }

    /**
     * Get or create the run mode.
     * @param runModes The run modes.
     * @return The feature for the given run modes.
     */
    public RunMode getOrCreateFeature(final String[] runModes) {
        RunMode result = findRunMode(runModes);
        if ( result == null ) {
            result = new RunMode(runModes);
            this.runModes.add(result);
            Collections.sort(this.runModes);
        }
        return result;
    }

    @Override
    public int compareTo(final Feature o) {
        if ( this.name == null ) {
            if ( o.name == null ) {
                return 0;
            }
            return -1;
        }
        if ( o.name == null ) {
            return 1;
        }
        return this.name.compareTo(o.name);
    }

}
