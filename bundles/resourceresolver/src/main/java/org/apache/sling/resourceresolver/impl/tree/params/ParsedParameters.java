package org.apache.sling.resourceresolver.impl.tree.params;

import java.util.Collections;
import java.util.Map;

public class ParsedParameters {

    private final Map<String, String> parameters;

    private final String path;

    public ParsedParameters(final String fullPath) {
        final PathParser parser = new PathParser();
        parser.parse(fullPath);

        parameters = Collections.unmodifiableMap(parser.getParameters());
        path = parser.getPath();
    }

    public String getRawPath() {
        return path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
