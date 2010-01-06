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
package org.apache.sling.commons.auth.spi;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AuthenticationHandler</code> interface defines the service API used
 * by the authentication implementation to support plugin various ways of
 * extracting credentials from the request.
 */
public interface AuthenticationHandler {

    /**
     * The name under which an implementation of this interface must be
     * registered to be used as an authentication handler.
     */
    static final String SERVICE_NAME = "org.apache.sling.commons.auth.AuthenticationHandler";

    /**
     * The name of the service registration property listing one or more URL
     * paths for which the authentication handler is to be used. The property
     * may be a single string value or an array of strings or a Collection of
     * strings.
     * <p>
     * Each string value may either be an absolute path (e.g. /content) or an
     * absolute URL (e.g. http://thehost/content) to assign the authentication
     * handler to authenticate request for a select virtual host.
     * <p>
     * Authentication handlers without a <code>path</code> service registration
     * property are ignored.
     */
    static final String PATH_PROPERTY = "path";

    /**
     * Extracts credential data from the request if at all contained.
     * <p>
     * The method returns any of the following values :
     * <table>
     * <tr>
     * <th>value
     * <th>description
     * </tr>
     * <tr>
     * <td><code>null</code>
     * <td>no user details were contained in the request or the handler is not
     * capable or willing to extract credentials from the request
     * </tr>
     * <tr>
     * <td>{@link AuthenticationInfo#DOING_AUTH}
     * <td>the handler is in an ongoing authentication transaction with the
     * client. Request processing should be aborted at this stage.
     * <tr>
     * <tr>
     * <td><code>AuthenticationInfo</code> object
     * <td>The user sent credentials. The returned object contains the
     * credentials as well as the type of authentication transmission employed.
     * </tr>
     * </table>
     * <p>
     * The method must not request credential information from the client, if
     * they are not found in the request.
     * <p>
     * The value of {@link #PATH_PROPERTY} service registration property value
     * triggering this call is available as the <code>path</code> request
     * attribute. If the service is registered with multiple path values, the
     * value of the <code>path</code> request attribute may be used to implement
     * specific handling.
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return A valid <code>AuthenticationInfo</code> instance identifying the
     *         request user, {@link AuthenticationInfo#DOING_AUTH} if the
     *         handler is in an authentication transaction with the client or
     *         null if the request does not contain authentication information.
     *         In case of {@link AuthenticationInfo#DOING_AUTH}, the method must
     *         have sent a response indicating that fact to the client.
     */
    AuthenticationInfo authenticate(HttpServletRequest request,
            HttpServletResponse response);

    /**
     * Requests authentication information from the client. Returns
     * <code>true</code> if the information has been requested and request
     * processing can be terminated normally. Otherwise the authorization
     * information could not be requested.
     * <p>
     * The <code>HttpServletResponse.sendError</code> methods should not be used
     * by the implementation because these responses might be post-processed by
     * the servlet container's error handling infrastructure thus preventing the
     * correct operation of the authentication handler. To convey a HTTP
     * response status the <code>HttpServletResponse.setStatus</code> method
     * should be used.
     * <p>
     * The value of {@link #PATH_PROPERTY} service registration property value
     * triggering this call is available as the <code>path</code> request
     * attribute. If the service is registered with multiple path values, the
     * value of the <code>path</code> request attribute may be used to implement
     * specific handling.
     *
     * @param request The request object.
     * @param response The response object to which to send the request.
     * @return <code>true</code> if the handler is able to end an authentication
     *         inquiry for the given request. <code>false</code> otherwise.
     * @throws IOException If an error occurrs sending the authentication
     *             inquiry to the client.
     */
    boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    /**
     * Drops any credential and authentication details from the request and asks
     * the client to do the same.
     *
     * @param request The request object.
     * @param response The response object to which to send the request.
     * @throws IOException If an error occurrs asking the client to drop any
     *             authentication traces.
     */
    void dropAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}
