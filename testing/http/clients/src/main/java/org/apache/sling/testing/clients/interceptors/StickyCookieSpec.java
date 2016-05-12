/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.interceptors;

import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookiePathComparator;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.DefaultCookieSpec;

import java.util.List;

public class StickyCookieSpec extends DefaultCookieSpec {
    private final static CookiePathComparator PATH_COMPARATOR = new CookiePathComparator();

    @Override
    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        List<Cookie> cookies = super.parse(header, origin);
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(StickyCookieHolder.COOKIE_NAME)) {
                // store it in the TestStickySessionRule threadlocal var
                StickyCookieHolder.setTestStickySessionCookie(cookie);
            }
        }
        return cookies;
    }

    @Override
    public List<Header> formatCookies(List<Cookie> cookies) {
        return super.formatCookies(cookies);
    }
}