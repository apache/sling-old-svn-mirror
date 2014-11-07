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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.distribution.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationException;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTransportAuthenticationProvider implements TransportAuthenticationProvider<SlingRepository, Session> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String serviceName;

    public RepositoryTransportAuthenticationProvider(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean canAuthenticate(Class authenticable) {
        return SlingRepository.class.isAssignableFrom(authenticable);
    }

    public Session authenticate(SlingRepository authenticable, TransportAuthenticationContext context)
            throws TransportAuthenticationException {
        String path = context.getAttribute("path", String.class);
        String privilege = context.getAttribute("privilege", String.class);

        if (path == null) {
            throw new TransportAuthenticationException(
                    "the path to authenticate is missing from the context");
        }

        Session session = null;
        try {
            session = authenticable.loginService(serviceName, null);

            if (session == null) {
                throw new TransportAuthenticationException("failed to authenticate" + path);
            }
            if (!session.hasPermission(path, privilege)) {
                session.logout();
                throw new TransportAuthenticationException("failed to access path " + path + " with privilege " + privilege);
            }

            log.info("authenticated path {} with privilege {}", path, privilege);
            return session;
        } catch (RepositoryException re) {
            if (session != null) {
                session.logout();
            }
            throw new TransportAuthenticationException(re);
        }
    }

}
