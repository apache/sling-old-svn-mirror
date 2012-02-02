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
package org.apache.sling.engine.impl.log;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.service.http.HttpContext;

class RequestLoggerRequest extends HttpServletRequestWrapper {

    RequestLoggerRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getRemoteUser() {
        final Object user = getAttribute(HttpContext.REMOTE_USER);
        if (user instanceof String) {
            return (String) user;
        }

        return super.getRemoteUser();
    }

    public String getAuthType() {
        final Object authType = getAttribute(HttpContext.AUTHENTICATION_TYPE);
        if (authType instanceof String) {
            return (String) authType;
        }

        return super.getAuthType();
    }

    Cookie getCookie(final String name) {
        final Cookie[] cookies = getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    return cookies[i];
                }
            }
        }

        return null;
    }
}
