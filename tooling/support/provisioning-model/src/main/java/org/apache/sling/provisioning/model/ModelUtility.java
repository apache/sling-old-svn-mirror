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

import static org.apache.sling.provisioning.model.ModelResolveUtility.resolveArtifactVersion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.provisioning.model.MergeUtility.MergeOptions;

/**
 * Model utility
 */
public abstract class ModelUtility {

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     * @deprecated Use {link {@link MergeUtility#merge(Model, Model)}
     */
    @Deprecated
    public static void merge(final Model base, final Model additional) {
        MergeUtility.merge(base, additional);
    }

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     * @param handleRemove Handle special remove run mode
     * @since 1.2
     * @deprecated Use {link {@link MergeUtility#merge(Model, Model, org.apache.sling.provisioning.model.MergeUtility.MergeOptions)}
     */
    @Deprecated
    public static void merge(final Model base, final Model additional, final boolean handleRemove) {
        final MergeOptions opts = new MergeOptions();
        opts.setHandleRemoveRunMode(handleRemove);
        MergeUtility.merge(base, additional, opts);

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
                        artifact.getType(),
                        artifact.getMetadata());
            }
        };

        // return model with updated version artifacts
        return versionUpdater.process(model);
    }

}
