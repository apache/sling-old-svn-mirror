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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.provisioning.model.ModelUtility.ArtifactVersionResolver;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;

/**
 * Helper methods for resolving variables and artifact versions in models.
 */
public class ModelResolveUtility {

    private ModelResolveUtility() {
    }

    /**
     * Replace properties in the string.
     *
     * @param feature The feature
     * @param v The variable name
     * @param resolver Optional resolver
     * @return The value of the variable
     * @throws IllegalArgumentException If variable can't be found.
     */
    public static String replace(final Feature feature, final String v, final VariableResolver resolver) {
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
     * Tries to resolves artifact version via {@link ArtifactVersionResolver} if no version was defined in provisioning file.
     * @param groupId Group ID
     * @param artifactId Artifact ID
     * @param version Version
     * @param classifier Classifier
     * @param type Type
     * @param artifactVersionResolver Artifact Version Resolver (may be null)
     * @return Version to use for this artifact
     */
    static String resolveArtifactVersion(final String groupId, final String artifactId, final String version,
            final String classifier, final String type, ArtifactVersionResolver artifactVersionResolver) {
        if (artifactVersionResolver != null && (version == null || "LATEST".equals(version))) {
            String newVersion = artifactVersionResolver.resolve(new Artifact(groupId, artifactId, version, classifier, type));
            if (newVersion != null) {
                return newVersion;
            }
        }
        return version;       
    }
    
    /**
     * Replaces variables in configuration.
     * @param feature Feature
     * @param newConfig New configuration with replaced variables
     * @param config Source configuration which may contain variable placeholders
     * @param replaceVariables If set to true variables are resolved in the config before processing it.
     * @param resolver Variable resolver Optional variable resolver which is used. If not given only the feature's variables are used.
     */
    static void getProcessedConfiguration(
            final Feature feature,
            final Configuration newConfig,
            final Configuration config,
            final boolean replaceVariables,
            final VariableResolver resolver) {
        newConfig.setComment(config.getComment());
        newConfig.setLocation(config.getLocation());

        // check for raw configuration
        String rawConfig = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED);
        if ( rawConfig != null ) {
            if ( replaceVariables ) {
                rawConfig = replace(feature, rawConfig, resolver);
            }
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
