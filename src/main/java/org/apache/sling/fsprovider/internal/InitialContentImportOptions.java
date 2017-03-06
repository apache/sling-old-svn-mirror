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
package org.apache.sling.fsprovider.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

class InitialContentImportOptions {

    /**
     * The overwrite directive specifying if content should be overwritten or
     * just initially added.
     */
    private static final String OVERWRITE_DIRECTIVE = "overwrite";

    /**
     * The ignore content readers directive specifying whether the available ContentReaders
     * should be used during content loading.
     */
    private static final String IGNORE_CONTENT_READERS_DIRECTIVE = "ignoreImportProviders";

    
    private final boolean overwrite;
    private final Set<String> ignoreImportProviders;
    
    public InitialContentImportOptions(String optionsString) {
        Map<String,String> options = parseOptions(optionsString);
        overwrite = BooleanUtils.toBoolean(options.get(OVERWRITE_DIRECTIVE));
        ignoreImportProviders = new HashSet<>(Arrays.asList(StringUtils.split(StringUtils.defaultString(options.get(IGNORE_CONTENT_READERS_DIRECTIVE)))));
    }
    
    private static Map<String,String> parseOptions(String optionsString) {
        Map<String,String> options = new HashMap<>();
        String[] optionsList = StringUtils.split(optionsString, ";");
        if (optionsList != null) {
            for (String keyValueString : optionsList) {
                String[] keyValue = StringUtils.splitByWholeSeparator(keyValueString, ":=");
                if (keyValue.length == 2) {
                    options.put(StringUtils.trim(keyValue[0]), StringUtils.trim(keyValue[1]));
                }
            }
        }
        return options;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public Set<String> getIgnoreImportProviders() {
        return ignoreImportProviders;
    }
    
}
