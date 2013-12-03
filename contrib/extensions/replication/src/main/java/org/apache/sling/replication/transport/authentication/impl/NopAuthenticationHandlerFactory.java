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

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationHandlerFactory;


@Component(immediate = true)
@Service(value = AuthenticationHandlerFactory.class)
@Property(name = "name", value = NopAuthenticationHandlerFactory.TYPE)
public class NopAuthenticationHandlerFactory implements AuthenticationHandlerFactory {
    public static final String TYPE = "nop";

    private static final AuthenticationHandler<Object, Object> nopAuthenticationHandler = new NopAuthenticationHandler();

    public AuthenticationHandler<Object, Object> createAuthenticationHandler(Map<String, String> properties) {
        return nopAuthenticationHandler;
    }

    public String getType() {
        return TYPE;
    }

}
