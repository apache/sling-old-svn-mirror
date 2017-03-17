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
package org.apache.sling.jcr.contentparser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Options for content parser.
 */
public final class ParserOptions {
    
    /**
     * Default primary type.
     */
    public static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured";

    /**
     * Default list of prefixes to remove from property names.
     */
    public static final Set<String> DEFAULT_REMOVE_PROPERTY_NAME_PREFIXES
            = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("jcr:reference:", "jcr:path:")));
    
    private String defaultPrimaryType = DEFAULT_PRIMARY_TYPE;
    private boolean detectCalendarValues;
    private Set<String> ignorePropertyNames;
    private Set<String> ignoreResourceNames;
    private Set<String> removePropertyNamePrefixes = DEFAULT_REMOVE_PROPERTY_NAME_PREFIXES;
    
    /**
     * Default "jcr:primaryType" property for resources that have no explicit value for this value.
     * If set to null, not default type is applied.
     * @param value Default primary type.
     * @return this
     */
    public ParserOptions defaultPrimaryType(String value) {
        this.defaultPrimaryType = value;
        return this;
    }
    public String getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * Some content formats like JSON do not contain information to identify date/time values.
     * Instead they have to be detected by heuristics by trying to parse every string value.
     * This mode is disabled by default.
     * @param value Activate calendar value detection
     * @return this
     */
    public ParserOptions detectCalendarValues(boolean value) {
        this.detectCalendarValues = value;
        return this;
    }
    public boolean isDetectCalendarValues() {
        return detectCalendarValues;
    }

    /**
     * Set a list of property names that should be ignored when parsing the content file.
     * @param value List of property names 
     * @return this
     */
    public ParserOptions ignorePropertyNames(Set<String> value) {
        this.ignorePropertyNames = value;
        return this;
    }
    public Set<String> getIgnorePropertyNames() {
        return ignorePropertyNames;
    }

    /**
     * Set a list of resource/node names that should be ignored when parsing the content file.
     * @param value List of resource/node names
     * @return this
     */
    public ParserOptions ignoreResourceNames(Set<String> value) {
        this.ignoreResourceNames = value;
        return this;
    }
    public Set<String> getIgnoreResourceNames() {
        return ignoreResourceNames;
    }

    /**
     * Set a list of property name prefixes that should be removed automatically from the property name. 
     * @param value List of property name prefixes
     * @return this
     */
    public ParserOptions removePropertyNamePrefixes(Set<String> value) {
        this.removePropertyNamePrefixes = value;
        return this;
    }
    public Set<String> getRemovePropertyNamePrefixes() {
        return removePropertyNamePrefixes;
    }
    
}
