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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTransportAuthenticationProvider implements TransportAuthenticationProvider<SlingRepository, Session> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Credentials credentials;

    public RepositoryTransportAuthenticationProvider(String user, String password) {
        this.credentials = new SimpleCredentials(user, password.toCharArray());
    }

    public boolean canAuthenticate(Class<?> authenticable) {
        return SlingRepository.class.isAssignableFrom(authenticable);
    }

    public Session authenticate(SlingRepository authenticable, TransportAuthenticationContext context)
            throws TransportAuthenticationException {
        String path = context.getAttribute("path", String.class);
        if (path != null) {
            Session session = null;
            try {
                session = authenticable.login(credentials);
                if (!session.nodeExists(path)) {
                    throw new TransportAuthenticationException("failed to read path " + path);
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("authenticated path {} ", path);
                    }
                    return session;
                }
            } catch (RepositoryException re) {
                if (session != null) {
                    session.logout();
                }
                throw new TransportAuthenticationException(re);
            }
        } else {
            throw new TransportAuthenticationException(
                    "the path to authenticate is missing from the context");
        }
    }

}
