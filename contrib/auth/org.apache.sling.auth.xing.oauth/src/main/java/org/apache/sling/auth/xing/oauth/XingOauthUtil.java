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
package org.apache.sling.auth.xing.oauth;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.apache.sling.auth.xing.api.XingUser;
import org.scribe.model.Token;

public class XingOauthUtil {

    public static Token getAccessToken(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final Object attribute = simpleCredentials.getAttribute(XingOauth.AUTHENTICATION_CREDENTIALS_ACCESS_TOKEN_KEY);
            if (attribute instanceof Token) {
                return (Token) attribute;
            }
        }
        return null;
    }

    public static XingUser getXingUser(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final Object attribute = simpleCredentials.getAttribute(XingOauth.AUTHENTICATION_CREDENTIALS_USER_KEY);
            if (attribute instanceof XingUser) {
                return (XingUser) attribute;
            }
        }
        return null;
    }

}
