package org.apache.sling.resourceresolver.impl.tree.params;

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

    public Map<String, String> getParameters() {
        return parameters;
    }

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
