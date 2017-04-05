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
package org.apache.sling.commons.json.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONTokener;

/**
 * Utility class for validating JSON text.
 */
@Deprecated
public class Validator {

    /**
     * Strictly validate the JSON text
     * @param text The text to check.
     * @throws JSONException If the text is not valid.
     */
    public static void validate(final String text) throws JSONException {
        JSONTokener x = new JSONTokener(text);
        validate(x);
        
        // make sure nothing more is present after last array or object
        char c = x.nextClean();
        if ( c != 0 ) {
            throw x.syntaxError("Unexpected '" + c + "' at end of file.");
        }
    }

    /**
     * Strictly validate the JSON text
     * @param x The tokener to check.
     * @throws JSONException If the text is not valid.
     */
    public static void validate(JSONTokener x) throws JSONException {
        char c = x.nextClean();
        if ( c == 0 ) {
            // no tokens at all - we consider this valid
            return;
        } else  if (c == '[') {
            char nextChar = x.nextClean();
            if (nextChar == ']') {
                return;
            }
            else if (nextChar == 0) {
                throw x.syntaxError("Detected unclosed array.");
            }
            x.back();
            for (;;) {
                if (x.nextClean() == ',') {
                    x.back();
                } else {
                    x.back();
                    c = x.nextClean();
                    x.back();
                    if ( c == '{' || c == '[') {
                        // recursive validation for object and array
                        validate(x);
                    } else {
                        x.nextValue();
                    }
                }
                switch (x.nextClean()) {
                    case ',': if (x.nextClean() == ']') {
                                  throw x.syntaxError("Trailing separator ',' in array.");
                              }
                              x.back();
                              break;
                    case ']': return;
                    default:  throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        } else if ( c == '{') {
            final Set<String> keys = new HashSet<String>();
            for (;;) {
                String key;
                c = x.nextClean();
                switch (c) {
                    case 0  : throw x.syntaxError("A JSONObject text must end with '}'");
                    case '}': return;
                    default : x.back();
                              key = x.nextValue().toString();
                }

                c = x.nextClean();
                if (c != ':') {
                    throw x.syntaxError("Expected a ':' after a key");
                }
                if ( keys.contains(key) ) {
                    throw x.syntaxError("JSONObject contains key '" + key + "' multiple times.");
                }
                keys.add(key);
                // get the value
                c = x.nextClean();
                x.back();
                if ( c == '{' || c == '[') {
                    // recursiv validation for object and array
                    validate(x);
                } else {
                    x.nextValue();
                }

                switch (x.nextClean()) {
                    case ',': if (x.nextClean() == '}') {
                                  throw x.syntaxError("Trailing separator ',' in object.");
                              }
                              x.back();
                              break;
                    case '}': return;
                    default:  throw x.syntaxError("Expected a ',' or '}'");
                }
            }

        } else {
            throw x.syntaxError("A JSON text must begin with '{' (for an object) or '[' (for an array).");
        }
    }
}
