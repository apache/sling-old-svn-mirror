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
package org.apache.sling.jcr.contentparser.impl;

/**
 * Converts JSON with ticks to JSON with quotes.
 * <p>Conversions:</p>
 * <ul>
 * <li>Converts ticks ' to " when used as quotation marks for names or string values</li>
 * <li>Within names or string values quoted with ticks, ticks have to be escaped with <code>\'</code>.
 *     This escaping sign is removed on the conversion, because in JSON ticks must not be escaped.</li>
 * <li>Within names or string values quoted with ticks, double quotes may or may not be escaped.
 *     After the conversion they are always escaped.</li>
 * </ul>
 */
public final class JsonTicksConverter {
    
    private JsonTicksConverter() {
        // static methods only
    }
    
    public static String tickToDoubleQuote(final String input) {
        final int len = input.length();
        final StringBuilder output = new StringBuilder(len);
        boolean quoted = false;
        boolean tickQuoted = false;
        boolean escaped = false;
        boolean comment = false;
        char lastChar = ' ';
        for (int i = 0; i < len; i++) {
            char in = input.charAt(i);
            if (quoted || tickQuoted) {
                if (escaped) {
                    if (in != '\'') {
                        output.append("\\");
                    }
                    if (in == '\\') {
                        output.append("\\");
                    }
                    escaped = false;
                }
                else {
                    if (in == '"') {
                        if (quoted) {
                            quoted = false;
                        }
                        else if (tickQuoted) {
                            output.append("\\");
                        }
                    }
                    else if (in == '\'') {
                        if (tickQuoted) {
                            in = '"';
                            tickQuoted = false;
                        }
                    }
                    else if (in == '\\') {
                        escaped = true;
                    }
                }
            }
            else {
                if (comment) {
                    if (lastChar == '*' && in == '/') {
                        comment = false;
                    }
                }
                else {
                    if (lastChar == '/' && in == '*') {
                        comment = true;
                    }
                    else if (in == '\'') {
                        in = '"';
                        tickQuoted = true;
                    }
                    else if (in == '"') {
                        quoted = true;
                    }
                }
            }
            if (in == '\\') {
                continue;
            }
            output.append(in);
            lastChar = in;
        }
        return output.toString();
    }

}
