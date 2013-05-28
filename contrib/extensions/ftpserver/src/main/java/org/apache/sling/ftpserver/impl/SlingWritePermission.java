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
package org.apache.sling.ftpserver.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.usermanager.impl.WriteRequest;
import org.apache.sling.api.resource.ResourceResolver;

public class SlingWritePermission implements Authority {

    private final AccessControlManager acm;

    private final Privilege[] write;

    public SlingWritePermission(final ResourceResolver resolver) {
        AccessControlManager acm = null;
        Privilege[] write = null;

        if (resolver != null) {
            try {
                final Session session = resolver.adaptTo(Session.class);
                if (session != null) {
                    acm = session.getAccessControlManager();
                    write = new Privilege[] {
                        acm.privilegeFromName(Privilege.JCR_WRITE)
                    };
                }
            } catch (RepositoryException re) {
                // TODO: log
                acm = null;
                write = null;
            }
        }

        this.acm = acm;
        this.write = write;
    }

    public boolean canAuthorize(final AuthorizationRequest request) {
        return request instanceof WriteRequest;
    }

    public AuthorizationRequest authorize(final AuthorizationRequest request) {
        if ((request instanceof WriteRequest) && this.acm != null) {
            WriteRequest writeRequest = (WriteRequest) request;
            String requestFile = writeRequest.getFile();

            try {
                if (this.acm.hasPrivileges(requestFile, this.write)) {
                    return writeRequest;
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
            }
        }

        return null;
    }

}
