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
package org.apache.sling.auth.core.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.framework.ServiceReference;

class AuthenticationRequirementHolder extends PathBasedHolder {

    private final boolean requiresAuthentication;

    @Nonnull
    static AuthenticationRequirementHolder fromConfig(@Nonnull final String config,
                                                      @Nullable final ServiceReference serviceReference) {
        if (config == null || config.length() == 0) {
            throw new IllegalArgumentException("Configuration must not be null or empty");
        }

        final boolean required;
        final String path;
        if (config.startsWith("+")) {
            required = true;
            path = config.substring(1);
        } else if (config.startsWith("-")) {
            required = false;
            path = config.substring(1);
        } else {
            required = true;
            path = config;
        }

        return new AuthenticationRequirementHolder(path, required,
            serviceReference);
    }

    AuthenticationRequirementHolder(@Nonnull final String fullPath,
                                    final boolean requiresAuthentication,
                                    @Nullable final ServiceReference serviceReference) {
        super(fullPath, serviceReference);
        this.requiresAuthentication = requiresAuthentication;
    }

    boolean requiresAuthentication() {
        return requiresAuthentication;
    }
}
