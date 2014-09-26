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


/**
 * Merge two models
 */
public abstract class SSMMerger {

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     */
    public static void merge(final SSMDeliverable base, final SSMDeliverable additional) {
        // features
        for(final SSMFeature additionlFeature : additional.getFeatures()) {
            final SSMFeature baseFeature = base.getOrCreateFeature(additionlFeature.getRunModes());

            // start levels
            for(final SSMStartLevel additionalStartLevel : additionlFeature.getStartLevels()) {
                // search for duplicates in other start levels
                for(final SSMArtifact artifact : additionalStartLevel.getArtifacts()) {
                    for(final SSMStartLevel mySL : baseFeature.getStartLevels()) {
                        if ( mySL.getLevel() == additionalStartLevel.getLevel() ) {
                            continue;
                        }
                        final SSMArtifact myArtifact = mySL.search(artifact);
                        if ( myArtifact != null ) {
                            mySL.getArtifacts().remove(myArtifact);
                        }
                    }
                }

                // now merge current level
                final SSMStartLevel baseStartLevel = baseFeature.getOrCreateStartLevel(additionalStartLevel.getLevel());

                // artifacts
                for(final SSMArtifact a : additionalStartLevel.getArtifacts()) {
                    final SSMArtifact found = baseStartLevel.search(a);
                    if ( found != null ) {
                        baseStartLevel.getArtifacts().remove(found);
                    }
                    baseStartLevel.getArtifacts().add(a);
                }
            }

            // configurations
            for(final SSMConfiguration config : additionlFeature.getConfigurations()) {
                final SSMConfiguration found = baseFeature.getOrCreateConfiguration(config.getPid(), config.getFactoryPid());
                final Enumeration<String> e = config.getProperties().keys();
                while ( e.hasMoreElements() ) {
                    final String key = e.nextElement();
                    found.getProperties().put(key, config.getProperties().get(key));
                }
            }

            // settings
            baseFeature.getSettings().putAll(additionlFeature.getSettings());
        }

        // variables
        base.getVariables().putAll(additional.getVariables());
    }
}
