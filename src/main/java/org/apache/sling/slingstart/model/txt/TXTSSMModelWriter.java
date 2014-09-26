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
package org.apache.sling.slingstart.model.txt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.apache.sling.slingstart.model.SSMTraceable;

/**
 * Simple writer for the a model
 */
public class TXTSSMModelWriter {

    private static void writeComment(final PrintWriter pw, final SSMTraceable traceable)
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
    public static void write(final Writer writer, final SSMDeliverable model)
    throws IOException {
        final PrintWriter pw = new PrintWriter(writer);

        writeComment(pw, model);
        // variables
        if ( model.getVariables().size() > 0 ) {
            pw.println("variables:");
            for(final Map.Entry<String, String> entry : model.getVariables().entrySet()) {
                pw.print("  ");
                pw.print(entry.getKey());
                pw.print("=");
                pw.println(entry.getValue());
            }
            pw.println();
        }

        // features
        for(final SSMFeature feature : model.getFeatures()) {
            // skip empty feature
            if ( feature.getConfigurations().isEmpty() && feature.getSettings().isEmpty() ) {
                boolean hasArtifacts = false;
                for(final SSMStartLevel sl : feature.getStartLevels()) {
                    if ( !sl.getArtifacts().isEmpty() ) {
                        hasArtifacts = true;
                        break;
                    }
                }
                if ( !hasArtifacts ) {
                    continue;
                }
            }
            writeComment(pw, feature);
            pw.print("feature:");
            final String[] runModes = feature.getRunModes();
            if ( runModes != null && runModes.length > 0 ) {
                pw.print(" ");
                boolean first = true;
                for(final String mode : runModes) {
                    if ( first ) {
                        first = false;
                    } else {
                        pw.print(",");
                    }
                    pw.print(mode);
                }
            }
            pw.println();

            // settings
            if ( feature.getSettings().size() > 0 ) {
                pw.println("  settings:");

                for(final Map.Entry<String, String> entry :feature.getSettings().entrySet()) {
                    pw.print("    ");
                    pw.print(entry.getKey());
                    pw.print("=");
                    pw.println(entry.getValue());
                }
                pw.println();
            }

            // start level
            for(final SSMStartLevel startLevel : feature.getStartLevels()) {
                // skip empty levels
                if ( startLevel.getArtifacts().size() == 0 ) {
                    continue;
                }
                writeComment(pw, startLevel);
                pw.print("  startLevel: ");
                pw.print(String.valueOf(startLevel.getLevel()));
                pw.println();
                pw.println();

                // artifacts
                for(final SSMArtifact ad : startLevel.getArtifacts()) {
                    writeComment(pw, ad);
                    pw.print("    ");
                    pw.print("artifact: ");
                    pw.print(ad.toMvnUrl().substring(4));
                    pw.println();
                    if ( ad.getMetadata().size() > 0 ) {
                        for(final Map.Entry<String, String> entry : ad.getMetadata().entrySet()) {
                            pw.print("      ");
                            pw.print(entry.getKey());
                            pw.print("=");
                            pw.println(entry.getValue());
                        }
                    }
                }
                if ( startLevel.getArtifacts().size() > 0 ) {
                    pw.println();
                }
            }

            // configurations
            for(final SSMConfiguration config : feature.getConfigurations()) {
                writeComment(pw, config);
                pw.print("  config: ");
                if ( config.getFactoryPid() != null ) {
                    pw.print(config.getFactoryPid());
                    pw.print("-");
                }
                pw.print(config.getPid());
                pw.println();
                final String configString;
                if ( config.isSpecial() ) {
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
                    pw.print("    ");
                    pw.println(line);
                }
                pw.println();
            }
        }
    }
}
