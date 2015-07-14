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

import static org.apache.sling.provisioning.model.ModelResolveUtility.getProcessedConfiguration;
import static org.apache.sling.provisioning.model.ModelResolveUtility.replace;
import static org.apache.sling.provisioning.model.ModelResolveUtility.resolveArtifactVersion;

import java.util.Map.Entry;

import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;


class EffectiveModelProcessor extends ModelProcessor {
    
    private final ResolverOptions options;

    public EffectiveModelProcessor(ResolverOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Resolver options is null");
        }
        this.options = options;
    }

    @Override
    protected Artifact processArtifact(Artifact artifact, Feature newFeature, RunMode newRunMode) {
        final String groupId = replace(newFeature, artifact.getGroupId(), options.getVariableResolver());
        final String artifactId = replace(newFeature, artifact.getArtifactId(), options.getVariableResolver());
        final String version = replace(newFeature, artifact.getVersion(), options.getVariableResolver());
        final String classifier = replace(newFeature, artifact.getClassifier(), options.getVariableResolver());
        final String type = replace(newFeature, artifact.getType(), options.getVariableResolver());
        final String resolvedVersion = resolveArtifactVersion(groupId, artifactId, version, classifier, type,
                options.getArtifactVersionResolver());
        return new Artifact(groupId, artifactId, resolvedVersion, classifier, type);
    }

    @Override
    protected Configuration processConfiguration(Configuration config, Feature newFeature, RunMode newRunMode) {
        Configuration newConfig = new Configuration(config.getPid(), config.getFactoryPid());
        getProcessedConfiguration(newFeature, newConfig, config, options.getVariableResolver());
        return newConfig;
    }

    @Override
    protected KeyValueMap<String> processSettings(KeyValueMap<String> settings, final Feature newFeature, final RunMode newRunMode) {
        KeyValueMap<String> newSettings = new KeyValueMap<String>();
        for (final Entry<String, String> entry : settings) {
            newSettings.put(entry.getKey(), replace(newFeature, entry.getValue(),
                    new VariableResolver() {
                        @Override
                        public String resolve(final Feature feature, final String name) {
                            if ( "sling.home".equals(name) ) {
                                return "${sling.home}";
                            }
                            if ( options.getVariableResolver() != null ) {
                                return options.getVariableResolver().resolve(newFeature, name);
                            }
                            return newFeature.getVariables().get(name);
                        }
                    }));
        }
        return newSettings;
    }

}
