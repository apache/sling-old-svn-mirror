/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Test the String formatting functionality of JSONObject */
public class DespacedResult {
    private final String despaced;
    
    /** Simplify whitespace in str and replace quotes
     *  to make it easier to compare the output 
     *  @param spaceReplacement replaces spaces after the first one
     *  */ 
    DespacedResult(String str, String spaceReplacement) throws JSONException {
        
        boolean previousWasSpace = false;
        
        // Verify that str parses
        new JSONObject(str);
        
        // And convert whitespace and quotes to
        // make comparisons easier
        final StringBuilder sb = new StringBuilder();
        for(int i=0; i < str.length(); i++) {
            final Character c = str.charAt(i);
            final boolean isWhitespace = Character.isWhitespace(c); 
            if(c == '\n') {
                sb.append("-nl-");
            } else if(isWhitespace) {
                if(previousWasSpace && spaceReplacement != null) {
                    sb.append(spaceReplacement);
                }
            } else if(c == '\"') {
                sb.append("_");
            } else if(c == '\"') {
                sb.append("-dq-");
            } else {
                sb.append(c);
            }
            previousWasSpace = isWhitespace;
        }
        despaced = sb.toString();
    }
    
    DespacedResult(String str) throws JSONException {
        this(str, null);
    }
    

    DespacedResult expect(String ...expected) {
        for(String e : expected) {
            assertTrue("Expecting " + e + " to be contained in " + despaced, despaced.contains(e));
        }
        return this;
    }
    
    DespacedResult assertExactMatch(String expected) {
        assertEquals("Expecting an exact match with " + expected + " at " + despaced, expected, despaced);
        return this;
    }
    
    DespacedResult expectLength(int n) {
        assertEquals("Expecting a String length of " + n + " for " + despaced, n, despaced.length());
        return this;
    }
};