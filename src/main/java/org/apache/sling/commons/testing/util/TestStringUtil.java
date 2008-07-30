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
package org.apache.sling.commons.testing.util;

/** String utilities for testing */
public class TestStringUtil {
    static private final String NATIVE_LINE_SEP = System.getProperty("line.separator");

    /** Replace \n with . in strings to make it easier to compare visually for testing */
    public static String flatten(String str) {

        // First replace native line-endings
        if(str.indexOf(NATIVE_LINE_SEP) >= 0) {
            str = str.replace(NATIVE_LINE_SEP, ".");
        }

        // Now find non-native line-endings, e.g. cygwin needs this
        if(str.indexOf('\n') >= 0) {
            str = str.replace('\n', '.');
        }

        return str;
    }
}
