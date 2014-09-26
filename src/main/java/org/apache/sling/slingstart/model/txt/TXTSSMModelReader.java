/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.slingstart.model.txt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.apache.sling.slingstart.model.SSMTraceable;


public class TXTSSMModelReader {

    public static final String FELIX_FORMAT_SUFFIX = "FORMAT:felix.config";

    private enum MODE {
        NONE,
        VARS,
        FEATURE,
        START_LEVEL,
        CONFIGURATION,
        SETTINGS,
        ARTIFACT
    }

    /**
     * Reads the deliverable file
     * The reader is not closed.
     * @throws IOException
     */
    public static SSMDeliverable read(final Reader reader, final String location)
    throws IOException {
        final TXTSSMModelReader mr = new TXTSSMModelReader(location);
        return mr.readModel(reader);
    }

    private MODE mode = MODE.NONE;

    private final SSMDeliverable model = new SSMDeliverable();

    private SSMFeature feature = null;
    private SSMStartLevel startLevel = null;
    private SSMConfiguration config = null;
    private SSMArtifact artifact = null;

    private String comment = null;

    private StringBuilder configBuilder = null;
    private boolean configFelixFormat = false;

    private LineNumberReader lineNumberReader;

    private TXTSSMModelReader(final String location) {
        this.model.setLocation(location);
    }

    private SSMDeliverable readModel(final Reader reader)
    throws IOException {

        boolean global = true;

        lineNumberReader = new LineNumberReader(reader);
        String line;
        while ( (line = lineNumberReader.readLine()) != null ) {
            // ignore empty line
            if ( line.trim().isEmpty() ) {
                continue;
            }
            // comment?
            if ( line.startsWith("#") ) {
                checkConfig();
                mode = MODE.NONE;
                final String c = line.substring(1).trim();
                if ( comment == null ) {
                    comment = c;
                } else {
                    comment = comment + "\n" + c;
                }
                continue;
            }

            if ( global ) {
                global = false;
                model.setComment(comment);
                comment = null;
            }

            final String trimmedLine = line.trim();
            final int pos = line.indexOf(':');
            final String params = (pos != -1 ? line.substring(pos + 1).trim() : null);

            if ( trimmedLine.startsWith("feature:") ) {
                checkConfig();

                mode = MODE.FEATURE;
                feature = model.getOrCreateFeature(params.split(","));
                this.init(feature);
                startLevel = feature.getOrCreateStartLevel(0);

            } else if ( trimmedLine.startsWith("variables:") ) {
                checkConfig();

                if ( comment != null ) {
                    throw new IOException("comment not allowed for variables in line " + this.lineNumberReader.getLineNumber());
                }
                mode = MODE.VARS;

            } else if ( trimmedLine.startsWith("startLevel:") ) {
                checkConfig();

                if ( feature == null ) {
                    throw new IOException("startlevel outside of feature in line " + this.lineNumberReader.getLineNumber());
                }
                int level = (params.length() == 0 ? level = 0 : Integer.valueOf(params));
                startLevel = feature.getOrCreateStartLevel(level);
                this.init(startLevel);
                mode = MODE.START_LEVEL;

            } else if ( trimmedLine.startsWith("config:FELIX ") || trimmedLine.startsWith("config: ") ) {
                checkConfig();

                if ( feature == null ) {
                    throw new IOException("configuration outside of feature in line " + this.lineNumberReader.getLineNumber());
                }

                mode = MODE.CONFIGURATION;
                final int factoryPos = params.indexOf('-');
                if ( factoryPos == -1 ) {
                    config = new SSMConfiguration(params, null);
                } else {
                    config = new SSMConfiguration(params.substring(pos + 1), params.substring(0, pos));
                }
                this.init(config);
                feature.getConfigurations().add(config);
                configBuilder = new StringBuilder();
                configFelixFormat = trimmedLine.startsWith("config:FELIX ");

            } else if ( trimmedLine.startsWith("settings:") ) {
                checkConfig();

                if ( comment != null ) {
                    throw new IOException("comment not allowed for settings in line " + this.lineNumberReader.getLineNumber());
                }
                if ( startLevel == null ) {
                    throw new IOException("settings outside of feature/startlevel in line " + this.lineNumberReader.getLineNumber());
                }
                mode = MODE.SETTINGS;

            } else if ( trimmedLine.startsWith("artifact:") ) {
                checkConfig();

                if ( startLevel == null ) {
                    throw new IOException("artifact outside of feature/startlevel in line " + this.lineNumberReader.getLineNumber());
                }

                mode = MODE.ARTIFACT;
                try {
                    artifact = SSMArtifact.fromMvnUrl("mvn:" + params);
                } catch ( final IllegalArgumentException iae) {
                    throw new IOException(iae.getMessage() + " in line " + this.lineNumberReader.getLineNumber(), iae);
                }
                this.init(artifact);
                startLevel.getArtifacts().add(artifact);

            } else {
                switch ( mode ) {
                    case NONE:  throw new IOException("No global contents allowed in line " + this.lineNumberReader.getLineNumber());
                    case ARTIFACT : final String[] metadata = parseProperty(trimmedLine);
                                    artifact.getMetadata().put(metadata[0], metadata[1]);
                                    break;
                    case VARS : final String[] vars = parseProperty(trimmedLine);
                                model.getVariables().put(vars[0], vars[1]);
                                break;
                    case SETTINGS : final String[] settings = parseProperty(trimmedLine);
                                    feature.getSettings().put(settings[0], settings[1]);
                                    break;
                    case FEATURE: throw new IOException("No contents allowed for feature in line " + this.lineNumberReader.getLineNumber());
                    case START_LEVEL: throw new IOException("No contents allowed for feature in line " + this.lineNumberReader.getLineNumber());
                    case CONFIGURATION: configBuilder.append(trimmedLine);
                                        configBuilder.append('\n');
                                        break;
                }
            }
        }
        checkConfig();
        if ( comment != null ) {
            throw new IOException("Comment not allowed at the end of file");
        }

        return model;
    }

    private void init(final SSMTraceable traceable) {
        traceable.setComment(this.comment);
        this.comment = null;
        final String number = String.valueOf(this.lineNumberReader.getLineNumber());
        if ( model.getLocation() != null ) {
            traceable.setLocation(model.getLocation() + ":" + number);
        } else {
            traceable.setLocation(number);
        }
    }

    private void checkConfig()
    throws IOException {
        if ( config != null ) {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(configBuilder.toString().getBytes("UTF-8"));
                @SuppressWarnings("unchecked")
                final Dictionary<String, Object> props = ConfigurationHandler.read(bais);
                final Enumeration<String> e = props.keys();
                while ( e.hasMoreElements() ) {
                    final String key = e.nextElement();
                    config.getProperties().put(key, props.get(key));
                }
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
        config = null;
        configBuilder = null;
    }

    private String[] parseProperty(final String line) throws IOException {
        final int equalsPos = line.indexOf('=');
        final String key = line.substring(0, equalsPos).trim();
        final String value = line.substring(equalsPos + 1).trim();
        if (key.isEmpty() || value.isEmpty() ) {
            throw new IOException("Invalid property; " + line + " in line " + this.lineNumberReader.getLineNumber());
        }
        return new String[] {key, value};
    }
}


