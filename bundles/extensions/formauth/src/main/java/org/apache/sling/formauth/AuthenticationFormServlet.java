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
package org.apache.sling.formauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.auth.Authenticator;

/**
 * The <code>AuthenticationFormServlet</code> provides the default login form
 * used for Form Based Authentication.
 *
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *               value="Default Login Form for Form Based Authentication"
 */
@SuppressWarnings("serial")
public class AuthenticationFormServlet extends HttpServlet {

    /**
     * @scr.property name="sling.servlet.paths"
     */
    static final String SERVLET_PATH = "/system/sling/form/login";

    /**
     * @scr.property name="sling.auth.requirements"
     */
    @SuppressWarnings("unused")
    private static final String AUTH_REQUIREMENT = "-" + SERVLET_PATH;

    private volatile String rawForm;

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // setup the response for HTML and cache prevention
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-control", "no-cache");
        response.addHeader("Cache-control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // send the form and flush
        response.getWriter().print(getForm(request));
        response.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);
    }

    private String getForm(final HttpServletRequest request) throws IOException {

        String resource = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
        if (resource == null) {
            resource = request.getParameter(Authenticator.LOGIN_RESOURCE);
            if (resource == null) {
                resource = "/";
            }
        }

        return getRawForm().replace("${resource}", resource);
    }

    private String getRawForm() throws IOException {
        if (rawForm == null) {
            InputStream ins = null;
            try {
                ins = getClass().getResourceAsStream("/login.html");
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
}
