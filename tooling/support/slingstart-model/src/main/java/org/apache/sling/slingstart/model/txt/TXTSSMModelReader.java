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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.UUID;

import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;


public class TXTSSMModelReader {

    public static final String FELIX_FORMAT_SUFFIX = "FORMAT:felix.config";

    /**
     * Reads the deliverable file
     * The reader is not closed.
     * @throws IOException
     */
    public static SSMDeliverable read(final Reader reader)
    throws IOException {
        final SSMDeliverable model = new SSMDeliverable();
        final LineNumberReader lnr = new LineNumberReader(reader);
        String line;
        while ( (line = lnr.readLine()) != null ) {
            if ( ignore(line) ) {
                continue;
            }

            // Command must start with a verb, optionally followed
            // by properties
            if (!isVerb(line)) {
                throw new IOException("Expecting verb, current line is " + line);
            }

            // Parse verb and qualifier from first line
            final String [] firstLine= line.split(" ");
            final String verb = firstLine[0];
            final StringBuilder builder = new StringBuilder();
            for(int i=1; i < firstLine.length; i++) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(firstLine[i]);
            }
            final String qualifier = builder.toString();

            // Parse properties from optional indented lines
            // that follow verb line
            StringBuilder props = null;
            do {
                line = lnr.readLine();
                if (line != null && !isVerb(line)) {
                    if (props == null) {
                        props = new StringBuilder();
                    }
                    addProperty(props, line);
                }
            } while ( line != null && !isVerb(line));

            if ( "classpath".equals("verb") ) {
                final SSMFeature boot = model.getOrCreateFeature(new String[] {SSMFeature.RUN_MODE_BOOT});
                final SSMArtifact artifact = SSMArtifact.fromMvnUrl(qualifier);
                boot.getOrCreateStartLevel(0).artifacts.add(artifact);
            } else if ( "bundle".equals(verb) ) {
                final SSMFeature feature = model.getOrCreateFeature(null);
                final SSMArtifact artifact = SSMArtifact.fromMvnUrl(qualifier);
                feature.getOrCreateStartLevel(0).artifacts.add(artifact);
            } else if ( "config".equals(verb) ) {
                final SSMFeature feature = model.getOrCreateFeature(null);
                final SSMConfiguration config = new SSMConfiguration();
                boolean felixFormat = false;
                if (qualifier.endsWith(FELIX_FORMAT_SUFFIX)) {
                    felixFormat = true;
                    config.pid = qualifier.split(" ")[0].trim();
                } else {
                    config.pid = qualifier;
                }
                if ( props != null ) {
                    config.properties = props.toString();
                }
                feature.configurations.add(config);
            } else if ( "config.factory".equals(verb) ) {
                final SSMFeature feature = model.getOrCreateFeature(null);
                final SSMConfiguration config = new SSMConfiguration();
                boolean felixFormat = false;
                if (qualifier.endsWith(FELIX_FORMAT_SUFFIX)) {
                    felixFormat = true;
                    config.factoryPid = qualifier.split(" ")[0].trim();
                } else {
                    config.factoryPid = qualifier;
                }
                // create unique alias
                config.pid = UUID.randomUUID().toString();

                if ( props != null ) {
                    config.properties = props.toString();
                }
                feature.configurations.add(config);
            }
        }

        return model;
    }

    private static boolean ignore(final String line) {
        return line.trim().length() == 0 || line.startsWith("#");
    }

    private static boolean isVerb(final String line) {
        return line.length() > 0 && !Character.isWhitespace(line.charAt(0));
    }

   private static void addProperty(final StringBuilder builder, final String line)
   throws IOException {
        final int equalsPos = line.indexOf('=');
        final String key = line.substring(0, equalsPos).trim();
        final String value = line.substring(equalsPos + 1).trim();
        if (key.trim().isEmpty() || value.trim().isEmpty() ) {
            throw new IOException("Invalid property line [" + line + "]");
        }

        builder.append(key);
        builder.append('=');
        builder.append(value);
        builder.append('\n');
    }
}


