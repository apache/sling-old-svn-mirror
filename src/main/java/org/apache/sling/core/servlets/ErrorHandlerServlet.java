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
package org.apache.sling.core.servlets;

import javax.servlet.Servlet;

/**
 * The <code>ErrorHandlerServlet</code> interface extends the
 * <code>Component</code> providing the required API for components used for
 * error handling.
 * <p>
 * Error handling components are registered just like regular components but are
 * recognized by the Component Framework and used for error handling only.
 */
public interface ErrorHandlerServlet extends Servlet {

    /**
     * Returns <code>true</code> if this error handler can handle the given
     * <code>throwable</code>. This method does not compare the class name in
     * the class hierarchy. For example, if the handler can handle the
     * <code>java.io.IOException</code> it must return <code>false</code> for
     * the <code>java.io.MalformedURLException</code> class.
     * <p>
     * This method is called by the Component Framework with the fully qualified
     * name of the Throwable class to be handled. If no registered handler
     * component is registered for the exact class, the Component Framework
     * will follow the super class hierarchy of the throwable to ultimately find
     * a handler.
     *
     * @param throwable The fully qualified name of the class to check.
     *
     * @return <code>true</code> if this handler can handle excactly this class.
     */
    boolean canHandle(String throwable);

    /**
     * Returns <code>true</code> if this error handler can handle the given
     * HTTP <code>status</code> code.
     * <p>
     * This method is called by the Component Framework with the HTTP status
     * code. This method must evaluate the code with an exact match. The
     * Component Framework will search for a registered handler taking care of
     * a status in this order:
     * <ol>
     * <li>Exact status code, e.g. 423 (Locked)
     * <li>Status with zero on last place, e.g. 420. Handlers for the status
     *      range 420..429 are expected to respond <code>true</code> here.
     * <li>Status with zero on last two places, e.g. 400. Handlers for the
     *      status code range 400..499 are expected to respond <code>true</code>
     *      here.
     * <li>Status zero. This is a last resort and should be responded to with
     *      <code>true</code> only by handlers which take any status code.
     * </ol>
     *
     * @param status The HTTP status code to check.
     */
    boolean canHandle(int status);
}
