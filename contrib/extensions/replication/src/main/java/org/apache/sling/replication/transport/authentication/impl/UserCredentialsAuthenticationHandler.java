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
package org.apache.sling.replication.transport.authentication.impl;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.transport.authentication.AuthenticationContext;
import org.apache.sling.replication.transport.authentication.AuthenticationException;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentialsAuthenticationHandler implements
        AuthenticationHandler<Executor, Executor> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private String username;

    private String password;

    public UserCredentialsAuthenticationHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Executor authenticate(Executor authenticable, AuthenticationContext context)
                    throws AuthenticationException {
        if (authenticable instanceof Executor) {
            ReplicationEndpoint endpoint = context.getAttribute("endpoint",
                            ReplicationEndpoint.class);
            if (endpoint != null) {
                Executor authenticated = ((Executor) authenticable).auth(new HttpHost(endpoint
                                .getUri().getHost()), username, password);
                if (log.isInfoEnabled()) {
                    log.info("authenticated executor {} with user and password", authenticated);
                }
                return authenticated;
            } else {
                throw new AuthenticationException(
                                "the endpoint to authenticate is missing from the context");
            }
        } else {
            throw new AuthenticationException("could not authenticate a " + authenticable);
        }
    }

    public boolean canAuthenticate(Class<?> authenticable) {
        return Executor.class.isAssignableFrom(authenticable);
    }

}
