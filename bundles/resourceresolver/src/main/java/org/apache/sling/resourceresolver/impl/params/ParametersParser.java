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

package org.apache.sling.resourceresolver.impl.params;

import java.util.LinkedHashMap;
import java.util.Map;

class ParametersParser {

    private enum ParamsState {
        INIT, NAME, EQUALS, VALUE, QUOTED_VALUE, QUOTE_END
    }

    private StringBuilder name;

    private StringBuilder value;

    private Map<String, String> parameters = new LinkedHashMap<String, String>();

    private boolean invalid;

    /**
     * Parses parameters string, eg.: {@code ;x=123;a='1.0'}. The result of the method is available in
     * {@link #parameters} and {@link #invalid}.
     * 
     * @param chars Array containing path with parameters.
     * @param from Index of the first character of the parameters substring (it must be a semicolon).
     * @param dotAllowed If true, the dot in parameter value won't stop parsing.
     * @return Index of the first character not related to parameters.
     */
    public int parseParameters(final char[] chars, final int from, final boolean dotAllowed) {
        resetCurrentParameter();
        parameters.clear();
        invalid = false;

        ParamsState state = ParamsState.INIT;
        for (int i = from; i <= chars.length; i++) {
            final char c;
            if (i == chars.length) {
                c = 0;
            } else {
                c = chars[i];
            }
            switch (state) {
                case INIT:
                    if (c == ';') {
                        state = ParamsState.NAME;
                    } else if (c == '.' || c == '/' || c == 0) {
                        invalid = true;
                        return i;
                    }
                    break;

                case NAME:
                    if (c == '=') {
                        state = ParamsState.EQUALS;
                    } else if (c == '.' || c == '/' || c == 0) {
                        invalid = true;
                        return i;
                    } else if (c == ';') {
                        resetCurrentParameter();
                    } else {
                        name.append(c);
                    }
                    break;

                case EQUALS:
                    if (c == '\'') {
                        state = ParamsState.QUOTED_VALUE;
                    } else if (c == '.' || c == '/' || c == 0) {
                        addParameter(); // empty one
                        return i;
                    } else if (c == ';') {
                        state = ParamsState.NAME; // empty one
                        addParameter();
                    } else {
                        state = ParamsState.VALUE;
                        value.append(c);
                    }
                    break;

                case QUOTED_VALUE:
                    if (c == '\'') {
                        state = ParamsState.QUOTE_END;
                        addParameter();
                    } else if (c == 0) {
                        invalid = true;
                        return i;
                    } else {
                        value.append(c);
                    }
                    break;

                case VALUE:
                    if (c == ';') {
                        state = ParamsState.NAME;
                        addParameter();
                    } else if ((c == '.' && !dotAllowed) || c == '/' || c == 0) {
                        addParameter();
                        return i;
                    } else {
                        value.append(c);
                    }
                    break;

                case QUOTE_END:
                    if (c == ';') {
                        state = ParamsState.NAME;
                    } else {
                        return i;
                    }
            }
        }

        return chars.length;
    }

    /**
     * @return Parsed parameters.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @return True if the {@link #parseParameters(char[], int, boolean)} method failed.
     */
    public boolean isInvalid() {
        return invalid;
    }

    private void resetCurrentParameter() {
        name = new StringBuilder();
        value = new StringBuilder();
    }

    private void addParameter() {
        parameters.put(name.toString(), value.toString());
        name = new StringBuilder();
        value = new StringBuilder();
    }
}
