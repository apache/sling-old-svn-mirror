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

import java.util.Collections;
import java.util.Map;

class PathParser {

    /**
     * List of states. V1 and V2 prefixes means variant 1 and 2. In V1, the parameters are added after
     * selectors and extension: {@code /content/test.sel.html;v=1.0}. In V2 parameters are added before
     * selectors and extension: {@code /content/test;v='1.0'.sel.html}
     */
    private enum ParserState {
        INIT, V1_EXTENSION, V1_PARAMS, V2_PARAMS, V2_EXTENSION, SUFFIX, INVALID
    }

    private String rawPath;

    private String parametersString;

    private Map<String, String> parameters;

    /**
     * @return Path with no parameters.
     */
    public String getPath() {
        return rawPath;
    }

    /**
     * @return Path's substring containing parameters
     */
    public String getParametersString() {
        return parametersString;
    }

    /**
     * @return Parsed parameters.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Parses path containing parameters. Results will be available in {@link #rawPath} and {@link parameters}.
     * 
     * @param path
     */
    public void parse(String path) {
        this.rawPath = path;
        this.parameters = Collections.emptyMap();

        if (path == null) {
            return;
        }

        // indexOf shortcut for the most common case
        final int di = path.indexOf('.');
        final int si = path.indexOf(';');
        if (di == -1 && si == -1) {
            return;
        }

        final char[] chars = path.toCharArray();
        final ParametersParser parametersParser = new ParametersParser();

        ParserState state = ParserState.INIT;
        int paramsStart = -1, paramsEnd = -1;

        int i = (di != -1) ? ((si != -1) ? Math.min(di, si) : di) : si;
        for (; i <= chars.length; i++) {
            final char c;
            if (i == chars.length) {
                c = 0;
            } else {
                c = chars[i];
            }

            switch (state) {
                case INIT:
                    if (c == '.') {
                        state = ParserState.V1_EXTENSION;
                    } else if (c == ';') {
                        paramsStart = i;
                        i = parametersParser.parseParameters(chars, i, false);
                        paramsEnd = i--;
                        state = parametersParser.isInvalid() ? ParserState.INVALID : ParserState.V2_PARAMS;
                    }
                    break;

                case V1_EXTENSION:
                    if (c == '/') {
                        state = ParserState.SUFFIX;
                    } else if (c == ';') {
                        paramsStart = i;
                        i = parametersParser.parseParameters(chars, i, true);
                        paramsEnd = i--;
                        state = parametersParser.isInvalid() ? ParserState.INVALID : ParserState.V1_PARAMS;
                    }
                    break;

                case V1_PARAMS:
                    if (c == '/') {
                        state = ParserState.SUFFIX;
                    } else if (c == '.') {
                        state = ParserState.INVALID; // no dots after params
                    }
                    break;

                case V2_PARAMS:
                    if (c == '/') {
                        state = ParserState.INVALID; // there was no extension, so no suffix is allowed
                    } else if (c == '.') {
                        state = ParserState.V2_EXTENSION;
                    }
                    break;

                case V2_EXTENSION:
                    if (c == '/') {
                        state = ParserState.SUFFIX;
                    }
                    break;

                case SUFFIX:
                case INVALID:
                    break;
            }
        }

        if (state == ParserState.INVALID) {
            paramsStart = paramsEnd = -1;
        } else {
            cutPath(path, paramsStart, paramsEnd);
            parameters = parametersParser.getParameters();
        }
    }

    private void cutPath(String path, int from, int to) {
        if (from == -1) {
            rawPath = path;
            parametersString = null;
        } else if (to == -1) {
            rawPath = path.substring(0, from);
            parametersString = path.substring(from);
        } else {
            rawPath = path.substring(0, from) + path.substring(to);
            parametersString = path.substring(from, to);
        }
    }


}
