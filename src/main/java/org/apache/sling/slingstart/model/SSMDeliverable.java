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

    private final List<SSMFeature> features = new ArrayList<SSMFeature>();

    private final Map<String, String> variables = new HashMap<String, String>();

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
     * @return The feature or null
     */
    public SSMFeature getRunMode(final String runMode) {
       return findFeature(new String[] {runMode});
    }

    /**
     * Get or create the feature.
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

    public List<SSMFeature> getFeatures() {
        return this.features;
    }

    /**
     * Replace properties in the string.
     *
     * @throws IllegalArgumentException
     */
    public String getValue(final String v) {
        String msg = v;
        // check for variables
        int pos = -1;
        int start = 0;
        while ( ( pos = msg.indexOf('$', start) ) != -1 ) {
            if ( msg.length() > pos && msg.charAt(pos + 1) == '{' ) {
                final int endPos = msg.indexOf('}', pos);
                if ( endPos == -1 ) {
                    start = pos + 1;
                } else {
                    final String name = msg.substring(pos + 2, endPos);
                    final String value = this.variables.get(name);
                    if ( value == null ) {
                        throw new IllegalArgumentException("Unknown variable: " + name);
                    }
                    msg = msg.substring(0, pos) + value + msg.substring(endPos + 1);
                }
            } else {
                start = pos + 1;
            }
        }
        return msg;
    }

    /**
     * Merge two deliverables.
     */
    public void merge(final SSMDeliverable other) {
        for(final SSMFeature mode : other.features) {
            final SSMFeature mergeFeature = this.getOrCreateFeature(mode.getRunModes());
            mergeFeature.merge(mode);
        }
        this.variables.putAll(other.variables);
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
