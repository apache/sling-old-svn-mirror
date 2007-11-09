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
package org.apache.sling.core.auth;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AuthenticationHandler</code> interface is implemented by handlers
 * configured for the <code>AuthenticationHandlerService</code>. That service
 * uses the handler to offload the implementation of extracting credential data
 * from the HTTP request.
 */
public interface AuthenticationHandler {

    /**
     * Returned by {@link #authenticate} to indicate an ongoing authentication
     * transaction.
     */
    static final Credentials DOING_AUTH = new Credentials() {
    };

    // TODO
    // @return true this handler can authenticate the request
    boolean handles(HttpServletRequest request);

    /**
     * Extracts credential data from the request if at all contained. This check
     * must basically be based on the original request object with the exception
     * of the request URI, which may be gotten from the request object through
     * the {@link DeliveryHttpServletRequest#getRealRequestURI()}, which
     * returns the request URI with the context path removed.
     * <p>
     * The method returns any of the following values : <table>
     * <tr>
     * <th>value
     * <th>description</tr>
     * <tr>
     * <td><code>null</code>
     * <td>no user details were contained in the request </tr>
     * <tr>
     * <td>{@link #DOING_AUTH}
     * <td>the handler is in an ongoing authentication transaction with the
     * client. Further request handling in the DeliveryModule should not take
     * place.
     * <tr>
     * <tr>
     * <td><code>Credentials</code> object
     * <td>The user sent credentials.</tr>
     * </table>
     * <p>
     * The method must not request credential information from the client, if
     * they are not found in the request.
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return A valid <code>Credentials</code> instance identifying the
     *         request user, {@link #DOING_AUTH} if the handler is in an
     *         authentication transaction with the client or null if the request
     *         does not contain authentication information. In case of
     *         {@link #DOING_AUTH}, the method must have sent a response
     *         indicating that fact to the client.
     */
    Credentials authenticate(HttpServletRequest request,
            HttpServletResponse response);

    /**
     * Requests authentication information from the client. Returns
     * <code>true</code> if the information has been requested and request
     * processing can be terminated normally. Otherwise the authorization
     * information could not be requested and the request will be terminated
     * with a 403 (Forbidden) response.
     * <p>
     * Any response sent by the handler though the <code>sendError</code>
     * method is also handled by the error handler infrastructure.
     *
     * @param request The request object.
     * @param response The response object to which to send the request.
     * @param addInfo Additional information string from the configuration. This
     *            may for example be used as the realm name for HTTP header
     *            authentication. TODO: handler configuration
     * @return <code>true</code> if the information could be requested or
     *         <code>false</code>, if the request should fail with the
     *         appropriate error status.
     */
    boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response);
}
