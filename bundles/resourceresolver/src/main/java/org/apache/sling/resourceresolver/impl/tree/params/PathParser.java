package org.apache.sling.resourceresolver.impl.tree.params;

import java.util.Collections;
import java.util.Map;

public class PathParser {

    private enum ParserState {
        // initial state, before semicolon or dot
        INIT,
        // parameters have been parsed, waiting for dot marking extension
        PARSED_PARAMS_WAITING_FOR_EXTENSION,
        // parameters have been already parsed, parsing extension,
        PARSED_PARAMS_PARSING_EXT,
        // parsing extension, there have been no parameters yet
        PARSING_EXTENSION_NO_PARAMS,
        // extension and parameters parsed, waiting for suffix
        PARSED_EXT_AND_PARAMS,
        // parsing suffix
        SUFFIX,
        // illegal path, parsing cancelled
        INVALID
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
                        state = ParserState.PARSING_EXTENSION_NO_PARAMS;
                    } else if (c == ';') {
                        paramsStart = i;
                        i = parametersParser.parseParameters(chars, i, false);
                        paramsEnd = i--;
                        state = parametersParser.isInvalid() ? ParserState.INVALID
                                : ParserState.PARSED_PARAMS_WAITING_FOR_EXTENSION;
                    }
                    break;

                case PARSING_EXTENSION_NO_PARAMS:
                    if (c == '/') {
                        state = ParserState.SUFFIX;
                    } else if (c == ';') {
                        paramsStart = i;
                        i = parametersParser.parseParameters(chars, i, true);
                        paramsEnd = i--;
                        state = parametersParser.isInvalid() ? ParserState.INVALID
                                : ParserState.PARSED_EXT_AND_PARAMS;
                    }
                    break;

                case PARSED_EXT_AND_PARAMS:
                    if (c == '/') {
                        state = ParserState.SUFFIX;
                    } else if (c == '.') {
                        state = ParserState.INVALID; // no dots after params
                    }
                    break;

                case PARSED_PARAMS_WAITING_FOR_EXTENSION:
                    if (c == '/') {
                        state = ParserState.INVALID; // there was no extension, so no suffix is allowed
                    } else if (c == '.') {
                        state = ParserState.PARSED_PARAMS_PARSING_EXT;
                    }
                    break;

                case PARSED_PARAMS_PARSING_EXT:
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
