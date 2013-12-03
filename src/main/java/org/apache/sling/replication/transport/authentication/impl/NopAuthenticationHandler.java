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

import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationContext;
import org.apache.sling.replication.transport.authentication.AuthenticationException;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;

/**
 * {@link AuthenticationHandler} used when no authentication is required
 */
public class NopAuthenticationHandler implements AuthenticationHandler<Object, Object> {

    public boolean supportsTransportHandler(TransportHandler transportHandler) {
        return true;
    }

    public Object authenticate(Object authenticable, AuthenticationContext context)
                    throws AuthenticationException {
        return authenticable;
    }

    public boolean canAuthenticate(Class<?> authenticable) {
        return true;
    }

}
