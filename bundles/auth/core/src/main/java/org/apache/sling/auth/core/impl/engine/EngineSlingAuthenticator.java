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
package org.apache.sling.auth.core.impl.engine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.engine.auth.Authenticator;
import org.apache.sling.engine.auth.NoAuthenticationHandlerException;
import org.osgi.framework.Constants;

/**
 * The <code>EngineSlingAuthenticator</code> class is a simple proxy service
 * providing the old Sling Engine {@link Authenticator} service calling into the
 * new standalone Apache Sling
 * {@link org.apache.sling.auth.core.AuthenticationSupport} service.
 */
@Component()
@Service(value = Authenticator.class)
@Properties( {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling Request Authenticator (Legacy Bridge)"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
@SuppressWarnings("deprecation")
public class EngineSlingAuthenticator implements Authenticator {

    @Reference
    private org.apache.sling.api.auth.Authenticator slingAuthenticator;

    public void login(HttpServletRequest request, HttpServletResponse response) {
        try {
            slingAuthenticator.login(request, response);
        } catch (org.apache.sling.api.auth.NoAuthenticationHandlerException nahe) {
            final NoAuthenticationHandlerException wrapped = new NoAuthenticationHandlerException();
            wrapped.initCause(nahe);
            throw wrapped;
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        slingAuthenticator.logout(request, response);
    }

}
