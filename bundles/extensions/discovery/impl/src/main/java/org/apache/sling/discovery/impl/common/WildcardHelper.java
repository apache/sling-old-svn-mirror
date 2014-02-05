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
package org.apache.sling.discovery.impl.common;

import java.util.regex.Pattern;

/** Helper class for wildcards **/
public class WildcardHelper {

    /** converts a string containing wildcards (* and ?) into a valid regex **/
    public static String wildcardAsRegex(String patternWithWildcards) {
        if (patternWithWildcards==null) {
            throw new IllegalArgumentException("patternWithWildcards must not be null");
        }
        return "\\Q"+patternWithWildcards.replace("?", "\\E.\\Q").replace("*", "\\E.*\\Q")+"\\E";
    }

    /**
     * Compare a given string (comparee) against a pattern that contains wildcards
     * and return true if it matches.
     * @param comparee the string which should be tested against a pattern containing wildcards
     * @param patternWithWildcards the pattern containing wildcards (* and ?)
     * @return true if the comparee string matches against the pattern containing wildcards
     */
    public static boolean matchesWildcard(String comparee, String patternWithWildcards) {
        if (comparee==null) {
            throw new IllegalArgumentException("comparee must not be null");
        }
        if (patternWithWildcards==null) {
            throw new IllegalArgumentException("patternWithEWildcards must not be null");
        }
        final String regex = wildcardAsRegex(patternWithWildcards);
        return Pattern.matches(regex, comparee);
    }

}
