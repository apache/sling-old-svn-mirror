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
package org.apache.sling.auth.core.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AbstractAuthenticationFormServlet</code> provides a basic
 * implementation of a simple servlet to render a login form for authentication
 * purposes.
 */
@SuppressWarnings("serial")
public abstract class AbstractAuthenticationFormServlet extends HttpServlet {

    /**
     * The path to the default login form.
     *
     * @see #getDefaultFormPath()
     */
    public static final String DEFAULT_FORM_PATH = "login.html";

    /**
     * The path to the custom login form.
     *
     * @see #getCustomFormPath()
     */
    public static final String CUSTOM_FORM_PATH = "custom_login.html";

    /**
     * The raw form used by the {@link #getForm(HttpServletRequest)} method to
     * fill in with per-request data. This field is set by the
     * {@link #getRawForm()} method when first loading the form.
     */
    private volatile String rawForm;

    /**
     * Prepares and returns the login form. The response is sent as an UTF-8
     * encoded <code>text/html</code> page with all known cache control headers
     * set to prevent all caching.
     * <p>
     * This servlet is to be called to handle the request directly, that is it
     * expected to not be included and for the response to not be committed yet
     * because it first resets the response.
     *
     * @throws IOException if an error occurrs preparing or sending back the
     *             login form
     * @throws IllegalStateException if the response has already been committed
     *             and thus response reset is not possible.
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    /**
     * Prepares and returns the login form. The response is sent as an UTF-8
     * encoded <code>text/html</code> page with all known cache control headers
     * set to prevent all caching.
     * <p>
     * This servlet is to be called to handle the request directly, that is it
     * expected to not be included and for the response to not be committed yet
     * because it first resets the response.
     *
     * @throws IOException if an error occurrs preparing or sending back the
     *             login form
     * @throws IllegalStateException if the response has already been committed
     *             and thus response reset is not possible.
     */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // reset the response first
        response.reset();

        // setup the response for HTML and cache prevention
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // send the form and flush
        response.getWriter().print(getForm(request));
        response.flushBuffer();
    }

    /**
     * Returns the form to be sent back to the client for login providing an
     * optional informational message and the optional target to redirect to
     * after successfully logging in.
     *
     * @param request The request providing parameters indicating the
     *            informational message and redirection target.
     * @return The login form to be returned to the client
     * @throws IOException If the login form cannot be loaded
     */
    protected String getForm(final HttpServletRequest request)
            throws IOException {
        String form = getRawForm();

        form = form.replace("${resource}", getResource(request));
        form = form.replace("${j_reason}", getReason(request));
        form = form.replace("${requestContextPath}", request.getContextPath());

        return form;
    }

    /**
     * Returns the path to the resource to which the request should be
     * redirected after successfully completing the form or an empty string if
     * there is no <code>resource</code> request parameter.
     *
     * @param request The request providing the <code>resource</code> parameter.
     * @return The target to redirect after sucessfully login or an empty string
     *         if no specific target has been requested.
     */
    protected String getResource(final HttpServletRequest request) {
        return AbstractAuthenticationHandler.getLoginResource(request, "");
    }

    /**
     * Returns an informational message according to the value provided in the
     * <code>j_reason</code> request parameter. Supported reasons are invalid
     * credentials and session timeout.
     *
     * @param request The request providing the parameter
     * @return The "translated" reason to render the login form or an empty
     *         string if there is no specific reason
     */
    protected abstract String getReason(final HttpServletRequest request);

    /**
     * Load the raw unmodified form from the bundle (through the class loader).
     *
     * @return The raw form as a string
     * @throws IOException If an error occurrs reading the "file" or if the
     *             class loader cannot provide the form data.
     */
    private String getRawForm() throws IOException {
        if (rawForm == null) {
            InputStream ins = null;
            try {
                // try a custom login page first.
                ins = getClass().getResourceAsStream(getCustomFormPath());
                if (ins == null) {
                    // try the standard login page
                    ins = getClass().getResourceAsStream(getDefaultFormPath());
                }

                if (ins != null) {
                    StringBuilder builder = new StringBuilder();
                    Reader r = new InputStreamReader(ins, "UTF-8");
                    char[] cbuf = new char[1024];
                    int rd = 0;
                    while ((rd = r.read(cbuf)) >= 0) {
                        builder.append(cbuf, 0, rd);
                    }

                    rawForm = builder.toString();
                }
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            if (rawForm == null) {
                throw new IOException("Failed reading form template");
            }
        }

        return rawForm;
    }

    /**
     * Returns the path to the default login form to load through the class
     * loader of this instance using <code>Class.getResourceAsStream</code>.
     * <p>
     * The default form is used intended to be included with the bundle
     * implementing this abstract class.
     * <p>
     * This method returns {@link #DEFAULT_FORM_PATH} and may be overwritten by
     * implementations.
     */
    protected String getDefaultFormPath() {
        return DEFAULT_FORM_PATH;
    }

    /**
     * Returns the path to the custom login form to load through the class
     * loader of this instance using <code>Class.getResourceAsStream</code>.
     * <p>
     * The custom form can be supplied by a fragment attaching to the bundle
     * implementing this abstract class.
     * <p>
     * This method returns {@link #CUSTOM_FORM_PATH} and may be overwritten by
     * implementations.
     */
    protected String getCustomFormPath() {
        return CUSTOM_FORM_PATH;
    }
}
