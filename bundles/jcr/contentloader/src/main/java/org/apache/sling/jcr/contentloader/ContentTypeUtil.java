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
package org.apache.sling.jcr.contentloader;

import org.apache.commons.lang.StringUtils;

public class ContentTypeUtil {

    public static final String EXT_JSON = ".json";

    public static final String TYPE_JSON = "application/json";

    public static final String EXT_XML = ".xml";

    public static final String TYPE_XML = "application/xml";

    public static final String EXT_JCR_XML = ".jcr.xml";

    // unofficial content type!
    public static final String TYPE_JCR_XML = "application/x-jcr+xml";

    public static final String EXT_ZIP = ".zip";

    public static final String TYPE_ZIP = "application/zip";

    public static final String EXT_JAR = ".jar";

    public static final String TYPE_JAR = "application/java-archive";

    public static String detectContentType(final String filename) {
        if (StringUtils.isNotBlank(filename)) {
            if (StringUtils.endsWithIgnoreCase(filename, EXT_JSON)) {
                return TYPE_JSON;
            }
            if (StringUtils.endsWithIgnoreCase(filename, EXT_JCR_XML)) {
                return TYPE_JCR_XML;
            }
            if (StringUtils.endsWithIgnoreCase(filename, EXT_XML)) {
                return TYPE_XML;
            }
            if (StringUtils.endsWithIgnoreCase(filename, EXT_ZIP)) {
                return TYPE_ZIP;
            }
            if (StringUtils.endsWithIgnoreCase(filename, EXT_JAR)) {
                return TYPE_JAR;
            }
        }
        return null;
    }

    public static String getDefaultExtension(final String contentType) {
        if (StringUtils.isNotBlank(contentType)) {
            if (TYPE_JSON.equalsIgnoreCase(contentType)) {
                return EXT_JSON;
            }
            if (TYPE_JCR_XML.equalsIgnoreCase(contentType)) {
                return EXT_JCR_XML;
            }
            if (TYPE_XML.equalsIgnoreCase(contentType)) {
                return EXT_XML;
            }
            if (TYPE_ZIP.equalsIgnoreCase(contentType)) {
                return EXT_ZIP;
            }
            if (TYPE_JAR.equalsIgnoreCase(contentType)) {
                return EXT_JAR;
            }
        }
        return null;
    }

}
