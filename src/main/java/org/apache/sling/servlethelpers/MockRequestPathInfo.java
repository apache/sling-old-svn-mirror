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
package org.apache.sling.servlethelpers;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Mock {@link RequestPathInfo} implementation.
 */
@ConsumerType
public class MockRequestPathInfo implements RequestPathInfo {

    private String extension;
    private String resourcePath;
    private String selectorString;
    private String suffix;

    @Override
    public String getExtension() {
        return this.extension;
    }

    @Override
    public String getResourcePath() {
        return this.resourcePath;
    }

    @Override
    public String[] getSelectors() {
        if (StringUtils.isEmpty(this.selectorString)) {
            return new String[0];
        } else {
            return StringUtils.split(this.selectorString, ".");
        }
    }

    @Override
    public String getSelectorString() {
        return this.selectorString;
    }

    @Override
    public String getSuffix() {
        return this.suffix;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public void setResourcePath(final String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void setSelectorString(final String selectorString) {
        this.selectorString = selectorString;
    }

    public void setSuffix(final String suffix) {
        this.suffix = suffix;
    }

    // --- unsupported operations ---
    @Override
    public Resource getSuffixResource() {
        throw new UnsupportedOperationException();
    }

}
