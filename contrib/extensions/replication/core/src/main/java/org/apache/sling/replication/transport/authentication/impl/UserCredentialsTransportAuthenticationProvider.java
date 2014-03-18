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
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentialsTransportAuthenticationProvider implements
        TransportAuthenticationProvider<Executor, Executor> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String username;

    private final String password;

    public UserCredentialsTransportAuthenticationProvider(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Executor authenticate(Executor authenticable, TransportAuthenticationContext context)
            throws TransportAuthenticationException {
        ReplicationEndpoint endpoint = context.getAttribute("endpoint",
                ReplicationEndpoint.class);
        if (endpoint != null) {
            Executor authenticated = authenticable.auth(new HttpHost(endpoint
                    .getUri().getHost()), username, password);
            if (log.isInfoEnabled()) {
                log.info("authenticated executor {} with user and password", authenticated);
            }
            return authenticated;
        } else {
            throw new TransportAuthenticationException(
                    "the endpoint to authenticate is missing from the context");
        }
    }

    public boolean canAuthenticate(Class<?> authenticable) {
        return Executor.class.isAssignableFrom(authenticable);
    }

}
