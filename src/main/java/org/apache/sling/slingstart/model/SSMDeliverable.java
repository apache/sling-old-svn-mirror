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

/**
 * A deliverable is the central object.
 * It consists of a set of features and properties.
 * The variables can be used for specifying artifact versions, referencing them
 * with ${variableName}
 *
 * At least it has a "global" feature which contains artifacts that are always installed..
 */
public class SSMDeliverable extends SSMTraceable {

    /** All features. */
    private final List<SSMFeature> features = new ArrayList<SSMFeature>();

    /** Variables. */
    private final Map<String, String> variables = new HashMap<String, String>();

    /**
     * Construct a new deliverable.
     */
    public SSMDeliverable() {
        this.features.add(new SSMFeature(null)); // global features
    }

    /**
     * Find the feature if available
     * @param runModes
     * @return The feature or null.
     */
    private SSMFeature findFeature(final String[] runModes) {
        final String[] sortedRunModes = SSMFeature.getSortedRunModesArray(runModes);
        SSMFeature result = null;
        for(final SSMFeature current : this.features) {
            if ( Arrays.equals(sortedRunModes, current.getRunModes()) ) {
                result = current;
                break;
            }
        }
        return result;
    }

    /**
     * Get the feature if available
     * @param runmode The single run mode.
     * @return The feature or null
     */
    public SSMFeature getRunMode(final String runMode) {
       return findFeature(new String[] {runMode});
    }

    /**
     * Get or create the feature.
     * @param runModes The run modes.
     * @return The feature for the given run modes.
     */
    public SSMFeature getOrCreateFeature(final String[] runModes) {
        SSMFeature result = findFeature(runModes);
        if ( result == null ) {
            result = new SSMFeature(runModes);
            this.features.add(result);
            Collections.sort(this.features);
        }
        return result;
    }

    /**
     * Return all features.
     * The returned list is modifiable and directly modifies the model.
     * @return The list of features.
     */
    public List<SSMFeature> getFeatures() {
        return this.features;
    }

    /**
     * Get all variables
     * @return The set of variables
     */
    public Map<String, String> getVariables() {
        return this.variables;
    }

    @Override
    public String toString() {
        return "SSMDeliverable [features=" + features
                + ", variables=" + variables
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
