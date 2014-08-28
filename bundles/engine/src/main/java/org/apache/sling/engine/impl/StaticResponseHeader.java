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
package org.apache.sling.engine.impl;

/**
 * The <code>Mapping</code> class defines the mapping of a optional additional 
 * response headers
 */
public class StaticResponseHeader {

    private final String responseHeaderName;

    private final String responseHeaderValue;

    /**
     * Creates a mapping entry for the entry specification of the form:
     *
     * <pre>
     * spec = responseHeaderName "=" responseHeaderValue .
     * </pre>
     *
     * @param spec The mapping specification.
     * @throws NullPointerException if {@code spec} is {@code null}.
     * @throws IllegalArgumentException if {@code spec} does not match the
     *             expected pattern.
     */
    StaticResponseHeader(final String spec) {

        if (spec.length() == 0) {
            throw new IllegalArgumentException("responseHeader must not be empty");
        }

        final int equals = spec.indexOf('=');

        if (equals <= 0) {
            throw new IllegalArgumentException("responseHeaderName is required");
        } else if (equals == spec.length() - 1) {
            throw new IllegalArgumentException("responseHeaderValue is required");
        }

        this.responseHeaderName = spec.substring(0, equals);
        this.responseHeaderValue = spec.substring(equals + 1);
    }

    public String getResponseHeaderName() {
        return responseHeaderName;
    }

    public String getResponseHeaderValue() {
        return responseHeaderValue;
    } 
}
