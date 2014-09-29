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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.cm.file.ConfigurationHandler;

/**
 * Utility methods
 */
public abstract class SSMUtil {

    /**
     * Optional variable resolver
     */
    public interface VariableResolver {

        /**
         * Resolve the variable.
         * An implementation might get the value of a variable from the system properties,
         * or the environment etc.
         * As a fallback, the resolver should check the variables of the model.
         * @param model The model
         * @param name The variable name
         * @return The variable value or null.
         */
        String resolve(final SSMDeliverable model, final String name);
    }

    /**
     * Replace all variables in the model and return a new model with the replaced values.
     * @param model The base model.
     * @param resolver Optional variable resolver.
     * @return The model with replaced variables.
     * @throws IllegalArgumentException If a variable can't be replaced or configuration properties can't be parsed
     */
    public static SSMDeliverable getEffectiveModel(final SSMDeliverable model, final VariableResolver resolver) {
        final SSMDeliverable result = new SSMDeliverable();
        result.setComment(model.getComment());
        result.setLocation(model.getLocation());
        result.getVariables().putAll(model.getVariables());

        for(final SSMFeature feature : model.getFeatures()) {
            final SSMFeature newFeature = result.getOrCreateFeature(feature.getRunModes());
            newFeature.setComment(feature.getComment());
            newFeature.setLocation(feature.getLocation());

            for(final SSMStartLevel startLevel : feature.getStartLevels()) {
                final SSMStartLevel newStartLevel = newFeature.getOrCreateStartLevel(startLevel.getLevel());
                newStartLevel.setComment(startLevel.getComment());
                newStartLevel.setLocation(startLevel.getLocation());

                for(final SSMArtifact artifact : startLevel.getArtifacts()) {
                    final SSMArtifact newArtifact = new SSMArtifact(replace(model, artifact.getGroupId(), resolver),
                            replace(model, artifact.getArtifactId(), resolver),
                            replace(model, artifact.getVersion(), resolver),
                            replace(model, artifact.getClassifier(), resolver),
                            replace(model, artifact.getType(), resolver));
                    newArtifact.setComment(artifact.getComment());
                    newArtifact.setLocation(artifact.getLocation());

                    newStartLevel.getArtifacts().add(newArtifact);
                }
            }

            for(final SSMConfiguration config : feature.getConfigurations()) {
                final SSMConfiguration newConfig = new SSMConfiguration(config.getPid(), config.getFactoryPid());
                newConfig.setComment(config.getComment());
                newConfig.setLocation(config.getLocation());

                // check for raw configuration
                final String rawConfig = (String)config.getProperties().get(SSMConstants.CFG_UNPROCESSED);
                if ( rawConfig != null ) {
                    final String format = (String)config.getProperties().get(SSMConstants.CFG_UNPROCESSED_FORMAT);

                    if ( SSMConstants.CFG_FORMAT_PROPERTIES.equals(format) ) {
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
                        ByteArrayInputStream bais = null;
                        try {
                            bais = new ByteArrayInputStream(rawConfig.getBytes("UTF-8"));
                            @SuppressWarnings("unchecked")
                            final Dictionary<String, Object> props = ConfigurationHandler.read(bais);
                            final Enumeration<String> i = props.keys();
                            while ( i.hasMoreElements() ) {
                                final String key = i.nextElement();
                                newConfig.getProperties().put(key, props.get(key));
                            }
                        } catch ( final IOException ioe) {
                            throw new IllegalArgumentException("Unable to read configuration properties.", ioe);
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
                } else {
                    // simply copy
                    final Enumeration<String> i = config.getProperties().keys();
                    while ( i.hasMoreElements() ) {
                        final String key = i.nextElement();
                        newConfig.getProperties().put(key, config.getProperties().get(key));
                    }
                }

                newFeature.getConfigurations().add(newConfig);
            }

            for(final Map.Entry<String, String> entry : feature.getSettings().entrySet() ) {
                newFeature.getSettings().put(entry.getKey(), replace(model, entry.getValue(), resolver));
            }
        }
        return result;
    }

    /**
     * Replace properties in the string.
     *
     * @param model The model
     * @param v The variable name
     * @param resolver Optional resolver
     * @result The value of the variable
     * @throws IllegalArgumentException
     */
    private static String replace(final SSMDeliverable model, final String v, final VariableResolver resolver) {
        if ( v == null ) {
            return null;
        }
        String msg = v;
        // check for variables
        int pos = -1;
        int start = 0;
        while ( ( pos = msg.indexOf('$', start) ) != -1 ) {
            if ( msg.length() > pos && msg.charAt(pos + 1) == '{' && (pos == 0 || msg.charAt(pos - 1) != '$') ) {
                final int endPos = msg.indexOf('}', pos);
                if ( endPos == -1 ) {
                    start = pos + 1;
                } else {
                    final String name = msg.substring(pos + 2, endPos);
                    final String value;
                    if ( resolver != null ) {
                        value = resolver.resolve(model, name);
                    } else {
                        value = model.getVariables().get(name);
                    }
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
