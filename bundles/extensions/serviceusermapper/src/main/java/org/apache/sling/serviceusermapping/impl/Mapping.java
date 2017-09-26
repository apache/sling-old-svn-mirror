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

import java.util.HashSet;
import java.util.Set;

/**
 * The <code>Mapping</code> class defines the mapping of a service's name and
 * optional service information to a user name and optionally to a set of principal names.
 */
class Mapping implements Comparable<Mapping> {


    /**
     * The name of the osgi property holding the service name.
     */
    static String SERVICENAME = ".serviceName";

    private final String serviceName;

    private final String subServiceName;

    private final String userName;

    private final Set<String> principalNames;

    /**
     * Creates a mapping entry for the entry specification of the form:
     *
     * <pre>
     * spec = serviceName [ ":" subServiceName ] "=" userName | "[" principalNames "]"
     * principalNames = principalName ["," principalNames]
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
            throw new IllegalArgumentException("userName or principalNames is required");
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

        String s = spec.substring(equals + 1);
        if (s.charAt(0) == '[' && s.charAt(s.length()-1) == ']') {
            this.userName = null;
            this.principalNames = extractPrincipalNames(s);
        } else {
            this.userName = s;
            this.principalNames = null;
        }
    }

    static Set<String> extractPrincipalNames(String s) {
        String[] sArr = s.substring(1, s.length() - 1).split(",");
        Set<String> set = new HashSet<>();
        for (String name : sArr) {
            String n = name.trim();
            if (!n.isEmpty()) {
                set.add(n);
            }
        }
        return set;
    }

    /**
     * Returns the user name if the {@code serviceName} and the
     * {@code serviceInfo} match and a single user name is configured (in contrast
     * to a set of principal names). Otherwise {@code null} is returned.
     *
     * @param serviceName The name of the service to match. If this is
     *            {@code null} this mapping will not match.
     * @param subServiceName The Subservice Name to match. This may be
     *            {@code null}.
     * @return The user name if this mapping matches and the configuration doesn't specify a set of principal names; {@code null} otherwise.
     */
    String map(final String serviceName, final String subServiceName) {
        if (this.serviceName.equals(serviceName) && equals(this.subServiceName, subServiceName)) {
            return userName;
        }

        return null;
    }

    /**
     * Returns the principal names if the {@code serviceName} and the
     * {@code serviceInfo} match and principal names have been configured.
     * Otherwise {@code null} is returned. If no principal names are configured
     * {@link #map(String, String)} needs to be used instead.
     *
     * @param serviceName The name of the service to match. If this is
     *            {@code null} this mapping will not match.
     * @param subServiceName The Subservice Name to match. This may be
     *            {@code null}.
     * @return An iterable of principals names this mapping matches and the configuration
     * does specify a set of principal names (intstead of a single user name); {@code null}
     * otherwise.
     */
    Iterable<String> mapPrincipals(final String serviceName, final String subServiceName) {
        if (this.serviceName.equals(serviceName) && equals(this.subServiceName, subServiceName)) {
            return principalNames;
        }

        return null;
    }

    private boolean equals(String str1, String str2) {
        return ((str1 == null) ? str2 == null : str1.equals(str2));
    }

    @Override
    public String toString() {
        String name = (userName != null) ? "userName=" + userName : "principleNames" + principalNames.toString();
        return "Mapping [serviceName=" + serviceName + ", subServiceName="
                + subServiceName + ", " + name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getSubServiceName() {
        return subServiceName;
    }


    public int compareTo(Mapping o) {
        if (o == null) {
            return -1;
        }

        int result = compare(this.serviceName, o.serviceName);
        if (result == 0) {
            result = compare(this.subServiceName, o.subServiceName);
        }
        return result;
    }

    private int compare(String str1, String str2) {
        if (str1 == str2) {
            return 0;
        }

        if (str1 == null) {
            return -1;
        }

        if (str2 == null) {
            return 1;
        }

        return str1.hashCode() - str2.hashCode();
    }
}
