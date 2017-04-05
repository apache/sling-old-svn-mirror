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


import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

public class StickyCookieInterceptor implements HttpRequestInterceptor {

    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        final HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
        List<Cookie> cookies = clientContext.getCookieStore().getCookies();
        boolean set = (null != StickyCookieHolder.getTestStickySessionCookie());
        boolean found = false;
        ListIterator<Cookie> it = cookies.listIterator();
        while (it.hasNext()) {
            Cookie cookie = it.next();
            if (cookie.getName().equals(StickyCookieHolder.COOKIE_NAME)) {
                found = true;
                if (set) {
                    // set the cookie with the value saved for each thread using the rule
                    it.set(StickyCookieHolder.getTestStickySessionCookie());
                } else {
                    // if the cookie is not set in TestStickySessionRule, remove it from here
                    it.remove();
                }
            }
        }
        // if the cookie needs to be set from TestStickySessionRule but did not exist in the client cookie list, add it here.
        if (!found && set) {
            cookies.add(StickyCookieHolder.getTestStickySessionCookie());
        }
        BasicCookieStore cs = new BasicCookieStore();
        cs.addCookies(cookies.toArray(new Cookie[cookies.size()]));
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cs);
    }
}