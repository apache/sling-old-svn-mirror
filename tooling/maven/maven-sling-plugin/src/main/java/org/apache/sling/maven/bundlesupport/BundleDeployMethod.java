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
package org.apache.sling.maven.bundlesupport;

/**
 * Possible/Recognized Methodologies for deploying (installing and uninstalling)
 * bundles from the remote server.
 */
enum BundleDeployMethod {

    /** Via POST to Sling directly */
    POST_SERVLET("postServlet"),

    /** Via POST to Felix Web Console */
    WEBCONSOLE("webconsole"),

    /** Via WebDAV */
    WEBDAV("webdav");

    /** String value to utilize/recognize for the enum value */
    final String value;

    private BundleDeployMethod(String value) {
        this.value = value;
    }

    /**
     * Retrieve the BundleDeployMethod matching the specified value
     * @param value value to retrieve its matching BundleDeployMethod
     * @return matching BundleDeployMethod for the specified value. <code>null</code> if no match was found.
     */
    public static BundleDeployMethod fromValue(String value) {
        for (BundleDeployMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return null;
    }
}
