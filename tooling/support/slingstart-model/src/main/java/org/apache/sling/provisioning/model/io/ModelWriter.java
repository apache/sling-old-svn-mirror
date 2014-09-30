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
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;

/**
 * Simple writer for the a model
 */
public class ModelWriter {

    private static void writeComment(final PrintWriter pw, final Traceable traceable)
    throws IOException {
        if ( traceable.getComment() != null ) {
            final LineNumberReader lnr = new LineNumberReader(new StringReader(traceable.getComment()));
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

    /**
     * Writes the model to the writer.
     * The writer is not closed.
     * @param writer
     * @param subystem
     * @throws IOException
     */
    public static void write(final Writer writer, final Model model)
    throws IOException {
        final PrintWriter pw = new PrintWriter(writer);

        writeComment(pw, model);

        // features
        for(final Feature feature : model.getFeatures()) {
            writeComment(pw, feature);
            pw.print("[feature name=");
            pw.print(feature.getName());
            pw.println("]");
            pw.println();

            // variables
            if ( !feature.getVariables().isEmpty() ) {
                pw.println("[variables]");
                for(final Map.Entry<String, String> entry : feature.getVariables().entrySet()) {
                    pw.print("  ");
                    pw.print(entry.getKey());
                    pw.print("=");
                    pw.println(entry.getValue());
                }
                pw.println();
            }

            // run modes
            for(final RunMode runMode : feature.getRunModes()) {
                // skip empty run mode
                if ( runMode.getConfigurations().isEmpty() && runMode.getSettings().isEmpty() ) {
                    boolean hasArtifacts = false;
                    for(final ArtifactGroup sl : runMode.getArtifactGroups()) {
                        if ( !sl.getArtifacts().isEmpty() ) {
                            hasArtifacts = true;
                            break;
                        }
                    }
                    if ( !hasArtifacts ) {
                        continue;
                    }
                }
                writeComment(pw, runMode);
                final String[] runModes = runMode.getRunModes();
                if ( runModes == null || runModes.length == 0 ) {
                    pw.println("[global]");
                } else {
                    pw.print("[runMode names=");
                    boolean first = true;
                    for(final String mode : runModes) {
                        if ( first ) {
                            first = false;
                        } else {
                            pw.print(",");
                        }
                        pw.print(mode);
                    }
                    pw.println("]");
                }
                pw.println();

                // settings
                if ( !runMode.getSettings().isEmpty() ) {
                    pw.println("[settings]");

                    for(final Map.Entry<String, String> entry : runMode.getSettings().entrySet()) {
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
                    if ( group.getArtifacts().isEmpty() ) {
                        continue;
                    }
                    writeComment(pw, group);
                    pw.print("[artifacts");
                    if ( group.getLevel() > 0 ) {
                        pw.print(" startLevel=");
                        pw.print(String.valueOf(group.getLevel()));
                    }
                    pw.println("]");
                    pw.println();

                    // artifacts
                    for(final Artifact ad : group.getArtifacts()) {
                        writeComment(pw, ad);
                        pw.print("  ");
                        pw.print(ad.toMvnUrl().substring(4));
                        if ( !ad.getMetadata().isEmpty() ) {
                            boolean first = true;
                            for(final Map.Entry<String, String> entry : ad.getMetadata().entrySet()) {
                                if ( first ) {
                                    first = false;
                                    pw.print("{ ");
                                } else {
                                    pw.print(", ");
                                }
                                pw.print(entry.getKey());
                                pw.print("=");
                                pw.println(entry.getValue());
                            }
                            pw.print("}");
                        }
                        pw.println();
                    }
                    if ( !group.getArtifacts().isEmpty() ) {
                        pw.println();
                    }
                }

                // configurations
                if ( !runMode.getConfigurations().isEmpty() ) {
                    pw.println("[configurations]");
                    for(final Configuration config : runMode.getConfigurations()) {
                        writeComment(pw, config);
                        final String raw = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED);
                        String format = (String)config.getProperties().get(ModelConstants.CFG_UNPROCESSED_FORMAT);
                        if ( format == null ) {
                            format = ModelConstants.CFG_FORMAT_FELIX_CA;
                        }
                        pw.print("  ");
                        if ( config.getFactoryPid() != null ) {
                            pw.print(config.getFactoryPid());
                            pw.print("-");
                        }
                        pw.print(config.getPid());
                        if ( !ModelConstants.CFG_FORMAT_FELIX_CA.equals(format) ) {
                            pw.print(" { format=}");
                            pw.print(format);
                            pw.print(" }");
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
        }
    }
}
