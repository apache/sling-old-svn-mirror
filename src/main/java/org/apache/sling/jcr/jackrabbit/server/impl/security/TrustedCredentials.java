/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.jcr.jackrabbit.server.impl.security;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.security.authentication.Authentication;

/**
 *
 */
public abstract class TrustedCredentials implements Credentials {

    /**
     *
     */
    private static final long serialVersionUID = 5153578149776402602L;

    private Principal principal;

    private Authentication authentication;

    /**
     * @param userId
     */
    public TrustedCredentials(final String userId) {
        principal = getPrincipal(userId);
        authentication = new Authentication() {

            public boolean canHandle(Credentials credentials) {
                return (credentials instanceof AdministrativeCredentials)
                        || (credentials instanceof AnonCredentials);
            }

            public boolean authenticate(Credentials credentials)
                    throws RepositoryException {
                return (credentials instanceof AdministrativeCredentials)
                        || (credentials instanceof AnonCredentials);
            }
        };
    }

    /**
     * @param userId
     * @return
     */
    protected abstract Principal getPrincipal(String userId);

    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @return
     */
    public Authentication getTrustedAuthentication() {
        return authentication;
    }

    /**
     * @return null
     */
    public Object getImpersonator() {
        return null;
    }

}
