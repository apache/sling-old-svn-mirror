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


/**
 * A feature is a collection of
 * - a name
 * - a version
 * - variables
 * - run modes
 */
public class Feature
    extends Commentable
    implements Comparable<Feature> {

    /** All run modes. */
    private final List<RunMode> runModes = new ArrayList<RunMode>();

    /** The type of feature */
    private volatile String type = FeatureTypes.PLAIN;

    /** The version of the feature */
    private volatile String version;

    /** Variables. */
    private final KeyValueMap<String> variables = new KeyValueMap<String>();

    /** Feature name. */
    private final String name;

    /** Additional sections. */
    private final List<Section> additionalSections = new ArrayList<Section>();

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
     * Get the version of the feature.
     * @return The version string or {@code null}
     * @since 1.6.0
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Special feature?
     * @return true if the feature is special
     */
    public boolean isSpecial() {
        return this.name.startsWith(":");
    }

    /**
     * Get all variables
     * @return The set of variables
     */
    public KeyValueMap<String> getVariables() {
        return this.variables;
    }

    /**
     * Get all run modes.
     * @return The list of run modes.
     */
    public List<RunMode> getRunModes() {
        return this.runModes;
    }

    /**
     * Find the run mode if available
     * @param runModes The run modes or {@code null}
     * @return The feature or {@code null}.
     */
    public RunMode getRunMode(final String ... runModes) {
        final String[] sortedRunModes = RunMode.getSortedRunModesArray(runModes);
        RunMode result = null;
        for(final RunMode current : this.runModes) {
            if ( Arrays.equals(sortedRunModes, current.getNames()) ) {
                result = current;
                break;
            }
        }
        return result;
    }

    /**
     * Get or create the run mode.
     * @param names The run modes.
     * @return The run mode for the given run modes names.
     */
    public RunMode getOrCreateRunMode(final String[] names) {
        RunMode result = getRunMode(names);
        if ( result == null ) {
            result = new RunMode(names);
            this.runModes.add(result);
            Collections.sort(this.runModes);
        }
        return result;
    }

    /**
     * Get the feature type.
     * @return The feature type.
     * @since 1.4.0
     */
    public String getType() {
        return type;
    }

    /**
     * Set the version.
     * @param v The new version
     * @since 1.6.0
     */
    public void setVersion(final String v) {
        this.version = v;
    }

    /**
     * Set the feature type.
     * @param t The new type
     * @since 1.4.0
     */
    public void setType(final String t) {
        type = ( t == null ? FeatureTypes.PLAIN : t);
    }

    /**
     * Get all additional sections
     * @return The list of additional sections. It might be empty.
     * @since 1.4.0
     */
    public List<Section> getAdditionalSections() {
        return this.additionalSections;
    }

    /**
     * Get all sections with the given name.
     * @param name The section name.
     * @return The list of sections. The list might be empty.
     * @since 1.4.0
     */
    public List<Section> getAdditionalSections(final String name) {
        final List<Section> result = new ArrayList<Section>();

        for(final Section s : this.additionalSections) {
            if ( name.equals(s.getName()) ) {
                result.add(s);
            }
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

    @Override
    public String toString() {
        return "Feature [runModes=" + runModes + ", variables=" + variables
                + ", name=" + name
                + ( FeatureTypes.PLAIN.equals(this.type) ? "" : ", type=" + type )
                + ( additionalSections.isEmpty() ? "" : ", additionalSections=" + this.additionalSections)
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
