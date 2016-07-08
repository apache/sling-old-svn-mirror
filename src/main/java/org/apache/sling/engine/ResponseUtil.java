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
package org.apache.sling.engine;

import java.io.Writer;

/**
 * Response-related utilities.
 * @deprecated Use {@link org.apache.sling.api.request.ResponseUtil}
 */
@Deprecated
public class ResponseUtil {

    /** 
     * Escape xml text 
     * @param input The text to escape
     * @return The escaped text.
     */
    public static String escapeXml(final String input) {
        return org.apache.sling.api.request.ResponseUtil.escapeXml(input);
    }

    /** 
     * Return a Writer that writes escaped XML text to target
     * @param target The writer to wrap
     * @return The wrapped writer
     */
    public static Writer getXmlEscapingWriter(final Writer target) {
        return org.apache.sling.api.request.ResponseUtil.getXmlEscapingWriter(target);
    }
}
