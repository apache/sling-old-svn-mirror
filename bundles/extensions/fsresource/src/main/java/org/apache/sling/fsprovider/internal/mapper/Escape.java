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
package org.apache.sling.fsprovider.internal.mapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.BitSet;

import org.apache.commons.lang3.CharEncoding;

/**
 * Manages deescaping for platform file names to resource names.
 */
public final class Escape {
    
    /**
     * List of characters typically prohibited on unix and windows file systems.
     * "/" is not included because it is neither allowed in resource nor in file names on any system. 
     */
    private static final char[] RESERVED_CHARS = {
            '<',
            '>',
            ':',
            '"',
            '\\',
            '|',
            '?',
            '*',
            0x00
    };
    private static final BitSet RESERVED_CHARS_SET = new BitSet();
    static {
        for (int i=0; i<RESERVED_CHARS.length; i++) {
            RESERVED_CHARS_SET.set(RESERVED_CHARS[i]);
        }
    }
    
    private Escape() {
        // static methods only
    }
    
    /**
     * Convert file name to resource name.
     * Applies same rules as Apache Sling JCR ContentLoader. 
     * @param path File name or path
     * @return Resource name or path
     */
    public static String fileToResourceName(String path) {
        // check for encoded characters (%xx)
        // has encoded characters, need to decode
        if (path.indexOf('%') >= 0) {
            try {
                return URLDecoder.decode(path, "UTF-8");
            }
            catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Unsupported encoding.", ex);
            }
        }
        return path;
    }
    
    /**
     * Converts resource name to file name.
     * Allows all characters, but URL-encodes characters that are in the list of {@link #RESERVED_CHARS}.
     * @param path Resource name or path
     * @return File name or path
     */
    public static String resourceToFileName(String path) {
        try {
            StringBuilder result = new StringBuilder();
            for (int i=0; i<path.length(); i++) {
                char c = path.charAt(i);
                if (RESERVED_CHARS_SET.get(c)) {
                    result.append(URLEncoder.encode(String.valueOf(c), CharEncoding.UTF_8));
                }
                else {
                    result.append(c);
                }
            }
            return result.toString();
        }
        catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unsupported encoding.", ex);
        }
    }

}
