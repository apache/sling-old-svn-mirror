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
package org.apache.sling.provisioning.model.io;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Commentable;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;

/**
 * This class offers a method to read a model using a {@code Reader} instance.
 */
public class ModelReader {

    private enum CATEGORY {
        NONE(null, null),
        FEATURE("feature", new String[] {"name", "type"}),
        VARIABLES("variables", null),
        ARTIFACTS("artifacts", new String[] {"runModes", "startLevel"}),
        SETTINGS("settings", new String[] {"runModes"}),
        CONFIGURATIONS("configurations", new String[] {"runModes"}),
        CONFIG(null, null),
        ADDITIONAL(null, null);

        public final String name;

        public final String[] parameters;

        private CATEGORY(final String n, final String[] p) {
            this.name = n;
            this.parameters = p;
        }
    }

    /**
     * Reads the model file
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader providing the model
     * @param location Optional location string identifying the source of the model.
     * @throws IOException If an error occurs
     */
    public static Model read(final Reader reader, final String location)
    throws IOException {
        final ModelReader mr = new ModelReader(location);
        return mr.readModel(reader);
    }

    private CATEGORY mode = CATEGORY.NONE;

    private final Model model = new Model();

    private Feature feature;
    private RunMode runMode;
    private ArtifactGroup artifactGroup;
    private Configuration config;

    private Section additionalSection;

    private String comment;

    private StringBuilder configBuilder;

    private LineNumberReader lineNumberReader;

    private final String exceptionPrefix;

    private ModelReader(final String location) {
        this.model.setLocation(location);
        if ( location == null ) {
            exceptionPrefix = "";
        } else {
            exceptionPrefix = location + " : ";
        }
    }

