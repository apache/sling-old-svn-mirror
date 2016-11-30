/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.jacksonexporter.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Enumeration;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class HttpServletRequestMixin extends ServletRequestMixin implements HttpServletRequest {

    @JsonGetter
    @Override
    public abstract String getAuthType();

    @JsonGetter
    @Override
    public abstract Cookie[] getCookies();

    @JsonGetter
    @Override
    public abstract String getMethod();

    @JsonGetter
    @Override
    public abstract String getPathInfo();

    @JsonGetter
    @Override
    public abstract String getPathTranslated();

    @JsonGetter
    @Override
    public abstract String getContextPath();

    @JsonGetter
    @Override
    public abstract String getQueryString();

    @JsonGetter
    @Override
    public abstract  String getRemoteUser();

    @JsonGetter
    @Override
    public abstract String getRemoteHost();

    @JsonGetter
    @Override
    public abstract Principal getUserPrincipal();

    @JsonGetter
    @Override
    public abstract String getRequestedSessionId();

    @JsonGetter
    @Override
    public abstract String getRequestURI();

    @JsonGetter
    @Override
    public abstract boolean isRequestedSessionIdFromCookie();

    @JsonGetter
    @Override
    public abstract boolean isRequestedSessionIdFromURL();

    @JsonGetter
    @Override
    public abstract boolean isRequestedSessionIdValid();

    @JsonGetter
    @Override
    public abstract boolean isRequestedSessionIdFromUrl();

    @JsonGetter
    @Override
    public abstract Enumeration getHeaderNames();

    @JsonGetter
    @Override
    public abstract String getServletPath();
}
