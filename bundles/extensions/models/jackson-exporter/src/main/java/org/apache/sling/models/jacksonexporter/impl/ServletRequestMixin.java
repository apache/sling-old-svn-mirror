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

import javax.servlet.ServletRequest;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class ServletRequestMixin implements ServletRequest {

    @JsonGetter("parameters")
    @Override
    public abstract Map getParameterMap();

    @JsonGetter
    @Override
    public abstract Locale getLocale();

    @JsonGetter
    @Override
    public abstract String getContentType();

    @JsonGetter
    @Override
    public abstract int getContentLength();

    @JsonGetter
    @Override
    public abstract int getRemotePort();

    @JsonGetter
    @Override
    public abstract String getRemoteAddr();

    @JsonGetter
    @Override
    public abstract int getServerPort();

    @JsonGetter
    @Override
    public abstract String getServerName();

    @JsonGetter
    @Override
    public abstract boolean isSecure();

    @Override
    public abstract Enumeration getLocales();

    @Override
    public abstract String getCharacterEncoding();

    @Override
    public abstract int getLocalPort();

    @Override
    public abstract String getLocalAddr();

    @Override
    public abstract String getLocalName();

    @Override
    public abstract String getProtocol();

    @Override
    public abstract String getScheme();
}
