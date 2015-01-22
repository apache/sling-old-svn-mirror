package org.apache.sling.resourceresolver.impl.tree.params;

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

    private Map<String, String> parameters;

    private String rawPath;

    public String getPath() {
        return rawPath;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void parse(String path) {
        this.rawPath = path;
        this.parameters = Collections.emptyMap();

        if (path == null) {
            return;
        }

        final char[] chars = path.toCharArray();
        final ParametersParser parametersParser = new ParametersParser();

        ParserState state = ParserState.INIT;
        int paramsStart = -1, paramsEnd = -1;

        for (int i = 0; i <= chars.length; i++) {
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
            rawPath = cut(path, paramsStart, paramsEnd);
            parameters = parametersParser.getParameters();
        }
    }

    private static String cut(String string, int from, int to) {
        if (from == -1) {
            return string;
        } else if (to == -1) {
            return string.substring(0, from);
        } else {
            return string.substring(0, from) + string.substring(to, string.length());
        }
    }

}
