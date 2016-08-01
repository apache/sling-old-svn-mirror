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
package org.apache.sling.engine.servlets;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>ErrorHandler</code> defines the interface of the service used by
 * the Sling to handle calls to <code>HttpServletResponse.sendError</code> and
 * to handle uncaught <code>Throwable</code>s.
 */
@ConsumerType
public interface ErrorHandler {

    /**
     * Called to render a response for a HTTP status code. This method should
     * set the response status and print the status code and optional message.
     * <p>
     * If the response has already been committed, an error message should be
     * logged but no further processing should take place.
     *
     * @param status The HTTP status code to set
     * @param message An optional message to write to the response. This message
     *            may be <code>null</code>.
     * @param request The request object providing more information on the
     *            request.
     * @param response The response object used to send the status and message.
     * @throws IOException May be thrown if an error occurrs sending the
     *             response.
     */
    void handleError(int status, String message,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException;

    /**
     * Called to render a response for an uncaught <code>Throwable</code>.
     * <p>
     * If the response has already been committed, an error message should be
     * logged but no further processing should take place.
     *
     * @param throwable The <code>Throwable</code> causing this method to be
     *            called.
     * @param request The request object providing more information on the
     *            request.
     * @param response The response object used to send the status and message.
     * @throws IOException May be thrown if an error occurrs sending the
     *             response.
     */
    void handleError(Throwable throwable, SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException;

}