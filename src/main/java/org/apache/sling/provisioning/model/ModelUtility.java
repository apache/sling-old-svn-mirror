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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.cm.file.ConfigurationHandler;


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
        // features
        for(final Feature feature : additional.getFeatures()) {
            final Feature baseFeature = base.getOrCreateFeature(feature.getName());

            // variables
            baseFeature.getVariables().putAll(feature.getVariables());

            // run modes
            for(final RunMode runMode : feature.getRunModes()) {
                // check for special remove run mode
                String names[] = runMode.getNames();
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
        final boolean baseIsRaw = baseConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
        final boolean mergeIsRaw = mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
        // simplest case, both are raw
        if ( baseIsRaw && mergeIsRaw ) {
            final String cfgMode = (String)mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
            if ( cfgMode == null || ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode) ) {
                copyConfigurationProperties(baseConfig, mergeConfig);
            } else {
                final Configuration newConfig = new Configuration(baseConfig.getPid(), baseConfig.getFactoryPid());
                getProcessedConfiguration(newConfig, baseConfig);
                clearConfiguration(baseConfig);
                copyConfigurationProperties(baseConfig, newConfig);

                clearConfiguration(newConfig);
                getProcessedConfiguration(newConfig, mergeConfig);
                copyConfigurationProperties(baseConfig, newConfig);
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
                getProcessedConfiguration(newMergeConfig, mergeConfig);
                copyConfigurationProperties(baseConfig, newMergeConfig);
            }

            // base is raw, but merge is not raw
        } else {
            // merge mode is always overwrite
            clearConfiguration(baseConfig);
            copyConfigurationProperties(baseConfig, mergeConfig);
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
     * Replace all variables in the model and return a new model with the replaced values.
     * @param model The base model.
     * @param resolver Optional variable resolver.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     */
    public static Model getEffectiveModel(final Model model, final VariableResolver resolver) {
        final Model result = new Model();
        result.setLocation(model.getLocation());

        for(final Feature feature : model.getFeatures()) {
            final Feature newFeature = result.getOrCreateFeature(feature.getName());
            newFeature.setComment(feature.getComment());
            newFeature.setLocation(feature.getLocation());

            newFeature.getVariables().setComment(feature.getVariables().getComment());
            newFeature.getVariables().setLocation(feature.getVariables().getLocation());
            newFeature.getVariables().putAll(feature.getVariables());

            for(final RunMode runMode : feature.getRunModes()) {
                final RunMode newRunMode = newFeature.getOrCreateRunMode(runMode.getNames());
                newRunMode.setLocation(runMode.getLocation());

                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final ArtifactGroup newGroup = newRunMode.getOrCreateArtifactGroup(group.getStartLevel());
                    newGroup.setComment(group.getComment());
                    newGroup.setLocation(group.getLocation());

                    for(final Artifact artifact : group) {
                        final Artifact newArtifact = new Artifact(replace(feature, artifact.getGroupId(), resolver),
                                replace(feature, artifact.getArtifactId(), resolver),
                                replace(feature, artifact.getVersion(), resolver),
                                replace(feature, artifact.getClassifier(), resolver),
                                replace(feature, artifact.getType(), resolver));
                        newArtifact.setComment(artifact.getComment());
                        newArtifact.setLocation(artifact.getLocation());

                        newGroup.add(newArtifact);
                    }
                }

                newRunMode.getConfigurations().setComment(runMode.getConfigurations().getComment());
                newRunMode.getConfigurations().setLocation(runMode.getConfigurations().getLocation());
                for(final Configuration config : runMode.getConfigurations()) {
                    final Configuration newConfig = newRunMode.getOrCreateConfiguration(config.getPid(), config.getFactoryPid());

                    getProcessedConfiguration(newConfig, config);
                }

                newRunMode.getSettings().setComment(runMode.getSettings().getComment());
                newRunMode.getSettings().setLocation(runMode.getSettings().getLocation());
                for(final Map.Entry<String, String> entry : runMode.getSettings() ) {
                    newRunMode.getSettings().put(entry.getKey(), replace(feature, entry.getValue(),
                            new VariableResolver() {

                                @Override
                                public String resolve(final Feature feature, final String name) {
                                    if ( "sling.home".equals(name) ) {
                                        return "${sling.home}";
                                    }
                                    if ( resolver != null ) {
                                        return resolver.resolve(feature, name);
                                    }
                                    return feature.getVariables().get(name);
                                }
                            }));
                }
            }

        }
        return result;
    }

    /**
     * Replace properties in the string.
     *
     * @param feature The feature
     * @param v The variable name
     * @param resolver Optional resolver
     * @result The value of the variable
     * @throws IllegalArgumentException If variable can't be found.
     */
    static String replace(final Feature feature, final String v, final VariableResolver resolver) {
        if ( v == null ) {
            return null;
        }
        String msg = v;
        // check for variables
        int pos = -1;
        int start = 0;
        while ( ( pos = msg.indexOf('$', start) ) != -1 ) {
            boolean escapedVariable = (pos > 0 && msg.charAt(pos - 1) == '\\');
            if ( msg.length() > pos && msg.charAt(pos + 1) == '{' && (pos == 0 || msg.charAt(pos - 1) != '$') ) {
                final int endPos = msg.indexOf('}', pos);
                if ( endPos != -1 ) {
                    final String name = msg.substring(pos + 2, endPos);
                    final String value;
                    if (escapedVariable) {
                        value = "\\${" + name + "}";
                    } else if ( resolver != null ) {
                        value = resolver.resolve(feature, name);
                    } else {
                        value = feature.getVariables().get(name);
                    }
                    if ( value == null ) {
                        throw new IllegalArgumentException("Unknown variable: " + name);
                    }
                    int startPos = escapedVariable ? pos - 1 : pos;
                    msg = msg.substring(0, startPos) + value + msg.substring(endPos + 1);
                }
            }
            start = pos + 1;
        }
        return msg;
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

    private static void getProcessedConfiguration(final Configuration newConfig, final Configuration config) {
        newConfig.setComment(config.getComment());
        newConfig.setLocation(config.getLocation());

        // check for raw configuration
        final String rawConfig = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED);
        if ( rawConfig != null ) {
            if ( config.isSpecial() ) {
                newConfig.getProperties().put(config.getPid(), rawConfig);
            } else {
                final String format = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED_FORMAT);

                if ( ModelConstants.CFG_FORMAT_PROPERTIES.equals(format) ) {
                    // properties
                    final Properties props = new Properties();
                    try {
                        props.load(new StringReader(rawConfig));
                    } catch ( final IOException ioe) {
                        throw new IllegalArgumentException("Unable to read configuration properties.", ioe);
                    }
                    final Enumeration<Object> i = props.keys();
                    while ( i.hasMoreElements() ) {
                        final String key = (String)i.nextElement();
                        newConfig.getProperties().put(key, props.get(key));
                    }
                } else {
                    // Apache Felix CA format
                    // the raw format might have comments, we have to remove them first
                    final StringBuilder sb = new StringBuilder();
                    try {
                        final LineNumberReader lnr = new LineNumberReader(new StringReader(rawConfig));
                        String line = null;
                        while ((line = lnr.readLine()) != null ) {
                            line = line.trim();
                            if ( line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            sb.append(line);
                            sb.append('\n');
                        }
                    } catch ( final IOException ioe) {
                        throw new IllegalArgumentException("Unable to read configuration properties: " + config, ioe);
                    }

                    ByteArrayInputStream bais = null;
                    try {
                        bais = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
                        @SuppressWarnings("unchecked")
                        final Dictionary<String, Object> props = ConfigurationHandler.read(bais);
                        final Enumeration<String> i = props.keys();
                        while ( i.hasMoreElements() ) {
                            final String key = i.nextElement();
                            newConfig.getProperties().put(key, props.get(key));
                        }
                    } catch ( final IOException ioe) {
                        throw new IllegalArgumentException("Unable to read configuration properties: " + config, ioe);
                    } finally {
                        if ( bais != null ) {
                            try {
                                bais.close();
                            } catch ( final IOException ignore ) {
                                // ignore
                            }
                        }
                    }
                }
            }
        } else {
            // simply copy
            final Enumeration<String> i = config.getProperties().keys();
            while ( i.hasMoreElements() ) {
                final String key = i.nextElement();
                newConfig.getProperties().put(key, config.getProperties().get(key));
            }
        }
    }
}
