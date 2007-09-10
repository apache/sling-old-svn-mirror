/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.component;

import java.io.IOException;

import javax.servlet.RequestDispatcher;

/**
 * The <code>ComponentRequestDispatcher</code> interface defines an object
 * that receives requests from the client and sends them to the specified
 * resources (such as a servlet, HTML file, or JSP file) on the server. The
 * component framework creates the <code>ComponentRequestDispatcher</code>
 * object, which is used as a wrapper around a server resource located at a
 * particular path.
 */
public interface ComponentRequestDispatcher extends RequestDispatcher {

    /**
     * Includes the content of a resource (servlet, JSP page, HTML file) in the
     * response. In essence, this method enables programmatic server-side
     * includes.
     * <p>
     * The included component cannot set or change the response status code or
     * set headers; any attempt to make a change is ignored.
     * 
     * @param request a {@link ComponentRequest} object that contains the client
     *            request. This must be the original request object of the
     *            {@link Component#render(ComponentRequest, ComponentResponse)} method
     *            or a {@link ComponentRequestWrapper} around the original request
     *            object.
     * @param response a {@link ComponentResponse} object that contains the render
     *            response. This must be the original response object of the
     *            {@link Component#render(ComponentRequest, ComponentResponse)} method
     *            or a {@link ComponentResponseWrapper} around the original
     *            response object.
     * @throws ComponentException if the included resource throws a
     *             ServletException, or other exceptions that are not Runtime-
     *             or IOExceptions.
     * @throws IOException if the included resource throws this exception
     */
    void include(ComponentRequest request, ComponentResponse response)
        throws ComponentException, IOException;
}
