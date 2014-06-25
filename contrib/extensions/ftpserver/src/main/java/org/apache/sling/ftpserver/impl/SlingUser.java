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
package org.apache.sling.ftpserver.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.sling.api.resource.ResourceResolver;

public class SlingUser implements User {

    private final ResourceResolver resolver;

    private final String name;

    private int maxIdleTimeSec = SlingConfiguration.PROP_MAX_IDLE_TIME_DEFAULT;

    private boolean isEnabled = SlingConfiguration.PROP_ENABLED_DEFAULT;

    private List<Authority> authorities;

    private String homeDirectory = SlingConfiguration.PROP_FTP_HOME_DEFAULT;

    SlingUser(final String name, final ResourceResolver resolver) {
        this.resolver = resolver;
        this.name = name;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    public AuthorizationRequest authorize(AuthorizationRequest request) {
        // check for no authorities at all
        if (authorities == null) {
            return null;
        }

        boolean someoneCouldAuthorize = false;
        for (Authority authority : authorities) {
            if (authority.canAuthorize(request)) {
                someoneCouldAuthorize = true;

                request = authority.authorize(request);

                // authorization failed, return null
                if (request == null) {
                    return null;
                }
            }

        }

        if (someoneCouldAuthorize) {
            return request;
        }

        return null;
    }

    public List<Authority> getAuthorities() {
        return this.authorities;
    }

    public void setAuthorities(List<Authority> authorities) {
        if (authorities != null) {
            this.authorities = Collections.unmodifiableList(authorities);
        } else {
            this.authorities = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
        List<Authority> selected = new ArrayList<Authority>();

        for (Authority authority : authorities) {
            if (authority.getClass().equals(clazz)) {
                selected.add(authority);
            }
        }

        return selected;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean getEnabled() {
        return this.isEnabled;
    }

    void setHomeDirectory(final String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public void setMaxIdleTimeSec(int maxIdleTimeSec) {
        this.maxIdleTimeSec = maxIdleTimeSec;
    }

    public int getMaxIdleTime() {
        return this.maxIdleTimeSec;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Always returns {@code null} since the password cannot be read from the
     * repository.
     */
    public String getPassword() {
        return null;
    }
}
