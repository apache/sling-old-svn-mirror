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

import static org.apache.jackrabbit.vault.util.Constants.DOT_CONTENT_XML;
import static org.apache.sling.fsprovider.internal.parser.ContentFileTypes.JCR_XML_SUFFIX;
import static org.apache.sling.fsprovider.internal.parser.ContentFileTypes.JSON_SUFFIX;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.fscontentparser.ContentFileParser;
import org.apache.sling.fscontentparser.ContentFileParserFactory;
import org.apache.sling.fscontentparser.ContentFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses files that contains content fragments (e.g. JSON, JCR XML).
 */
class ContentFileParserUtil {
    
    private static final Logger log = LoggerFactory.getLogger(ContentFileParserUtil.class);
    
    private static final ContentFileParser JSON_PARSER;
    static {
        // workaround for JsonProvider classloader issue until https://issues.apache.org/jira/browse/GERONIMO-6560 is fixed
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ContentFileParserUtil.class.getClassLoader());
            JSON_PARSER = ContentFileParserFactory.create(ContentFileType.JSON);
        }
        finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
    private static final ContentFileParser JCR_XML_PARSER = ContentFileParserFactory.create(ContentFileType.JCR_XML);
    
    private ContentFileParserUtil() {
        // static methods only
    }
    
    /**
     * Parse content from file.
     * @param file File. Type is detected automatically.
     * @return Content or null if content could not be parsed.
     */
    public static Map<String,Object> parse(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            if (StringUtils.endsWith(file.getName(), JSON_SUFFIX)) {
                return JSON_PARSER.parse(file);
            }
            else if (StringUtils.equals(file.getName(), DOT_CONTENT_XML) || StringUtils.endsWith(file.getName(), JCR_XML_SUFFIX)) {
                return JCR_XML_PARSER.parse(file);
            }
        }
        catch (Throwable ex) {
            log.warn("Error parsing content from " + file.getPath(), ex);
        }
        return null;
    }

}
