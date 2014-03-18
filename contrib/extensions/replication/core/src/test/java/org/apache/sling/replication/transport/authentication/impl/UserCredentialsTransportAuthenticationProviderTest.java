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

import org.apache.http.client.fluent.Executor;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Testcase for {@link UserCredentialsTransportAuthenticationProvider}
 */
public class UserCredentialsTransportAuthenticationProviderTest {

    @Test
    public void testAuthenticationWithNullAuthenticableAndContext() throws Exception {
        UserCredentialsTransportAuthenticationProvider authenticationHandler = new UserCredentialsTransportAuthenticationProvider(
                        "foo", "bar");
        Executor authenticable = null;
        TransportAuthenticationContext context = null;
        try {
            authenticationHandler.authenticate(authenticable, context);
            fail("could not authenticate a null authenticable");
        } catch (Exception e) {
            // expected to fail
        }
    }

    @Test
    public void testAuthenticationWithAuthenticableAndNullContext() throws Exception {
        UserCredentialsTransportAuthenticationProvider authenticationHandler = new UserCredentialsTransportAuthenticationProvider(
                        "foo", "bar");
        Executor authenticable = Executor.newInstance();
        TransportAuthenticationContext context = null;
        try {
            authenticationHandler.authenticate(authenticable, context);
            fail("could not authenticate with a null context");
        } catch (Exception e) {
            // expected to fail
        }
    }

    @Test
    public void testAuthenticationWithAuthenticableAndEmptyContext() throws Exception {
        UserCredentialsTransportAuthenticationProvider authenticationHandler = new UserCredentialsTransportAuthenticationProvider(
                        "foo", "bar");
        Executor authenticable = Executor.newInstance();
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        try {
            authenticationHandler.authenticate(authenticable, context);
            fail("could not authenticate with an empty context");
        } catch (Exception e) {
            // expected to fail
        }
    }

    @Test
    public void testAuthenticationWithAuthenticableAndCorrectContext() throws Exception {
        UserCredentialsTransportAuthenticationProvider authenticationHandler = new UserCredentialsTransportAuthenticationProvider(
                        "foo", "bar");
        Executor authenticable = Executor.newInstance();
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        context.addAttribute("endpoint", new ReplicationEndpoint("http://www.apache.org"));
        authenticationHandler.authenticate(authenticable, context);
    }
}
