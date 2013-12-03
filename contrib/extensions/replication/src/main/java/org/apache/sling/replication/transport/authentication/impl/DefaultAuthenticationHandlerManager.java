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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationHandlerFactory;
import org.apache.sling.replication.transport.authentication.AuthenticationHandlerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@References({ 
    @Reference(name = "authenticationHandlerFactory", 
                    referenceInterface = AuthenticationHandlerFactory.class,
                    cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, 
                    policy = ReferencePolicy.DYNAMIC, 
                    bind = "bindAuthenticationHandlerFactory", 
                    unbind = "unbindAuthenticationHandlerFactory")
    })
public class DefaultAuthenticationHandlerManager implements AuthenticationHandlerProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, AuthenticationHandlerFactory> authenticationHandlerFactories = new HashMap<String, AuthenticationHandlerFactory>();

    @Deactivate
    protected void deactivate() {
        authenticationHandlerFactories.clear();
    }

    public AuthenticationHandler<?, ?> getAuthenticationHandler(String type,
                    Map<String, String> properties) {
        AuthenticationHandler<?, ?> authenticationHandler = null;
        AuthenticationHandlerFactory authenticationHandlerFactory = authenticationHandlerFactories
                        .get(type);
        if (authenticationHandlerFactory != null) {
            authenticationHandler = authenticationHandlerFactory
                            .createAuthenticationHandler(properties);
        }
        return authenticationHandler;
    }

    public void bindAuthenticationHandlerFactory(
                    AuthenticationHandlerFactory authenticationHandlerFactory) {
        synchronized (authenticationHandlerFactories) {
            authenticationHandlerFactories.put(authenticationHandlerFactory.getType(),
                            authenticationHandlerFactory);
        }
        if (log.isInfoEnabled()) {
            log.info("Registered AuthenticationHandlerFactory {}", authenticationHandlerFactory);
        }
    }

    public void unbindAuthenticationHandlerFactory(
                    AuthenticationHandlerFactory authenticationHandlerFactory) {
        synchronized (authenticationHandlerFactories) {
            authenticationHandlerFactories.remove(authenticationHandlerFactory.getType());
        }
        if (log.isInfoEnabled()) {
            log.info("Unregistered AuthenticationHandlerFactory {}", authenticationHandlerFactory);
        }
    }

}
