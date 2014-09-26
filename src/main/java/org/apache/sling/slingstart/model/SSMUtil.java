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

import java.util.Enumeration;
import java.util.Map;

/**
 * Utility methods
 */
public abstract class SSMUtil {

    /**
     * Replace all variables in the model and return a new model with the replaced values.
     * @param base The base model.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced.
     */
    public static SSMDeliverable getEffectiveModel(final SSMDeliverable base) {
        final SSMDeliverable result = new SSMDeliverable();
        result.setComment(base.getComment());
        result.setLocation(base.getLocation());
        result.getVariables().putAll(base.getVariables());

        for(final SSMFeature feature : base.getFeatures()) {
            final SSMFeature newFeature = result.getOrCreateFeature(feature.getRunModes());
            newFeature.setComment(feature.getComment());
            newFeature.setLocation(feature.getLocation());

            for(final SSMStartLevel startLevel : feature.getStartLevels()) {
                final SSMStartLevel newStartLevel = newFeature.getOrCreateStartLevel(startLevel.getLevel());
                newStartLevel.setComment(startLevel.getComment());
                newStartLevel.setLocation(startLevel.getLocation());

                for(final SSMArtifact artifact : startLevel.getArtifacts()) {
                    final SSMArtifact newArtifact = new SSMArtifact(replace(base, artifact.getGroupId()),
                            replace(base, artifact.getArtifactId()),
                            replace(base, artifact.getVersion()),
                            replace(base, artifact.getClassifier()),
                            replace(base, artifact.getType()));
                    newArtifact.setComment(artifact.getComment());
                    newArtifact.setLocation(artifact.getLocation());

                    newStartLevel.getArtifacts().add(newArtifact);
                }
            }

            for(final SSMConfiguration config : feature.getConfigurations()) {
                final SSMConfiguration newConfig = new SSMConfiguration(config.getPid(), config.getFactoryPid());
                newConfig.setComment(config.getComment());
                newConfig.setLocation(config.getLocation());

                final Enumeration<String> i = config.getProperties().keys();
                while ( i.hasMoreElements() ) {
                    final String key = i.nextElement();
                    newConfig.getProperties().put(key, config.getProperties().get(key));
                }

                newFeature.getConfigurations().add(newConfig);
            }

            for(final Map.Entry<String, String> entry : feature.getSettings().entrySet() ) {
                newFeature.getSettings().put(entry.getKey(), replace(base, entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Replace properties in the string.
     *
     * @throws IllegalArgumentException
     */
    private static String replace(final SSMDeliverable base, final String v) {
        if ( v == null ) {
            return null;
        }
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
                    final String value = base.getVariables().get(name);
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
}
