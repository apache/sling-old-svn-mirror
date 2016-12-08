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
package org.apache.sling.caconfig.impl.override;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Holds override information provided by override providers.
 */
class OverrideItem {

    private final String path;
    private final Pattern pathPattern;
    private final String configName;
    private final Map<String,Object> properties;
    private final boolean allProperties;
    
    public OverrideItem(String path, String configName,
            Map<String, Object> properties, boolean allProperties) {
        this.path = path;
        this.pathPattern = toPathPattern(path);
        this.configName = configName;
        this.properties = properties;
        this.allProperties = allProperties;
    }
    
    private static Pattern toPathPattern(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        return Pattern.compile("^" + Pattern.quote(StringUtils.trim(path)) + "(/.*)?$");
    }

    /**
     * @return Path (incl. subtree) to match - or null for all paths
     */
    public String getPath() {
        return path;
    }
    
    /**
     * @param path Path to check
     * @return true if path matches
     */
    public boolean matchesPath(String path) {
        if (pathPattern == null) {
            return true;
        }
        else {
            return pathPattern.matcher(path).matches();
        }
    }

    /**
     * @return Configuration name (may contain a relative hierarchy with "/")
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * @return Properties map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @return If true, all properties for this config name should be replaced
     *    with those from the map. Otherwise they are merged.
     */
    public boolean isAllProperties() {
        return allProperties;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
}
