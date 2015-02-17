/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.api.servlets;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Helper base class for data modifying Servlets used in Sling. This class
 * extends the {@link SlingSafeMethodsServlet} by support for the <em>POST</em>,
 * <em>PUT</em> and <em>DELETE</em> methods.
 * <p>
 * Implementors note: The methods in this class are all declared to throw the
 * exceptions according to the intentions of the Servlet API rather than
 * throwing their Sling RuntimeException counter parts. This is done to easy the
 * integration with traditional servlets.
 *
 * @see SlingSafeMethodsServlet for more information on supporting more HTTP
 *      methods
 */
public class SlingAllMethodsServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -7960975481323952419L;

    /**
     * Called by the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method to
     * handle an HTTP <em>POST</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>POST</em> method support.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException If the error status cannot be reported back to the
     *             client.
     */
    @SuppressWarnings("unused")
    protected void doPost(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Called by the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method to
     * handle an HTTP <em>PUT</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PUT</em> method support.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException If the error status cannot be reported back to the
     *             client.
     */
    @SuppressWarnings("unused")
    protected void doPut(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Called by the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method to
     * handle an HTTP <em>DELETE</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>DELETE</em> method support.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException If the error status cannot be reported back to the
     *             client.
     */
    @SuppressWarnings("unused")
    protected void doDelete(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Tries to handle the request by calling a Java method implemented for the
     * respective HTTP request method.
     * <p>
     * This implementation first calls the base class implementation and only if
     * the base class cannot dispatch will try to dispatch the supported methods
     * <em>POST</em>, <em>PUT</em> and <em>DELETE</em> and returns
     * <code>true</code> if any of these methods is requested. Otherwise
     * <code>false</code> is just returned.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return <code>true</code> if the requested method (<code>request.getMethod()</code>)
     *         is known. Otherwise <code>false</code> is returned.
     * @throws ServletException Forwarded from any of the dispatched methods
     * @throws IOException Forwarded from any of the dispatched methods
     */
    protected boolean mayService(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {

        // assume the method is known for now
        if (super.mayService(request, response)) {
            return true;
        }

        // assume the method is known for now
        boolean methodKnown = true;

        String method = request.getMethod();
        if (HttpConstants.METHOD_POST.equals(method)) {
            doPost(request, response);
        } else if (HttpConstants.METHOD_PUT.equals(method)) {
            doPut(request, response);
        } else if (HttpConstants.METHOD_DELETE.equals(method)) {
            doDelete(request, response);
        } else {
            // actually we do not know the method
            methodKnown = false;
        }

        // return whether we actually knew the request method or not
        return methodKnown;
    }

    /**
     * Helper method called by
     * {@link #doOptions(SlingHttpServletRequest, SlingHttpServletResponse)} to calculate
     * the value of the <em>Allow</em> header sent as the response to the HTTP
     * <em>OPTIONS</em> request.
     * <p>
     * This implementation overwrites the base class implementation adding
     * support for the <em>POST</em>, <em>PUT</em> and <em>DELETE</em>
     * methods in addition to the methods returned by the base class
     * implementation.
     *
     * @param declaredMethods The public and protected methods declared in the
     *            extension of this class.
     * @return A <code>StringBuffer</code> containing the list of HTTP methods
     *         supported.
     */
    protected @Nonnull StringBuffer getAllowedRequestMethods(
            @Nonnull Map<String, Method> declaredMethods) {
        StringBuffer allowBuf = super.getAllowedRequestMethods(declaredMethods);

        // add more method names depending on the methods found
        String className = SlingAllMethodsServlet.class.getName();
        if (isMethodValid(declaredMethods.get("doPost"), className)) {
            allowBuf.append(", ").append(HttpConstants.METHOD_POST);

        } else if (isMethodValid(declaredMethods.get("doPut"), className)) {
            allowBuf.append(", ").append(HttpConstants.METHOD_PUT);

        } else if (isMethodValid(declaredMethods.get("doDelete"), className)) {
            allowBuf.append(", ").append(HttpConstants.METHOD_DELETE);
        }

        return allowBuf;
    }

    /**
     * Returns <code>true</code> if <code>method</code> is not
     * <code>null</code> and the method is not defined in the class named by
     * <code>className</code>.
     * <p>
     * This method may be used to make sure a method is actually overwritten and
     * not just the default implementation.
     *
     * @param method The Method to check
     * @param className The name of class assumed to contained the initial
     *            declaration of the method.
     * @return <code>true</code> if <code>method</code> is not
     *         <code>null</code> and the methods declaring class is not the
     *         given class.
     */
    protected boolean isMethodValid(Method method, String className) {
        return method != null && !method.getClass().getName().equals(className);
    }
}
