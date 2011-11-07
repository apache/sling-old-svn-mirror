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
package org.apache.sling.auth.openid;

import org.apache.sling.auth.core.spi.AuthenticationHandler;

/**
 * The <code>OpenIDConstants</code> class defines useful constants for
 * implementors of login forms for OpenID authentication.
 */
public final class OpenIDConstants {

    /**
     * Identification of this authentication handler. This value is set by the
     * handler as the authentication type of the <code>AuthenticationInfo</code>
     * object returned from the <code>extractCredentials</code> method.
     * <p>
     * To explicitly request OpenID authentication handling, this should be used
     * as the value of the <code>sling:authRequestLogin</code> request
     * parameter.
     */
    public static final String OPENID_AUTH = "OpenID";

    /**
     * The name of the request parameter set by the
     * <code>requestCredentials</code> method when redirecting to the login
     * request form. The value of the parameter is the name of one of the
     * {@link OpenIDFailure} constants.
     * <p>
     * This parameter is intended to be used by the login form to provide
     * information to the client as to why OpenID authentication has failed. For
     * example a login form implemented as a JSP may use the parameter to write
     * the message like this:
     *
     * <pre>
     * &lt;%
     *     String reason = request.getParameter(OPENID_FAILURE_REASON_ATTRIBUTE);
     *     OpenIDFailure fReason = OpenIDFailure.valueOf(reason);
     * %>
     * &lt;div id="err">
     *   &lt;p>&lt;%= fReason %>&lt;/p>
     * &lt;/div>
     * </pre>
     */
    public static final String OPENID_FAILURE_REASON = AuthenticationHandler.FAILURE_REASON;

    /**
     * The name of the request parameter set by the
     * <code>requestCredentials</code> method providing to authenticated OpenID
     * identity. This parameter is only set if the
     * {@link #OPENID_FAILURE_REASON} is {@link OpenIDFailure#REPOSITORY} and
     * can be used to offer the user assistance with associating an existing JCR
     * user with the OpenID identity.
     */
    public static final String OPENID_IDENTITY = "j_openid_identity";

}
