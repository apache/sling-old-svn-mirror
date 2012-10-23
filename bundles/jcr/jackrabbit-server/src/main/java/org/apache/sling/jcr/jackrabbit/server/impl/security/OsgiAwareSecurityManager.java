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

package org.apache.sling.jcr.jackrabbit.server.impl.security;

import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.sling.jcr.jackrabbit.base.security.DelegatingPrincipalProviderRegistry;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;

/**
 * User: chetanm
 * Date: 16/10/12
 * Time: 7:57 PM
 */
public class OsgiAwareSecurityManager extends DefaultSecurityManager {

    @Override
    protected PrincipalProviderRegistry getPrincipalProviderRegistry() {
        PrincipalProviderRegistry defaultRegistry =  super.getPrincipalProviderRegistry();
        PrincipalProviderRegistry osgiRegistry = Activator.getPrincipalProviderTracker();
        if(osgiRegistry != null){
            return new DelegatingPrincipalProviderRegistry(defaultRegistry,osgiRegistry);
        }
        return defaultRegistry;
    }

}
