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
package org.apache.sling.distribution.serialization.impl.vlt;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jackrabbit.vault.fs.api.PathMapping;

final class RegexpPathMapping implements PathMapping {

    private final Map<Pattern, String> pathsMapping = new HashMap<Pattern, String>();

    public <K, V> void addAllMappings(Map<K, V> pathsMappingMap) {
        for (Entry<K, V> entry : pathsMappingMap.entrySet()) {
            addMapping(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    public void addMapping(String fromPattern, String toPattern) {
        pathsMapping.put(Pattern.compile(fromPattern), toPattern);
    }

    @Override
    public String map(String path) {
        for (Entry<Pattern, String> pathMapping : pathsMapping.entrySet()) {
            Matcher matcher = pathMapping.getKey().matcher(path);
            if (matcher.matches()) {
                return matcher.replaceAll(pathMapping.getValue());
            }
        }
        return path;
    }

    // `reverse` is not taken in consideration at all in this version
    @Override
    public String map(String path, boolean reverse) {
        return map(path);
    }

}
