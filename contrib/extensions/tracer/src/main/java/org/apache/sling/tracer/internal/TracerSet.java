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

package org.apache.sling.tracer.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.Level;
import org.apache.sling.commons.osgi.ManifestHeader;

class TracerSet {
    public static final String LEVEL = "level";
    private final String name;
    private final List<TracerConfig> configs;

    public TracerSet(String config) {
        int indexOfColon = config.indexOf(':');
        if (indexOfColon == -1) {
            throw new IllegalArgumentException("Invalid tracer config format. TracerSet " +
                    "name cannot be determined " + config);
        }

        name = config.substring(0, indexOfColon).toLowerCase().trim();
        configs = parseTracerConfigs(config.substring(indexOfColon + 1));
    }

    public TracerSet(String name, String config) {
        this.name = name;
        this.configs = parseTracerConfigs(config);
    }

    public TracerConfig getConfig(String category) {
        for (TracerConfig tc : configs) {
            if (tc.match(category)) {
                return tc;
            }
        }
        return null;
    }

    public List<TracerConfig> getConfigs() {
        return configs;
    }

    public String getName() {
        return name;
    }

    private static List<TracerConfig> parseTracerConfigs(String config) {
        ManifestHeader parsedConfig = ManifestHeader.parse(config);
        List<TracerConfig> result = new ArrayList<TracerConfig>(parsedConfig.getEntries().length);
        for (ManifestHeader.Entry e : parsedConfig.getEntries()) {
            String category = e.getValue();

            //Defaults to Debug
            Level level = Level.valueOf(e.getAttributeValue(LEVEL));
            result.add(new TracerConfig(category, level));
        }
        return Collections.unmodifiableList(result);
    }
}
