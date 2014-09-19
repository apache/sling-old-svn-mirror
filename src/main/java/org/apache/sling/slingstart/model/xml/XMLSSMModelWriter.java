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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMRunMode;
import org.apache.sling.slingstart.model.SSMStartLevel;
import org.apache.sling.slingstart.model.SSMSubsystem;

/**
 * Simple writer for the a model
 */
public class XMLSSMModelWriter {

    private static void printRunModeAttribute(final PrintWriter pw, final SSMRunMode rmd) {
        if ( rmd.runModes != null && rmd.runModes.length > 0 ) {
            pw.print(" modes=\"");
            boolean first = true;
            for(final String mode : rmd.runModes) {
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
    public static void write(final Writer writer, final SSMSubsystem subsystem)
    throws IOException {
        final PrintWriter pw = new PrintWriter(writer);
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<subsystem>");

        // properties
        if ( subsystem.properties.size() > 0 ) {
            pw.print(INDENT);
            pw.println("<properties><![CDATA[");
            for(final Map.Entry<String, String> entry : subsystem.properties.entrySet()) {
                pw.print(INDENT);
                pw.print(INDENT);
                pw.print(entry.getKey());
                pw.print("=");
                pw.println(entry.getValue());
            }
            pw.print(INDENT);
            pw.println("]]></properties>");
        }
        for(final SSMRunMode runMode : subsystem.runModes) {
            // TODO - don't write out empty run modes
            String indent = INDENT;
            if ( runMode.runModes != null ) {
                pw.print(indent);
                pw.print("<runMode");
                printRunModeAttribute(pw, runMode);
                pw.println(">");
                indent = indent + INDENT;
            }

            for(final SSMStartLevel startLevel : runMode.startLevels) {
                if ( startLevel.artifacts.size() == 0 ) {
                    continue;
                }
                if ( startLevel.level != 0 ) {
                    pw.print(indent);
                    pw.print("<startLevel");
                    pw.print(" level=\"");
                    pw.print(String.valueOf(startLevel.level));
                    pw.print("\"");
                    pw.println(">");
                    indent += INDENT;
                }
                for(final SSMArtifact ad : startLevel.artifacts) {
                    pw.print(indent);
                    pw.print("<artifact groupId=\"");
                    pw.print(escapeXml(ad.groupId));
                    pw.print("\" artifactId=\"");
                    pw.print(escapeXml(ad.artifactId));
                    pw.print("\" version=\"");
                    pw.print(escapeXml(ad.version));
                    pw.print("\"");
                    if ( !"jar".equals(ad.type) ) {
                        pw.print(" type=\"");
                        pw.print(escapeXml(ad.type));
                        pw.print("\"");
                    }
                    if ( ad.classifier != null ) {
                        pw.print(" classifier=\"");
                        pw.print(escapeXml(ad.classifier));
                        pw.print("\"");
                    }
                    pw.println("/>");
                }
                if ( startLevel.level != 0 ) {
                    indent = indent.substring(0, indent.length() - INDENT.length());
                    pw.print(indent);
                    pw.println("</startLevel>");
                }
            }

            for(final SSMConfiguration config : runMode.configurations) {
                pw.print(indent);
                pw.print("<configuration ");
                if ( config.factoryPid != null ) {
                    pw.print("factory=\"");
                    pw.print(escapeXml(config.factoryPid));
                    pw.print("\" ");
                }
                pw.print("pid=\"");
                pw.print(escapeXml(config.pid));
                pw.println("\"><![CDATA[");
                pw.println(config.properties);
                pw.print(indent);
                pw.println("]]></configuration>");
            }

            if ( runMode.settings != null ) {
                pw.print(indent);
                pw.println("<settings><![CDATA[");
                pw.println(runMode.settings.properties);
                pw.print(indent);
                pw.println("]]></settings>");
            }

            if ( runMode.runModes != null ) {
                indent = indent.substring(0, indent.length() - INDENT.length());
                pw.print(indent);
                pw.println("</runMode>");
            }
        }

        pw.println("</subsystem>");
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
