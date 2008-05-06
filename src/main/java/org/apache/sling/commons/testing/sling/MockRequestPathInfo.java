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
package org.apache.sling.commons.testing.sling;

import org.apache.sling.api.request.RequestPathInfo;

class MockRequestPathInfo implements RequestPathInfo {

    private final String selectors;

    private final String extension;

    private final String suffix;

    public MockRequestPathInfo(String selectors, String extension, String suffix) {
        this.selectors = selectors;
        this.extension = extension;
        this.suffix = suffix;
    }

    public String getExtension() {
        return extension;
    }

    public String getResourcePath() {
        return null;
    }

    public String getSelectorString() {
        return selectors;
    }

    public String[] getSelectors() {
        return (getSelectorString() != null)
                ? getSelectorString().split("\\.")
                : new String[0];
    }

    public String getSuffix() {
        return suffix;
    }
}