    private Model readModel(final Reader reader)
    throws IOException {

        boolean global = true;

        lineNumberReader = new LineNumberReader(reader);
        String line;
        while ( (line = lineNumberReader.readLine()) != null ) {
            // trim the line
            line = line.trim();

            // ignore empty line
            if ( line.isEmpty() ) {
                if ( this.mode == CATEGORY.ADDITIONAL ) {
                    if ( this.additionalSection.getContents() == null ) {
                        this.additionalSection.setContents(line);
                    } else {
                        this.additionalSection.setContents(this.additionalSection.getContents() + '\n' + line);
                    }
                    continue;
                }
                checkConfig();
                continue;
            }

            // comment?
            if ( line.startsWith("#") ) {
                if ( config != null ) {
                    configBuilder.append(line);
                    configBuilder.append('\n');

                    continue;
                }
                if ( this.mode == CATEGORY.ADDITIONAL ) {
                    if ( this.additionalSection.getContents() == null ) {
                        this.additionalSection.setContents(line);
                    } else {
                        this.additionalSection.setContents(this.additionalSection.getContents() + '\n' + line);
                    }
                    continue;
                }
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
                if ( !line.startsWith("[feature ") ) {
                    throw new IOException(exceptionPrefix + " Model file must start with a feature category.");
                }
            }

            if ( line.startsWith("[") ) {
                additionalSection = null;
                if ( !line.endsWith("]") ) {
                    throw new IOException(exceptionPrefix + "Illegal category definition in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                }
                int pos = 1;
                while ( line.charAt(pos) != ']' && !Character.isWhitespace(line.charAt(pos))) {
                    pos++;
                }
                final String category = line.substring(1, pos);
                CATEGORY found = null;
                for (CATEGORY c : CATEGORY.values()) {
                    if ( category.equals(c.name)) {
                        found = c;
                        break;
                    }
                }
                if ( found == null ) {
                    // additional section
                    if ( !category.startsWith(":") ) {
                        throw new IOException(exceptionPrefix + "Unknown category in line " + this.lineNumberReader.getLineNumber() + ": " + category);
                    }
                    found = CATEGORY.ADDITIONAL;
                }
                this.mode = found;
                Map<String, String> parameters = Collections.emptyMap();
                if (line.charAt(pos) != ']') {
                    final String parameterLine = line.substring(pos + 1, line.length() - 1).trim();
                    parameters = parseParameters(parameterLine, this.mode.parameters);
                }

                switch ( this.mode ) {
                    case NONE : break; // this can never happen
                    case CONFIG : break; // this can never happen
                    case FEATURE : final String name = parameters.get("name");
                                   if ( name == null ) {
                                       throw new IOException(exceptionPrefix + "Feature name missing in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                   }
                                   if ( model.getFeature(name) != null ) {
                                       throw new IOException(exceptionPrefix + "Duplicate feature in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                   }
                                   this.feature = model.getOrCreateFeature(name);
                                   this.feature.setType(parameters.get("type"));
                                   this.init(this.feature);
                                   this.runMode = null;
                                   this.artifactGroup = null;
                                   break;
                    case VARIABLES : checkFeature();
                                     this.init(this.feature.getVariables());
                                     break;
                    case SETTINGS: checkFeature();
                                   checkRunMode(parameters);
                                   this.init(this.runMode.getSettings());
                                   break;
                    case ARTIFACTS: checkFeature();
                                    checkRunMode(parameters);
                                    int startLevel = 0;
                                    String level = parameters.get("startLevel");
                                    if ( level != null ) {
                                        try {
                                            startLevel = Integer.valueOf(level);
                                        } catch ( final NumberFormatException nfe) {
                                            throw new IOException(exceptionPrefix + "Invalid start level in line " + this.lineNumberReader.getLineNumber() + ": " + line + ":" + level);
                                        }
                                    }
                                    if ( this.runMode.getArtifactGroup(startLevel) != null ) {
                                        throw new IOException(exceptionPrefix + "Duplicate artifact group in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    this.artifactGroup = this.runMode.getOrCreateArtifactGroup(startLevel);
                                    this.init(this.artifactGroup);
                                    break;
                    case CONFIGURATIONS: checkFeature();
                                         checkRunMode(parameters);
                                         this.init(this.runMode.getConfigurations());
                                         break;
                    case ADDITIONAL: checkFeature();
                                     this.runMode = null;
                                     this.artifactGroup = null;
                                     this.additionalSection = new Section(category.substring(1));
                                     this.init(this.additionalSection);
                                     this.feature.getAdditionalSections().add(this.additionalSection);
                                     this.additionalSection.getAttributes().putAll(parameters);
                }
            } else {
                switch ( this.mode ) {
                    case NONE : break;
                    case VARIABLES : final String[] vars = parseProperty(line);
                                     feature.getVariables().put(vars[0], vars[1]);
                                     break;
                    case SETTINGS : final String[] settings = parseProperty(line);
                                    runMode.getSettings().put(settings[0], settings[1]);
                                    break;
                    case FEATURE:   this.runMode = this.feature.getOrCreateRunMode(null);
                                    this.artifactGroup = this.runMode.getOrCreateArtifactGroup(0);
                                    // no break, we continue with ARTIFACT
                    case ARTIFACTS : String artifactUrl = line;
                                     Map<String, String> parameters = Collections.emptyMap();
                                     if ( line.endsWith("]") ) {
                                         final int startPos = line.indexOf("[");
                                         if ( startPos != -1 ) {
                                             artifactUrl = line.substring(0, startPos).trim();
                                             parameters = parseParameters(line.substring(startPos + 1, line.length() - 1).trim(), null);
                                         }
                                     }
                                     try {
                                         final Artifact artifact = Artifact.fromMvnUrl("mvn:" + artifactUrl);
                                         this.init(artifact);
                                         this.artifactGroup.add(artifact);
                                         artifact.getMetadata().putAll(parameters);
                                     } catch ( final IllegalArgumentException iae) {
                                         throw new IOException(exceptionPrefix + iae.getMessage() + " in line " + this.lineNumberReader.getLineNumber(), iae);
                                     }
                                     break;
                    case CONFIGURATIONS : String configId = line;
                                          Map<String, String> cfgPars = Collections.emptyMap();
                                          if ( line.endsWith("]") ) {
                                              final int startPos = line.indexOf("[");
                                              if ( startPos != -1 ) {
                                                  configId = line.substring(0, startPos).trim();
                                                  cfgPars = parseParameters(line.substring(startPos + 1, line.length() - 1).trim(), new String[] {"format", "mode"});
                                              }
                                          }
                                          String format = cfgPars.get("format");
                                          if ( format != null ) {
                                              if ( !ModelConstants.CFG_FORMAT_FELIX_CA.equals(format)
                                                  && !ModelConstants.CFG_FORMAT_PROPERTIES.equals(format) ) {
                                                  throw new IOException(exceptionPrefix + "Unknown format configuration parameter in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                              }
                                          } else {
                                              format = ModelConstants.CFG_FORMAT_FELIX_CA;
                                          }
                                          String cfgMode= cfgPars.get("mode");
                                          if ( cfgMode != null ) {
                                              if ( !ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode)
                                                      && !ModelConstants.CFG_MODE_MERGE.equals(cfgMode) ) {
                                                      throw new IOException(exceptionPrefix + "Unknown mode configuration parameter in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                                  }
                                          } else {
                                              cfgMode = ModelConstants.CFG_MODE_OVERWRITE;
                                          }
                                          final String pid;
                                          final String factoryPid;
                                          final int factoryPos = configId.indexOf('-');
                                          if ( factoryPos == -1 ) {
                                              pid = configId;
                                              factoryPid = null;
                                          } else {
                                              pid = configId.substring(factoryPos + 1);
                                              factoryPid = configId.substring(0, factoryPos);
                                          }
                                          if ( runMode.getConfiguration(pid, factoryPid) != null ) {
                                              throw new IOException(exceptionPrefix + "Duplicate configuration in line " + this.lineNumberReader.getLineNumber());
                                          }
                                          config = runMode.getOrCreateConfiguration(pid, factoryPid);
                                          this.init(config);
                                          config.getProperties().put(ModelConstants.CFG_UNPROCESSED_FORMAT, format);
                                          config.getProperties().put(ModelConstants.CFG_UNPROCESSED_MODE, cfgMode);
                                          configBuilder = new StringBuilder();
                                          mode = CATEGORY.CONFIG;
                                          break;
                    case CONFIG : configBuilder.append(line);
                                  configBuilder.append('\n');
                                  break;
                    case ADDITIONAL : if ( this.additionalSection.getContents() == null ) {
                                          this.additionalSection.setContents(line);
                                      } else {
                                          this.additionalSection.setContents(this.additionalSection.getContents() + '\n' + line);
                                      }
                                      break;
                }
            }

        }
        checkConfig();
        if ( comment != null ) {
            throw new IOException(exceptionPrefix + "Comment not allowed at the end of file");
        }

        return model;
    }

    /**
     * Check for a feature object
     */
    private void checkFeature() throws IOException {
        if ( feature == null ) {
            throw new IOException(exceptionPrefix + "No preceding feature definition in line " + this.lineNumberReader.getLineNumber());
        }
    }

    /**
     * Check for a run mode object
     */
    private void checkRunMode(final Map<String, String> parameters) throws IOException {
        String[] runModes = null;
        final String rmDef = parameters.get("runModes");
        if ( rmDef != null ) {
            runModes = rmDef.split(",");
            for(int i=0; i<runModes.length; i++) {
                runModes[i] = runModes[i].trim();
            }
        }
        runMode = this.feature.getOrCreateRunMode(runModes);
    }

    private void init(final Commentable traceable) {
        traceable.setComment(this.comment);
        this.comment = null;
        final String number = String.valueOf(this.lineNumberReader.getLineNumber());
        if ( model.getLocation() != null ) {
            traceable.setLocation(model.getLocation() + ":" + number);
        } else {
            traceable.setLocation(number);
        }
    }

    private void checkConfig() {
        if ( config != null ) {
            config.getProperties().put(ModelConstants.CFG_UNPROCESSED, configBuilder.toString());
            this.mode = CATEGORY.CONFIGURATIONS;
        }
        config = null;
        configBuilder = null;
    }

    /**
     * Parse a single property line
     * @param line The line
     * @return The key and the value
     * @throws IOException If something goes wrong
     */
    private String[] parseProperty(final String line) throws IOException {
        final int equalsPos = line.indexOf('=');
        final String key = line.substring(0, equalsPos).trim();
        final String value = line.substring(equalsPos + 1).trim();
        if (key.isEmpty() || value.isEmpty() ) {
            throw new IOException(exceptionPrefix + "Invalid property; " + line + " in line " + this.lineNumberReader.getLineNumber());
        }
        return new String[] {key, value};
    }

    private Map<String, String> parseParameters(final String line, final String[] allowedParameters) throws IOException {
        final Map<String, String>parameters = new HashMap<String, String>();
        final String[] keyValuePairs = line.split(" ");
        for(String kv : keyValuePairs) {
            kv = kv.trim();
            if ( !kv.isEmpty() ) {
                final int sep = kv.indexOf('=');
                if ( sep == -1 ) {
                    throw new IOException(exceptionPrefix + "Invalid parameter definition in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                }
                final String key = kv.substring(0, sep).trim();
                parameters.put(key, kv.substring(sep + 1).trim());

                if ( allowedParameters != null ) {
                    boolean found = false;
                    for(final String allowed : allowedParameters) {
                        if ( key.equals(allowed) ) {
                            found = true;
                            break;
                        }
                    }
                    if ( !found ) {
                        throw new IOException(exceptionPrefix + "Invalid parameter " + key + " in line " + this.lineNumberReader.getLineNumber());
                    }
                }
            }
        }
        return parameters;
    }
}


