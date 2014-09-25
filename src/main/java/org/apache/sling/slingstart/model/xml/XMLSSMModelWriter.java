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
package org.apache.sling.slingstart.model.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMStartLevel;

/**
 * Simple writer for the a model
 */
public class XMLSSMModelWriter {

    private static void printRunModeAttribute(final PrintWriter pw, final SSMFeature rmd) {
        if ( rmd.getRunModes() != null && rmd.getRunModes().length > 0 ) {
            pw.print(" modes=\"");
            boolean first = true;
            for(final String mode : rmd.getRunModes()) {
                if ( first ) {
                    first = false;
                } else {
                    pw.print(",");
                }
                pw.print(escapeXml(mode));
            }
            pw.print("\"");
        }

    }

    private static String INDENT = "    ";


    /**
     * Writes the model to the writer.
     * The writer is not closed.
     * @param writer
     * @param subystem
     * @throws IOException
     */
    public static void write(final Writer writer, final SSMDeliverable subsystem)
    throws IOException {
        final PrintWriter pw = new PrintWriter(writer);
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<deliverable>");

        // properties
        if ( subsystem.getVariables().size() > 0 ) {
            pw.print(INDENT);
            pw.println("<variables><![CDATA[");
            for(final Map.Entry<String, String> entry : subsystem.getVariables().entrySet()) {
                pw.print(INDENT);
                pw.print(INDENT);
                pw.print(entry.getKey());
                pw.print("=");
                pw.println(entry.getValue());
            }
            pw.print(INDENT);
            pw.println("]]></variables>");
        }
        for(final SSMFeature feature : subsystem.getFeatures()) {
            // TODO - don't write out empty features
            String indent = INDENT;
            if ( feature.getRunModes() != null ) {
                pw.print(indent);
                pw.print("<feature");
                printRunModeAttribute(pw, feature);
                pw.println(">");
                indent = indent + INDENT;
            }

            for(final SSMStartLevel startLevel : feature.getStartLevels()) {
                if ( startLevel.getArtifacts().size() == 0 ) {
                    continue;
                }
                if ( startLevel.getLevel() != 0 ) {
                    pw.print(indent);
                    pw.print("<startLevel");
                    pw.print(" level=\"");
                    pw.print(String.valueOf(startLevel.getLevel()));
                    pw.print("\"");
                    pw.println(">");
                    indent += INDENT;
                }
                for(final SSMArtifact ad : startLevel.getArtifacts()) {
                    pw.print(indent);
                    pw.print("<artifact groupId=\"");
                    pw.print(escapeXml(ad.getGroupId()));
                    pw.print("\" artifactId=\"");
                    pw.print(escapeXml(ad.getArtifactId()));
                    pw.print("\" version=\"");
                    pw.print(escapeXml(ad.getVersion()));
                    pw.print("\"");
                    if ( !"jar".equals(ad.getType()) ) {
                        pw.print(" type=\"");
                        pw.print(escapeXml(ad.getType()));
                        pw.print("\"");
                    }
                    if ( ad.getClassifier() != null ) {
                        pw.print(" classifier=\"");
                        pw.print(escapeXml(ad.getClassifier()));
                        pw.print("\"");
                    }
                    pw.println("/>");
                }
                if ( startLevel.getLevel() != 0 ) {
                    indent = indent.substring(0, indent.length() - INDENT.length());
                    pw.print(indent);
                    pw.println("</startLevel>");
                }
            }

            for(final SSMConfiguration config : feature.getConfigurations()) {
                pw.print(indent);
                pw.print("<configuration ");
                if ( config.getFactoryPid() != null ) {
                    pw.print("factory=\"");
                    pw.print(escapeXml(config.getFactoryPid()));
                    pw.print("\" ");
                }
                pw.print("pid=\"");
                pw.print(escapeXml(config.getPid()));
                pw.println("\"><![CDATA[");
                if ( config.isSpecial() ) {
                    pw.println(config.getProperties().get(config.getPid()));
                } else {
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ConfigurationHandler.write(os , config.getProperties());
                    } finally {
                        os.close();
                    }
                    final String configString = new String(os.toByteArray(), "UTF-8");
                    pw.println(configString);
                }
                pw.print(indent);
                pw.println("]]></configuration>");
            }

            if ( feature.getSettings().size() > 0 ) {
                pw.print(indent);
                pw.println("<settings><![CDATA[");

                for(final Map.Entry<String, String> entry :feature.getSettings().entrySet()) {
                    pw.print(INDENT);
                    pw.print(INDENT);
                    pw.print(entry.getKey());
                    pw.print("=");
                    pw.println(entry.getValue());
                }

                pw.print(indent);
                pw.println("]]></settings>");
            }

            if ( feature.getRunModes() != null ) {
                indent = indent.substring(0, indent.length() - INDENT.length());
                pw.print(indent);
                pw.println("</feature>");
            }
        }

        pw.println("</deliverable>");
    }

    /** Escape xml text */
    private static String escapeXml(final String input) {
        if (input == null) {
            return null;
        }

        final StringBuilder b = new StringBuilder(input.length());
        for(int i = 0;i  < input.length(); i++) {
            final char c = input.charAt(i);
            if(c == '&') {
                b.append("&amp;");
            } else if(c == '<') {
                b.append("&lt;");
            } else if(c == '>') {
                b.append("&gt;");
            } else if(c == '"') {
                b.append("&quot;");
            } else if(c == '\'') {
                b.append("&apos;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

}
