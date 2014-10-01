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
import java.util.Collections;
import java.util.List;

/**
 * A model is the central object.
 * It consists of features.
 */
public class Model extends Traceable {

    /** All features. */
    private final List<Feature> features = new ArrayList<Feature>();

    /**
     * Find the feature if available
     * @param name The feature name
     * @return The feature or {@code null}.
     */
    public Feature getFeature(final String name) {
        for(final Feature f : this.features) {
            if ( name.equals(f.getName()) ) {
                return f;
            }
        }
        return null;
    }

    /**
     * Get or create the feature.
     * @param runModes The run modes.
     * @return The feature for the given run modes.
     */
    public Feature getOrCreateFeature(final String name) {
        Feature result = getFeature(name);
        if ( result == null ) {
            result = new Feature(name);
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
    public List<Feature> getFeatures() {
        return this.features;
    }

    @Override
    public String toString() {
        return "Model [features=" + features + "]";
    }
}
