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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Validate a complete model.
 */
public class SSMValidator {

    /**
     * Validates the model.
     * @param model
     * @return
     */
    public Map<SSMTraceable, String> validate(final SSMDeliverable model) {
        final Map<SSMTraceable, String> errors = new HashMap<SSMTraceable, String>();

        for(final SSMFeature feature : model.getFeatures() ) {
            final String[] rm = feature.getRunModes();
            if ( rm != null ) {
                boolean hasSpecial = false;
                for(final String m : rm) {
                    if ( m.startsWith(":") ) {
                        if ( hasSpecial ) {
                            errors.put(feature, "Invalid modes " + Arrays.toString(rm));
                            break;
                        }
                        hasSpecial = true;
                    }
                }
            }
            for(final SSMStartLevel sl : feature.getStartLevels()) {
                if ( sl.getLevel() < 0 ) {
                    errors.put(sl, "Invalid start level " + sl.getLevel());
                }
                for(final SSMArtifact a : sl.getArtifacts()) {
                    String error = null;
                    if ( a.getGroupId() == null || a.getGroupId().isEmpty() ) {
                        error = "groupId missing";
                    }
                    if ( a.getArtifactId() == null || a.getArtifactId().isEmpty() ) {
                        error = (error != null ? error + ", " : "") + "artifactId missing";
                    }
                    if ( a.getVersion() == null || a.getVersion().isEmpty() ) {
                        error = (error != null ? error + ", " : "") + "version missing";
                    }
                    if ( a.getType() == null || a.getType().isEmpty() ) {
                        error = (error != null ? error + ", " : "") + "type missing";
                    }
                    if (error != null) {
                        errors.put(a, error);
                    }
                }
            }
            for(final SSMConfiguration c : feature.getConfigurations()) {
                String error = null;
                if ( c.getPid() == null || c.getPid().isEmpty() ) {
                    error = "pid missing";
                }
                if ( c.isSpecial() && c.getFactoryPid() != null ) {
                    error = (error != null ? error + ", " : "") + "factory pid not allowed for special configuration";
                }
                if ( c.getProperties().isEmpty() ) {
                    error = (error != null ? error + ", " : "") + "configuration properties missing";
                }
                if (error != null) {
                    errors.put(c, error);
                }
            }
        }
        if ( errors.size() == 0 ) {
            return null;
        }
        return errors;
    }
}
