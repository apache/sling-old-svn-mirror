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
package org.apache.sling.nosql.generic.resource.impl;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper functions for handling paths.
 */
public final class PathUtil {

    private PathUtil() {
        // static methods only
    }
    
    /**
     * Generated a regex pattern that accepts all paths that are direct children of the given parent path.
     * @param parentPath Parent path
     * @return Regex pattern
     */
    public static Pattern getChildPathPattern(String parentPath) {
        return Pattern.compile("^" + Pattern.quote(StringUtils.removeEnd(parentPath,  "/")) + "/[^/]+$");
    }
    
    /**
     * Generated a regex pattern that accepts all paths that are same or descendants of the given parent path.
     * @param path Path
     * @return Regex pattern
     */
    public static Pattern getSameOrDescendantPathPattern(String path) {
        return Pattern.compile("^" + Pattern.quote(path) + "(/.*)?$");
    }
    
}
