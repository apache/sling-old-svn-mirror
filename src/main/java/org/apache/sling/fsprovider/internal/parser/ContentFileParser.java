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
package org.apache.sling.fsprovider.internal.parser;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses file that contains content fragments (e.g. JSON, JCR XML).
 */
public final class ContentFileParser {
    
    /**
     * JSON content files.
     */
    public static final String JSON_SUFFIX = ".json";
    
    private ContentFileParser() {
        // static methods only
    }
    
    /**
     * Parse content from file.
     * @param file File. Type is detected automatically.
     * @return Content or null if content could not be parsed.
     */
    public static Map<String,Object> parse(File file) {
        if (StringUtils.endsWith(file.getName(), JSON_SUFFIX)) {
            return JsonFileParser.parse(file);
        }
        return null;
    }

}
