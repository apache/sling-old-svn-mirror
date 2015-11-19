/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.sling.resourceresolver.impl.params;

import java.util.Collections;
import java.util.Map;

/**
 * Parses path looking for semicolon-separated parameters. Parameters are extracted and exposed as an
 * immutable map. The path without parameters is available as raw path.
 * 
 * Parameters should be added immedietaly before or after selectors and extension:
 * {@code /content/test;v='1.0'.html} or {@code /content/test.html;v=1.0}. Quotes can be used to escape the
 * parameter value (it is necessary if the value contains dot and parameter is added before extension).
 */
public class ParsedParameters {

    private final Map<String, String> parameters;

    private final String parametersString;

    private final String path;

    /**
     * Parse path and create parameters object.
     * 
     * @param fullPath Path to parse.
     */
    public ParsedParameters(final String fullPath) {
        final PathParser parser = new PathParser();
        parser.parse(fullPath);

        parametersString = parser.getParametersString();
        parameters = Collections.unmodifiableMap(parser.getParameters());
        path = parser.getPath();
    }

    /**
     * @return Path with no parameters.
     */
    public String getRawPath() {
        return path;
    }
    
    /**
     * @return Path's substring containing parameters
     */
    public String getParametersString() {
        return parametersString;
    }

    /**
     * @return Map of the parameters.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

}
