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
package org.apache.sling.distribution.transport.authentication.impl;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationException;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentialsTransportAuthenticationProvider implements
        TransportAuthenticationProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String username;

    private final String password;

    public UserCredentialsTransportAuthenticationProvider(String username, String password) {
        if (username.length() == 0 || password.length() == 0) {
            throw new IllegalArgumentException("Username and password are required");
        }

        this.username = username;
        this.password = password;
    }

    public Object authenticate(Object authenticable, TransportAuthenticationContext context) throws TransportAuthenticationException {

        DistributionEndpoint endpoint = context.getAttribute("endpoint", DistributionEndpoint.class);

        if (endpoint == null) {
            throw new TransportAuthenticationException("the endpoint to authenticate is missing from the context");
        }

        if (authenticable instanceof Executor) {
            Executor executor = (Executor) authenticable;

            Executor authenticated = executor.auth(new HttpHost(endpoint.getUri().getHost(), endpoint.getUri().getPort()),
                    username, password).authPreemptive(
                        new HttpHost(endpoint.getUri().getHost(), endpoint.getUri().getPort()));
            log.debug("authenticated executor HTTP client with user and password");
            return authenticated;

        } else if (authenticable instanceof CredentialsProvider) {
            CredentialsProvider credentialsProvider = (CredentialsProvider) authenticable;
            credentialsProvider.setCredentials(new AuthScope(new HttpHost(endpoint.getUri().getHost(), endpoint.getUri().getPort())),
                    new UsernamePasswordCredentials(username, password));

            log.debug("authenticated CredentialsProvider HTTP client with user and password");
            return credentialsProvider;
        }

        return null;
    }

    public boolean canAuthenticate(Class authenticable) {
        return Executor.class.isAssignableFrom(authenticable) || CredentialsProvider.class.isAssignableFrom(authenticable);
    }

}
