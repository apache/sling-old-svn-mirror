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
package org.apache.sling.caconfig.resource.spi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Holds reference to a context root resource and configuration reference path
 * that was detected for the configuration context.
 */
@ProviderType
public final class ContextResource {
    
    private final Resource resource;
    private final String configRef;
    private final String key;
    
    /**
     * @param resource Context root resource
     * @param configRef Config reference (normally a resource path).
     *    May be null if the {@link ConfigurationResourceResolvingStrategy} has it's own concept of detecting the matching configuration.
     */
    public ContextResource(@Nonnull Resource resource, String configRef) {
        this.resource = resource;
        this.configRef = configRef;
        this.key = resource.getPath() + "|" + configRef;
    }

    /**
     * @return Context root resource
     */
    public @Nonnull Resource getResource() {
        return resource;
    }

    /**
     * @return Config reference (normally a resource path).
     *    May be null if the {@link ConfigurationResourceResolvingStrategy} has it's own concept of detecting the matching configuration.
     */
    public @CheckForNull String getConfigRef() {
        return configRef;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContextResource) {
            return StringUtils.equals(key, ((ContextResource)obj).key);
        }
        return false;
    }

}
