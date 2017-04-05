/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.clients.interceptors;

import org.apache.http.cookie.Cookie;
import org.apache.sling.testing.clients.Constants;

public class StickyCookieHolder {

    private static final ThreadLocal<Cookie> testStickySessionCookie = new ThreadLocal<Cookie>();
    public static final String COOKIE_NAME = System.getProperty(Constants.CONFIG_PROP_PREFIX + "session.cookie.name", "test_session_id");

    public static Cookie getTestStickySessionCookie() {
        return testStickySessionCookie.get();
    }

    public static void setTestStickySessionCookie(Cookie stickySessionCookie) {
        testStickySessionCookie.set(stickySessionCookie);
    }

    public static void remove() {
        testStickySessionCookie.remove();
    }
}
