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
package org.apache.sling.provisioning.model.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;

import org.apache.felix.cm.file.ConfigurationHandler;
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
 * Simple writer for the a model
 */
public class ModelWriter {

    private static void writeComment(final PrintWriter pw, final Commentable commentable)
    throws IOException {
        if ( commentable.getComment() != null ) {
            final LineNumberReader lnr = new LineNumberReader(new StringReader(commentable.getComment()));
            try {
                String line = null;
                while ( (line = lnr.readLine()) != null ) {
                    pw.print("# ");
                    pw.println(line);
                }
            } finally {
                lnr.close();
            }
        }
    }

    private static void writeRunMode(final PrintWriter pw, final RunMode runMode) {
        final String[] rm = runMode.getNames();
        if ( rm != null && rm.length > 0 ) {
            pw.print(" runModes=");
            boolean first = true;
            for(final String mode : rm) {
                if ( first ) {
                    first = false;
                } else {
                    pw.print(",");
                }
                pw.print(mode);
            }
        }
    }

    /**
     * Writes the model to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param model Model
     * @throws IOException
     */
    public static void write(final Writer writer, final Model model)
    throws IOException {
        final PrintWriter pw = new PrintWriter(writer);

        // features
        for(final Feature feature : model.getFeatures()) {
            writeComment(pw, feature);
            pw.print("[feature name=");
            pw.print(feature.getName());
            if (feature.getType() != Feature.Type.PLAIN) {
                pw.print(" type=");
                pw.print(feature.getType().getTextRepresentation());
            }
            pw.println("]");
            pw.println();

            // variables
            if ( !feature.getVariables().isEmpty() ) {
                writeComment(pw, feature.getVariables());
                pw.println("[variables]");
                for(final Map.Entry<String, String> entry : feature.getVariables()) {
                    pw.print("  ");
                    pw.print(entry.getKey());
                    pw.print("=");
                    pw.println(entry.getValue());
                }
                pw.println();
            }

            // run modes
            for(final RunMode runMode : feature.getRunModes()) {
                // settings
                if ( !runMode.getSettings().isEmpty() ) {
                    writeComment(pw, runMode.getSettings());
                    pw.print("[settings");
                    writeRunMode(pw, runMode);
                    pw.println("]");

                    for(final Map.Entry<String, String> entry : runMode.getSettings()) {
                        pw.print("  ");
                        pw.print(entry.getKey());
                        pw.print("=");
                        pw.println(entry.getValue());
                    }
                    pw.println();
                }

                // artifact groups
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    // skip empty groups
                    if ( group.isEmpty() ) {
                        continue;
                    }
                    writeComment(pw, group);
                    pw.print("[artifacts");
                    if ( group.getStartLevel() > 0 ) {
                        pw.print(" startLevel=");
                        pw.print(String.valueOf(group.getStartLevel()));
                    }
                    writeRunMode(pw, runMode);
                    pw.println("]");
                    pw.println();

                    // artifacts
                    for(final Artifact ad : group) {
                        writeComment(pw, ad);
                        pw.print("  ");
                        pw.print(ad.toMvnUrl().substring(4));
                        if ( !ad.getMetadata().isEmpty() ) {
                            boolean first = true;
                            for(final Map.Entry<String, String> entry : ad.getMetadata().entrySet()) {
                                if ( first ) {
                                    first = false;
                                    pw.print(" [");
                                } else {
                                    pw.print(", ");
                                }
                                pw.print(entry.getKey());
                                pw.print("=");
                                pw.print(entry.getValue());
                            }
                            pw.print("]");
                        }
                        pw.println();
                    }
                    pw.println();
                }

                // configurations
                if ( !runMode.getConfigurations().isEmpty() ) {
                    writeComment(pw, runMode.getConfigurations());
                    pw.print("[configurations");
                    writeRunMode(pw, runMode);
                    pw.println("]");
                    for(final Configuration config : runMode.getConfigurations()) {
                        writeComment(pw, config);
                        final String raw = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED);
                        String format = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED_FORMAT);
                        if ( format == null ) {
                            format = ModelConstants.CFG_FORMAT_FELIX_CA;
                        }
                        String cfgMode = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
                        if ( cfgMode == null ) {
                            cfgMode = ModelConstants.CFG_MODE_OVERWRITE;
                        }
                        pw.print("  ");
                        if ( config.getFactoryPid() != null ) {
                            pw.print(config.getFactoryPid());
                            pw.print("-");
                        }
                        pw.print(config.getPid());
                        final boolean isDefaultFormat = ModelConstants.CFG_FORMAT_FELIX_CA.equals(format);
                        final boolean isDefaultMode = ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode);
                        if ( !isDefaultFormat || !isDefaultMode ) {
                            pw.print(" [");
                            if ( !isDefaultFormat ) {
                                pw.print("format=");
                                pw.print(format);
                                if ( !isDefaultMode ) {
                                    pw.print(",");
                                }
                            }
                            if ( !isDefaultMode) {
                                pw.print("mode=");
                                pw.print(cfgMode);
                            }
                            pw.print("]");
                        }
                        pw.println();

                        final String configString;
                        if ( raw != null ) {
                            configString = raw;
                        } else if ( config.isSpecial() ) {
                            configString = config.getProperties().get(config.getPid()).toString();
                        } else {
                            final ByteArrayOutputStream os = new ByteArrayOutputStream();
                            try {
                                ConfigurationHandler.write(os , config.getProperties());
                            } finally {
                                os.close();
                            }
                            configString = new String(os.toByteArray(), "UTF-8");
                        }
                        // we have to read the configuration line by line to properly indent
                        final LineNumberReader lnr = new LineNumberReader(new StringReader(configString));
                        String line = null;
                        while ((line = lnr.readLine()) != null ) {
                            if ( line.trim().isEmpty() ) {
                                continue;
                            }
                            pw.print("    ");
                            pw.println(line.trim());
                        }
                        pw.println();
                    }
                }
            }

            // additional sections
            for(final Section section : feature.getAdditionalSections()) {
                pw.print("  [");
                pw.print(section.getName());
                for(final Map.Entry<String, String> entry : section.getAttributes().entrySet()) {
                    pw.print(' ');
                    pw.print(entry.getKey());
                    pw.print('=');
                    pw.print(entry.getValue());
                }
                pw.println("]");
                if ( section.getContents() != null ) {
                    pw.println(section.getContents());
                }
                pw.println();
            }
        }
    }
}
