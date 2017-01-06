/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.instance;

import java.net.URI;

/**
 * Configuration of a single instance instance.
 */
public class InstanceConfiguration {

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private URI url;
    private final String runmode;
    private String adminUser;
    private String adminPassword;

    public InstanceConfiguration(final URI url, final String runmode, String adminUser, String adminPassword) {
        this.url = url;
        this.runmode = runmode;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    public InstanceConfiguration(URI url, String runmode) {
        this(url, runmode, DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD);
    }

    public URI getUrl() {
        return this.url;
    }

    public String getRunmode() {
        return runmode;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
}