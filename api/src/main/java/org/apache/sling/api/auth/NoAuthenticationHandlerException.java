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
package org.apache.sling.api.auth;

import org.apache.sling.api.SlingException;

/**
 * The <code>NoAuthenticationHandlerException</code> is thrown to indicate that
 * the
 * {@link Authenticator#login(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method could not find a way to authenticate the request user.
 * <p>
 * This exception is thrown without a message. The caller of the
 * {@link Authenticator#login(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method called is expected to immediately handle this exception and not to
 * forward it up the call chain.
 * <p>
 * This exception is not intended to be thrown by client code but is used by the
 * {@link Authenticator} implementation.
 *
 * @since 1.0 (Sling API Bundle 2.1.0)
 */
@SuppressWarnings("serial")
public class NoAuthenticationHandlerException extends SlingException {

    public NoAuthenticationHandlerException() {
        super();
    }

}
