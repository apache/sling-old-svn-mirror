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

import static org.apache.sling.provisioning.model.ModelResolveUtility.getProcessedConfiguration;
import static org.apache.sling.provisioning.model.ModelResolveUtility.resolveArtifactVersion;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Merge two models
 */
public abstract class ModelUtility {

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     */
    public static void merge(final Model base, final Model additional) {
        merge(base, additional, true);
    }

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     * @param handleRemove Handle special remove run mode
     * @since 1.2
     */
    public static void merge(final Model base, final Model additional, final boolean handleRemove) {
        // features
        for(final Feature feature : additional.getFeatures()) {
            final Feature baseFeature = base.getOrCreateFeature(feature.getName());
            baseFeature.setType(feature.getType());

            // variables
            baseFeature.getVariables().putAll(feature.getVariables());

            // run modes
            for(final RunMode runMode : feature.getRunModes()) {
                // check for special remove run mode
                String names[] = runMode.getNames();
                if ( handleRemove ) {
                    if ( names != null ) {
                        int removeIndex = -1;
                        int index = 0;
                        for(final String name : names) {
                            if ( name.equals(ModelConstants.RUN_MODE_REMOVE) ) {
                                removeIndex = index;
                                break;
                            }
                            index++;
                        }
                        if ( removeIndex != -1 ) {
                            String[] newNames = null;
                            if ( names.length > 1 ) {
                                newNames = new String[names.length - 1];
                                index = 0;
                                for(final String name : names) {
                                    if ( !name.equals(ModelConstants.RUN_MODE_REMOVE) ) {
                                        newNames[index++] = name;
                                    }
                                }
                            }
                            names = newNames;
                            final RunMode baseRunMode = baseFeature.getRunMode(names);
                            if ( baseRunMode != null ) {

                                // artifact groups
                                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                                    for(final Artifact artifact : group) {
                                        for(final ArtifactGroup searchGroup : baseRunMode.getArtifactGroups()) {
                                            final Artifact found = searchGroup.search(artifact);
                                            if ( found != null ) {
                                                searchGroup.remove(found);
                                            }
                                        }
                                    }
                                }

                                // configurations
                                for(final Configuration config : runMode.getConfigurations()) {
                                    final Configuration found = baseRunMode.getConfiguration(config.getPid(), config.getFactoryPid());
                                    if ( found != null ) {
                                        baseRunMode.getConfigurations().remove(found);
                                    }
                                }

                                // settings
                                for(final Map.Entry<String, String> entry : runMode.getSettings() ) {
                                    baseRunMode.getSettings().remove(entry.getKey());
                                }
                            }
                            continue;
                        }
                    }
                }
                final RunMode baseRunMode = baseFeature.getOrCreateRunMode(names);

                // artifact groups
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final ArtifactGroup baseGroup = baseRunMode.getOrCreateArtifactGroup(group.getStartLevel());

                    for(final Artifact artifact : group) {
                        for(final ArtifactGroup searchGroup : baseRunMode.getArtifactGroups()) {
                            final Artifact found = searchGroup.search(artifact);
                            if ( found != null ) {
                                searchGroup.remove(found);
                            }
                        }
                        baseGroup.add(artifact);
                    }
                }

                // configurations
                for(final Configuration config : runMode.getConfigurations()) {
                    final Configuration found = baseRunMode.getOrCreateConfiguration(config.getPid(), config.getFactoryPid());

                    mergeConfiguration(found, config);
                }

                // settings
                for(final Map.Entry<String, String> entry : runMode.getSettings() ) {
                    baseRunMode.getSettings().put(entry.getKey(), entry.getValue());
                }
            }

        }
    }

    /**
     * Merge two configurations
     * @param baseConfig The base configuration.
     * @param mergeConfig The merge configuration.
     */
    private static void mergeConfiguration(final Configuration baseConfig, final Configuration mergeConfig) {
        // check for merge mode
        final boolean isNew = baseConfig.getProperties().isEmpty();
        if ( isNew ) {
            copyConfigurationProperties(baseConfig, mergeConfig);
            final Object mode = mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
            if ( mode != null ) {
                baseConfig.getProperties().put(ModelConstants.CFG_UNPROCESSED_MODE, mode);
            }
        } else {
            final boolean baseIsRaw = baseConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
            final boolean mergeIsRaw = mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
            // simplest case, both are raw
            if ( baseIsRaw && mergeIsRaw ) {
                final String cfgMode = (String)mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
                if ( cfgMode == null || ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode) ) {
                    copyConfigurationProperties(baseConfig, mergeConfig);
                } else {
                    final Configuration newConfig = new Configuration(baseConfig.getPid(), baseConfig.getFactoryPid());
                    getProcessedConfiguration(null, newConfig, baseConfig, false, null);
                    clearConfiguration(baseConfig);
                    copyConfigurationProperties(baseConfig, newConfig);

                    clearConfiguration(newConfig);
                    getProcessedConfiguration(null, newConfig, mergeConfig, false, null);

                    if ( baseConfig.isSpecial() ) {
                        final String baseValue = baseConfig.getProperties().get(baseConfig.getPid()).toString();
                        final String mergeValue = newConfig.getProperties().get(baseConfig.getPid()).toString();
                        baseConfig.getProperties().put(baseConfig.getPid(), baseValue + "\n" + mergeValue);
                    } else {
                        copyConfigurationProperties(baseConfig, newConfig);
                    }
                }

            // another simple case, both are not raw
            } else if ( !baseIsRaw && !mergeIsRaw ) {
                // merge mode is always overwrite
                clearConfiguration(baseConfig);
                copyConfigurationProperties(baseConfig, mergeConfig);

            // base is not raw but merge is
            } else if ( !baseIsRaw && mergeIsRaw ) {
                final String cfgMode = (String)mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
                if ( cfgMode == null || ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode) ) {
                    clearConfiguration(baseConfig);
                    copyConfigurationProperties(baseConfig, mergeConfig);
                } else {
                    final Configuration newMergeConfig = new Configuration(mergeConfig.getPid(), mergeConfig.getFactoryPid());
                    getProcessedConfiguration(null, newMergeConfig, mergeConfig, false, null);

                    if ( baseConfig.isSpecial() ) {
                        final String baseValue = baseConfig.getProperties().get(baseConfig.getPid()).toString();
                        final String mergeValue = newMergeConfig.getProperties().get(baseConfig.getPid()).toString();
                        baseConfig.getProperties().put(baseConfig.getPid(), baseValue + "\n" + mergeValue);
                    } else {
                        copyConfigurationProperties(baseConfig, newMergeConfig);
                    }
                }

                // base is raw, but merge is not raw
            } else {
                // merge mode is always overwrite
                clearConfiguration(baseConfig);
                copyConfigurationProperties(baseConfig, mergeConfig);
            }
        }
    }

    private static void clearConfiguration(final Configuration cfg) {
        final Set<String> keys = new HashSet<String>();
        final Enumeration<String> e = cfg.getProperties().keys();
        while ( e.hasMoreElements() ) {
            keys.add(e.nextElement());
        }

        for(final String key : keys) {
            cfg.getProperties().remove(key);
        }
    }

    private static void copyConfigurationProperties(final Configuration baseConfig, final Configuration mergeConfig) {
        final Enumeration<String> e = mergeConfig.getProperties().keys();
        while ( e.hasMoreElements() ) {
            final String key = e.nextElement();
            if ( !key.equals(ModelConstants.CFG_UNPROCESSED_MODE) ) {
                baseConfig.getProperties().put(key, mergeConfig.getProperties().get(key));
            }
        }
    }

    /**
     * Optional variable resolver
     */
    public interface VariableResolver {

        /**
         * Resolve the variable.
         * An implementation might get the value of a variable from the system properties,
         * or the environment etc.
         * As a fallback, the resolver should check the variables of the feature.
         * @param feature The feature
         * @param name The variable name
         * @return The variable value or null.
         */
        String resolve(final Feature feature, final String name);
    }

    /**
     * Optional artifact dependency version resolver
     */
    public interface ArtifactVersionResolver {

        /**
         * Setting a version for an artifact dependency in a Sling Provisioning file is optional.
         * By default an artifact without a defined version gets "LATEST" as version.
         * By defining an DependencyVersionResolver it is possible to plugin in an external dependency resolver
         * which decides which version to use if no version is given in the provisioning file.
         * If an exact version is given in the provisioning file this is always used.
         * @param artifact Artifact without version (version is set to LATEST)
         * @return New version, or null if the version should not be changed
         */
        String resolve(final Artifact artifact);
    }
    
    /**
     * Parameter builder class for {@link ModelUtility#getEffectiveModel(Model, ResolverOptions)} method.
     */
    public static final class ResolverOptions {
        
        private VariableResolver variableResolver;
        private ArtifactVersionResolver artifactVersionResolver;
        
        public VariableResolver getVariableResolver() {
            return variableResolver;
        }
        
        public ResolverOptions variableResolver(VariableResolver variableResolver) {
            this.variableResolver = variableResolver;
            return this;
        }
        
        public ArtifactVersionResolver getArtifactVersionResolver() {
            return artifactVersionResolver;
        }
        
        public ResolverOptions artifactVersionResolver(ArtifactVersionResolver dependencyVersionResolver) {
            this.artifactVersionResolver = dependencyVersionResolver;
            return this;
        }
        
    }

    /**
     * Replace all variables in the model and return a new model with the replaced values.
     * @param model The base model.
     * @param resolver Optional variable resolver.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     * @deprecated Use {@link #getEffectiveModel(Model)} or {@link #getEffectiveModel(Model, ResolverOptions)} instead
     */
    @Deprecated
    public static Model getEffectiveModel(final Model model, final VariableResolver resolver) {
        return getEffectiveModel(model, new ResolverOptions().variableResolver(resolver));
    }
    
    /**
     * Replace all variables in the model and return a new model with the replaced values.
     * @param model The base model.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     * @since 1.3
     */
    public static Model getEffectiveModel(final Model model) {
        return getEffectiveModel(model, new ResolverOptions());
    }
    
    /**
     * Replace all variables in the model and return a new model with the replaced values.
     * @param model The base model.
     * @param options Resolver options.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     * @since 1.3
     */
    public static Model getEffectiveModel(final Model model, final ResolverOptions options) {
        ModelProcessor processor = new EffectiveModelProcessor(options);
        return processor.process(model);
    }

    /**
     * Validates the model.
     * @param model The model to validate
     * @return A map with errors or {@code null}.
     */
    public static Map<Traceable, String> validate(final Model model) {
        final Map<Traceable, String> errors = new HashMap<Traceable, String>();

        for(final Feature feature : model.getFeatures() ) {
            // validate feature
            if ( feature.getName() == null || feature.getName().isEmpty() ) {
                errors.put(feature, "Name is required for a feature.");
            }
            for(final RunMode runMode : feature.getRunModes()) {
                final String[] rm = runMode.getNames();
                if ( rm != null ) {
                    boolean hasSpecial = false;
                    for(final String m : rm) {
                        if ( m.startsWith(":") ) {
                            if ( hasSpecial ) {
                                errors.put(runMode, "Invalid modes " + Arrays.toString(rm));
                                break;
                            }
                            hasSpecial = true;
                        }
                    }
                }

                for(final ArtifactGroup sl : runMode.getArtifactGroups()) {
                    if ( sl.getStartLevel() < 0 ) {
                        errors.put(sl, "Invalid start level " + sl.getStartLevel());
                    }
                    for(final Artifact a : sl) {
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

                for(final Configuration c : runMode.getConfigurations()) {
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
        }
        if ( errors.size() == 0 ) {
            return null;
        }
        return errors;
    }
    
    /**
     * Applies a set of variables to the given model.
     * All variables that are referenced anywhere within the model are detected and passed to the given variable resolver.
     * The variable resolver may look up variables on it's own, or fallback to the variables already defined for the feature.
     * All resolved variable values are collected and put to the "variables" section of the resulting model.
     * @param model Original model
     * @param resolver Variable resolver
     * @return Model with updated "variables" section.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     * @since 1.3
     */
    public static Model applyVariables(final Model model, final VariableResolver resolver) {
        
        // define delegating resolver that collects all variable names and value per feature
        final Map<String,Map<String,String>> collectedVars = new HashMap<String, Map<String,String>>();
        VariableResolver variableCollector = new VariableResolver() {
            @Override
            public String resolve(Feature feature, String name) {
                String value = resolver.resolve(feature, name);
                if (value != null) {
                    Map<String,String> featureVars = collectedVars.get(feature.getName());
                    if (featureVars == null) {
                        featureVars = new HashMap<String, String>();
                        collectedVars.put(feature.getName(), featureVars);
                    }
                    featureVars.put(name, value);
                }
                return value;
            }
        };
        
        // use effective model processor to collect variables, but drop the resulting model
        new EffectiveModelProcessor(new ResolverOptions().variableResolver(variableCollector)).process(model);
        
        // define a processor that updates the "variables" sections in the features
        ModelProcessor variablesUpdater = new ModelProcessor() {
            @Override
            protected KeyValueMap<String> processVariables(KeyValueMap<String> variables, Feature newFeature) {
                KeyValueMap<String> newVariables = new KeyValueMap<String>();
                Map<String,String> featureVars = collectedVars.get(newFeature.getName());
                if (featureVars != null) {
                    for (Map.Entry<String, String> entry : featureVars.entrySet()) {
                        newVariables.put(entry.getKey(), entry.getValue());
                    }
                }
                return newVariables;
            }
        };
        
        // return model with replaced "variables" sections
        return variablesUpdater.process(model);
    }

    /**
     * Resolves artifact versions that are no set explicitly in the provisioning file via the given resolver (version = "LATEST").
     * If the resolver does not resolve to a version "LATEST" is left in the model.
     * The resolver may decide to raise an IllegalArgumentException in this case if unresolved dependencies are no allowed.
     * @param model Original model
     * @param resolver Artifact version resolver
     * @return Model with updated artifact versions
     * @throws IllegalArgumentException If the provider does not allow unresolved version and a version could not be resolved
     * @since 1.3
     */
    public static Model applyArtifactVersions(final Model model, final ArtifactVersionResolver resolver) {
        
        // define a processor that updates the versions of artifacts
        ModelProcessor versionUpdater = new ModelProcessor() {
            @Override
            protected Artifact processArtifact(Artifact artifact, Feature newFeature, RunMode newRunMode) {
                String newVersion = resolveArtifactVersion(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        artifact.getClassifier(),
                        artifact.getType(),
                        resolver);
                return new Artifact(artifact.getGroupId(),
                        artifact.getArtifactId(),
                        newVersion,
                        artifact.getClassifier(),
                        artifact.getType());
            }
        };
        
        // return model with updated version artifacts
        return versionUpdater.process(model);
    }

}
