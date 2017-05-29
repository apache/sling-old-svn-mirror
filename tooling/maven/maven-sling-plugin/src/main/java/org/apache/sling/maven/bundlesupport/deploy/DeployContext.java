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
package org.apache.sling.maven.bundlesupport.deploy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.maven.plugin.logging.Log;

public final class DeployContext {
    
    private Log log;
    private HttpClient httpClient;
    private boolean failOnError = true;
    private String bundleStartLevel = "20";
    private boolean bundleStart = true;
    private String mimeType = "application/java-archive";
    private boolean refreshPackages = true;
    
    public Log getLog() {
        return log;
    }
    public DeployContext log(Log log) {
        this.log = log;
        return this;
    }
    public HttpClient getHttpClient() {
        return httpClient;
    }
    public DeployContext httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }
    public boolean isFailOnError() {
        return failOnError;
    }
    public DeployContext failOnError(boolean failOnError) {
        this.failOnError = failOnError;
        return this;
    }
    public String getBundleStartLevel() {
        return bundleStartLevel;
    }
    public DeployContext bundleStartLevel(String bundleStartLevel) {
        this.bundleStartLevel = bundleStartLevel;
        return this;
    }
    public boolean isBundleStart() {
        return bundleStart;
    }
    public DeployContext bundleStart(boolean bundleStart) {
        this.bundleStart = bundleStart;
        return this;
    }
    public String getMimeType() {
        return mimeType;
    }
    public DeployContext mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }
    public boolean isRefreshPackages() {
        return refreshPackages;
    }
    public DeployContext refreshPackages(boolean refreshPackages) {
        this.refreshPackages = refreshPackages;
        return this;
    }

}
