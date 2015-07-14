/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.provisioning.model;

import java.util.Enumeration;

/**
 * Allows to process a value. A new value is created and for each part in the model a process
 * method is called. Subclasses can overwrite those methods to inject specific behavior.
 * The processor itself does not change anything in the model. 
 */
class ModelProcessor {
    
    /**
     * Creates a copy of the model and calls a process method for each part found in the model.
     * This allows to modify the parts content (e.g. replace variables), but not to add or remove parts.
     * @param model The base model.
     * @return The processed and copied model.
     */
    public final Model process(final Model model) {
        final Model result = new Model();
        result.setLocation(model.getLocation());

        for(final Feature feature : model.getFeatures()) {
            final Feature newFeature = result.getOrCreateFeature(feature.getName());
            newFeature.setComment(feature.getComment());
            newFeature.setLocation(feature.getLocation());

            newFeature.getVariables().setComment(feature.getVariables().getComment());
            newFeature.getVariables().setLocation(feature.getVariables().getLocation());
            newFeature.getVariables().putAll(processVariables(feature.getVariables(), feature));

            for(final RunMode runMode : feature.getRunModes()) {
                final RunMode newRunMode = newFeature.getOrCreateRunMode(runMode.getNames());
                newRunMode.setLocation(runMode.getLocation());

                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final ArtifactGroup newGroup = newRunMode.getOrCreateArtifactGroup(group.getStartLevel());
                    newGroup.setComment(group.getComment());
                    newGroup.setLocation(group.getLocation());

                    for(final Artifact artifact : group) {
                        final Artifact newArtifact = processArtifact(artifact, newFeature, newRunMode);
                        newArtifact.setComment(artifact.getComment());
                        newArtifact.setLocation(artifact.getLocation());
                        newGroup.add(newArtifact);
                    }
                }

                newRunMode.getConfigurations().setComment(runMode.getConfigurations().getComment());
                newRunMode.getConfigurations().setLocation(runMode.getConfigurations().getLocation());
                for(final Configuration config : runMode.getConfigurations()) {
                    final Configuration processedConfig = processConfiguration(config, newFeature, newRunMode);
                    final Configuration newConfig = newRunMode.getOrCreateConfiguration(processedConfig.getPid(), processedConfig.getFactoryPid());
                    newConfig.setLocation(config.getLocation());
                    newConfig.setComment(config.getComment());
                    final Enumeration<String> i = processedConfig.getProperties().keys();
                    while ( i.hasMoreElements() ) {
                        final String key = i.nextElement();
                        newConfig.getProperties().put(key, processedConfig.getProperties().get(key));
                    }
                }

                newRunMode.getSettings().setComment(runMode.getSettings().getComment());
                newRunMode.getSettings().setLocation(runMode.getSettings().getLocation());
                newRunMode.getSettings().putAll(processSettings(runMode.getSettings(), newFeature, newRunMode));
            }

        }
        return result;
    }
    
    protected KeyValueMap<String> processVariables(KeyValueMap<String> variables, Feature feature) {
        return variables;
    }
    
    protected Artifact processArtifact(Artifact artifact, Feature newFeature, RunMode newRunMode) {
        return artifact;
    }
    
    protected Configuration processConfiguration(Configuration config, Feature newFeature, RunMode newRunMode) {
        return config;
    }

    protected KeyValueMap<String> processSettings(KeyValueMap<String> settings, Feature newFeature, RunMode newRunMode) {
        return settings;
    }
    
}
