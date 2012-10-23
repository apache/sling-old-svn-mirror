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

package org.apache.sling.jcr.jackrabbit.base.security;

import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;

public class DelegatingPrincipalProviderRegistry implements PrincipalProviderRegistry {
    private final PrincipalProviderRegistry defaultRegistry;
    private final PrincipalProviderRegistry osgiRegistry;

    public DelegatingPrincipalProviderRegistry(PrincipalProviderRegistry defaultRegistry,
                                                PrincipalProviderRegistry osgiRegistry) {
        this.defaultRegistry = defaultRegistry;
        this.osgiRegistry = osgiRegistry;
    }

    public PrincipalProvider registerProvider(Properties properties) throws RepositoryException {
        return defaultRegistry.registerProvider(properties);
    }

    public PrincipalProvider getDefault() {
        return defaultRegistry.getDefault();
    }

    public PrincipalProvider getProvider(String name) {
        PrincipalProvider p = defaultRegistry.getProvider(name);
        if (p == null) {
            p = osgiRegistry.getProvider(name);
        }
        return p;
    }

    public PrincipalProvider[] getProviders() {
        PrincipalProvider[] defaultProviders = defaultRegistry.getProviders();
        PrincipalProvider[] extraProviders = osgiRegistry.getProviders();

        //Quick check
        if(extraProviders.length == 0){
            return defaultProviders;
        }

        PrincipalProvider[] mergedResult = new PrincipalProvider[defaultProviders.length + extraProviders.length];
        System.arraycopy(defaultProviders, 0, mergedResult, 0, defaultProviders.length);
        System.arraycopy(extraProviders, 0, mergedResult, defaultProviders.length, extraProviders.length);
        return mergedResult;
    }

}