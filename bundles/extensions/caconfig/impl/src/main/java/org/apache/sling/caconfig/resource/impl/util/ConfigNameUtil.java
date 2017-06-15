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
package org.apache.sling.caconfig.resource.impl.util;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper methods for configuration names.
 */
public final class ConfigNameUtil {
    
    private ConfigNameUtil() {
        // static methods only
    }

    /**
     * Check if the config name is valid.
     * @param configName The name
     * @return {@code true} if it is valid
     */
    public static boolean isValid(final String configName) {
        return !StringUtils.isBlank(configName)
                && !StringUtils.startsWith(configName, "/")
                && !StringUtils.contains(configName, "../");
    }
    
    /**
     * Check if the config name is valid.
     * @param configNames The names
     * @return {@code true} if it is valid
     */
    public static boolean isValid(final Collection<String> configNames) {
        if (configNames == null) {
            return false;
        }
        for (String configName : configNames) {
            if (!isValid(configName)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Ensure that the config name is valid.
     * @param configName The name
     * @throws IllegalArgumentException if the config name is not valid
     */
    public static void ensureValidConfigName(final String configName) {
        if (!isValid(configName)) {
            throw new IllegalArgumentException("Invalid configuration name: " + configName);
        }
    }
    
    /**
     * Returns all partial combinations like: a, a/b, a/b/c from config name a/b/c/d
     * @param configName Config name
     * @return All partial combinations
     */
    public static String[] getAllPartialConfigNameVariations(String configName) {
        String[] configNameParts = StringUtils.splitPreserveAllTokens(configName, "/");
        if (configNameParts.length < 2) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String[] partialConfigNameVariations = new String[configNameParts.length - 1];
        for (int i = 0; i < configNameParts.length - 1; i++) {
            partialConfigNameVariations[i] = StringUtils.join(ArrayUtils.subarray(configNameParts, 0, i + 1), "/");
        }
        return partialConfigNameVariations;
    }

}
