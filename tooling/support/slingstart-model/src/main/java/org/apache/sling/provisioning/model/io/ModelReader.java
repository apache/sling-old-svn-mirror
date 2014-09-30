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
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;


public class ModelReader {

    private enum CATEGORY {
        NONE(null),
        FEATURE("feature"),
        VARIABLES("variables"),
        GLOBAL("global"),
        RUN_MODE("runMode"),
        ARTIFACTS("artifacts"),
        SETTINGS("settings"),
        CONFIGURATIONS("configurations"),
        CONFIG(null);

        public final String name;

        private CATEGORY(final String n) {
            this.name = n;
        }
    }

    /**
     * Reads the model file
     * The reader is not closed.
     * @throws IOException
     */
    public static Model read(final Reader reader, final String location)
    throws IOException {
        final ModelReader mr = new ModelReader(location);
        return mr.readModel(reader);
    }

    /** Is this a single feature model? */
    private boolean isSingleFeature = false;

    private CATEGORY mode = CATEGORY.NONE;

    private final Model model = new Model();

    private Feature feature = null;
    private RunMode runMode = null;
    private ArtifactGroup artifactGroup = null;
    private Configuration config = null;

    private String comment = null;

    private StringBuilder configBuilder = null;

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
            line = line.trim();
            // ignore empty line
            if ( line.isEmpty() ) {
                checkConfig();
                continue;
            }
            // comment?
            if ( line.startsWith("#") ) {
                checkConfig();
                mode = CATEGORY.NONE;
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

            if ( line.startsWith("[") ) {
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
                    throw new IOException(exceptionPrefix + "Unknown category in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                }
                this.mode = found;
                Map<String, String> parameters = Collections.emptyMap();
                if (line.charAt(pos) != ']') {
                    final String parameterLine = line.substring(pos + 1, line.length() - 1).trim();
                    parameters = parseParameters(parameterLine);
                }

                switch ( this.mode ) {
                    case NONE : break; // this can never happen
                    case CONFIG : break; // this can never happen
                    case FEATURE : if ( this.isSingleFeature ) {
                                       throw new IOException(exceptionPrefix + "Single feature model allows only one feature.");
                                   }
                                   final String name = parameters.get("name");
                                   if ( name == null ) {
                                       throw new IOException(exceptionPrefix + "Feature name missing in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                   }
                                   if ( parameters.size() > 1 ) {
                                       throw new IOException(exceptionPrefix + "Unknown feature parameters in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                   }
                                   if ( model.findFeature(name) != null ) {
                                       throw new IOException(exceptionPrefix + "Duplicate feature in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                   }
                                   this.feature = model.getOrCreateFeature(name);
                                   this.init(this.feature);
                                   this.runMode = null;
                                   this.artifactGroup = null;
                                   break;
                    case VARIABLES : checkFeature();
                                     break;
                    case RUN_MODE : checkFeature();
                                    final String names = parameters.get("names");
                                    if ( names == null ) {
                                        throw new IOException(exceptionPrefix + "Run mode names missing in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    if ( parameters.size() > 1 ) {
                                        throw new IOException(exceptionPrefix + "Unknown run mode parameters in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    final String[] rm = names.split(",");
                                    if ( this.feature.getRunMode(rm) != null ) {
                                        throw new IOException(exceptionPrefix + "Duplicate run mode in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    this.runMode = this.feature.getOrCreateFeature(rm);
                                    this.init(this.runMode);
                                    this.artifactGroup = null;
                                    break;
                    case GLOBAL : checkFeature();
                                  if ( !parameters.isEmpty() ) {
                                      throw new IOException(exceptionPrefix + "Unknown global parameters in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                  }
                                  if ( this.feature.getRunMode(null) != null ) {
                                      throw new IOException(exceptionPrefix + "Duplicate global run mode in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                  }
                                  this.runMode = this.feature.getOrCreateFeature(null);
                                  this.init(this.runMode);
                                  this.artifactGroup = null;
                                  break;
                    case SETTINGS: checkFeature();
                                   checkRunMode();
                                   break;
                    case ARTIFACTS: checkFeature();
                                    checkRunMode();
                                    String level = parameters.get("startLevel");
                                    if ( (level == null && !parameters.isEmpty())
                                        || (level != null && parameters.size() > 1 ) ) {
                                        throw new IOException(exceptionPrefix + "Unknown artifacts parameters in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    int startLevel = 0;
                                    if ( level != null ) {
                                        try {
                                            startLevel = Integer.valueOf(level);
                                        } catch ( final NumberFormatException nfe) {
                                            throw new IOException(exceptionPrefix + "Invalid start level in line " + this.lineNumberReader.getLineNumber() + ": " + line + ":" + level);
                                        }
                                    }
                                    if ( this.runMode.findArtifactGroup(startLevel) != null ) {
                                        throw new IOException(exceptionPrefix + "Duplicate artifact group in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                    }
                                    this.artifactGroup = this.runMode.getOrCreateArtifactGroup(startLevel);
                                    this.init(this.artifactGroup);
                                    break;
                    case CONFIGURATIONS: checkFeature();
                                         checkRunMode();
                                         break;
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
                    case FEATURE:
                    case RUN_MODE:
                    case GLOBAL:
                    case ARTIFACTS : this.checkFeature();
                                     this.checkRunMode();
                                     if ( this.artifactGroup == null ) {
                                         this.artifactGroup = this.runMode.getOrCreateArtifactGroup(0);
                                     }
                                     String artifactUrl = line;
                                     Map<String, String> parameters = Collections.emptyMap();
                                     if ( line.endsWith("]") ) {
                                         final int startPos = line.indexOf("[");
                                         if ( startPos != -1 ) {
                                             artifactUrl = line.substring(0, startPos).trim();
                                             parameters = parseParameters(line.substring(startPos + 1, line.length() - 1).trim());
                                         }
                                     }
                                     try {
                                         final Artifact artifact = Artifact.fromMvnUrl("mvn:" + artifactUrl);
                                         this.init(artifact);
                                         this.artifactGroup.getArtifacts().add(artifact);
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
                                                  cfgPars = parseParameters(line.substring(startPos + 1, line.length() - 1).trim());
                                              }
                                          }
                                          String format = cfgPars.get("format");
                                          if ( (format == null && !cfgPars.isEmpty())
                                               || (format != null && cfgPars.size() > 1 ) ) {
                                              throw new IOException(exceptionPrefix + "Unknown configuration parameters in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                          }
                                          if ( format != null ) {
                                              if ( !ModelConstants.CFG_FORMAT_FELIX_CA.equals(format)
                                                  && !ModelConstants.CFG_FORMAT_PROPERTIES.equals(format) ) {
                                                  throw new IOException(exceptionPrefix + "Unknown format configuration parameter in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                                              }
                                          } else {
                                              format = ModelConstants.CFG_FORMAT_FELIX_CA;
                                          }
                                          final int factoryPos = configId.indexOf('-');
                                          if ( factoryPos == -1 ) {
                                              config = new Configuration(configId, null);
                                          } else {
                                              config = new Configuration(configId.substring(factoryPos + 1), configId.substring(0, factoryPos));
                                          }
                                          this.init(config);
                                          config.getProperties().put(ModelConstants.CFG_UNPROCESSED_FORMAT, format);
                                          runMode.getConfigurations().add(config);
                                          configBuilder = new StringBuilder();
                                          mode = CATEGORY.CONFIG;
                                          break;
                    case CONFIG : configBuilder.append(line);
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
            if ( model.getLocation() == null ) {
                throw new IOException(exceptionPrefix + "No preceding feature definition in line " + this.lineNumberReader.getLineNumber());
            }
            final int beginPos = model.getLocation().replace('\\', '/').lastIndexOf("/");
            String newName = model.getLocation().substring(beginPos + 1);
            final int endPos = newName.lastIndexOf('.');
            if ( endPos != -1 ) {
                newName = newName.substring(0, endPos);
            }
            this.isSingleFeature = true;
            feature = model.getOrCreateFeature(newName);
        }
    }

    /**
     * Check for a run mode object
     */
    private void checkRunMode() throws IOException {
        if ( runMode == null ) {
            runMode = this.feature.getOrCreateFeature(null);
        }
    }

    private void init(final Traceable traceable) {
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

    private Map<String, String> parseParameters(final String line) throws IOException {
        final Map<String, String>parameters = new HashMap<String, String>();
        final String[] keyValuePairs = line.split(" ");
        for(String kv : keyValuePairs) {
            kv = kv.trim();
            if ( !kv.isEmpty() ) {
                final int sep = kv.indexOf('=');
                if ( sep == -1 ) {
                    throw new IOException(exceptionPrefix + "Invalid parameter definition in line " + this.lineNumberReader.getLineNumber() + ": " + line);
                }
                parameters.put(kv.substring(0, sep).trim(), kv.substring(sep + 1).trim());
            }
        }
        return parameters;
    }
}


