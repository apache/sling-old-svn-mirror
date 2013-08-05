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
package org.apache.sling.serviceusermapping.impl;

/**
 * The <code>Mapping</code> class defines the mapping of a service's name and
 * optional service information to a user name.
 */
class Mapping {

    private final String serviceName;

    private final String subServiceName;

    private final String userName;

    /**
     * Creates a mapping entry for the entry specification of the form:
     *
     * <pre>
     * spec = serviceName [ ":" subServiceName ] "=" userName .
     * </pre>
     *
     * @param spec The mapping specification.
     * @throws NullPointerException if {@code spec} is {@code null}.
     * @throws IllegalArgumentException if {@code spec} does not match the
     *             expected pattern.
     */
    Mapping(final String spec) {

        final int colon = spec.indexOf(':');
        final int equals = spec.indexOf('=');

        if (colon == 0 || equals <= 0) {
            throw new IllegalArgumentException("serviceName is required");
        } else if (equals == spec.length() - 1) {
            throw new IllegalArgumentException("userName is required");
        } else if (colon + 1 == equals) {
            throw new IllegalArgumentException("serviceInfo must not be empty");
        }

        if (colon < 0 || colon > equals) {
            this.serviceName = spec.substring(0, equals);
            this.subServiceName = null;
        } else {
            this.serviceName = spec.substring(0, colon);
            this.subServiceName = spec.substring(colon + 1, equals);
        }

        this.userName = spec.substring(equals + 1);
    }

    /**
     * Returns the user name if the {@code serviceName} and the
     * {@code serviceInfo} match. Otherwise {@code null} is returned.
     *
     * @param serviceName The name of the service to match. If this is
     *            {@code null} this mapping will not match.
     * @param subServiceName The Subservice Name to match. This may be
     *            {@code null}.
     * @return The user name if this mapping matches or {@code null} otherwise.
     */
    String map(final String serviceName, final String subServiceName) {
        if (this.serviceName.equals(serviceName) && equals(this.subServiceName, subServiceName)) {
            return userName;
        }

        return null;
    }

    private boolean equals(String str1, String str2) {
        return ((str1 == null) ? str2 == null : str1.equals(str2));
    }
}
