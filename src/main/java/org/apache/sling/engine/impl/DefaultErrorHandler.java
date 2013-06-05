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
package org.apache.sling.engine.impl;

import static org.apache.sling.api.SlingConstants.ERROR_REQUEST_URI;
import static org.apache.sling.api.SlingConstants.ERROR_SERVLET_NAME;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>DefaultErrorHandler</code> is used by the
 * {@link SlingRequestProcessorImpl} as long as no {@link ErrorHandler} service
 * is registered.
 */
public class DefaultErrorHandler implements ErrorHandler {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String serverInfo = SlingMainServlet.PRODUCT_NAME;

    void setServerInfo(final String serverInfo) {
        this.serverInfo = (serverInfo != null)
                ? serverInfo
                : SlingMainServlet.PRODUCT_NAME;
    }

    // ---------- ErrorHandler interface (default implementation) --------------

    /**
     * Backend implementation of the HttpServletResponse.sendError methods.
     * <p>
     * This implementation resets the response before sending back a
     * standardized response which just conveys the status, the message (either
     * provided or a message derived from the status code), and server
     * information.
     * <p>
     * This method logs error and does not write back and response data if the
     * response has already been committed.
     */
    public void handleError(final int status,
            String message,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
    throws IOException {

        if (message == null) {
            message = "HTTP ERROR:" + String.valueOf(status);
        } else {
            message = "HTTP ERROR:" + status + " - " + message;
        }

        sendError(status, message, null, request, response);
    }

    /**
     * Backend implementation of handling uncaught throwables.
     * <p>
     * This implementation resets the response before sending back a
     * standardized response which just conveys the status as 500/INTERNAL
     * SERVER ERROR, the message from the throwable, the stacktrace, and server
     * information.
     * <p>
     * This method logs error and does not write back and response data if the
     * response has already been committed.
     */
    public void handleError(final Throwable throwable,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
    throws IOException {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            throwable.getMessage(), throwable, request, response);
    }

    private void sendError(final int status,
            final String message,
            final Throwable throwable,
            final HttpServletRequest request,
            final HttpServletResponse response)
    throws IOException {

        if (response.isCommitted()) {
            log.error(
                "handleError: Response already committed; cannot send error "
                    + status + message, throwable);
        } else {

            // error situation
            final String servletName = (String) request.getAttribute(ERROR_SERVLET_NAME);
            String requestURI = (String) request.getAttribute(ERROR_REQUEST_URI);
            if (requestURI == null) {
                requestURI = request.getRequestURI();
            }

            // reset anything in the response first
            response.reset();

            // set the status, content type and encoding
            response.setStatus(status);
            response.setContentType("text/html; charset=UTF-8");

            final PrintWriter pw = response.getWriter();
            pw.println("<html><head><title>");
            pw.println(ResponseUtil.escapeXml(message));
            pw.println("</title></head><body><h1>");
            if (throwable != null) {
                pw.println(ResponseUtil.escapeXml(throwable.toString()));
            } else if (message != null) {
                pw.println(ResponseUtil.escapeXml(message));
            } else {
                pw.println("Internal error (no Exception to report)");
            }
            pw.println("</h1><p>");
            pw.print("RequestURI=");
            pw.println(ResponseUtil.escapeXml(request.getRequestURI()));
            if (servletName != null) {
                pw.println("</p><p>Servlet=");
                pw.println(ResponseUtil.escapeXml(servletName));
            }
            pw.println("</p>");

            if (throwable != null) {
                final PrintWriter escapingWriter = new PrintWriter(
                        ResponseUtil.getXmlEscapingWriter(pw));
                pw.println("<h3>Exception stacktrace:</h3>");
                pw.println("<pre>");
                pw.flush();
                throwable.printStackTrace(escapingWriter);
                escapingWriter.flush();
                pw.println("</pre>");

                final RequestProgressTracker tracker = ((SlingHttpServletRequest) request).getRequestProgressTracker();
                pw.println("<h3>Request Progress:</h3>");
                pw.println("<pre>");
                pw.flush();
                tracker.dump(new PrintWriter(escapingWriter));
                escapingWriter.flush();
                pw.println("</pre>");
            }

            pw.println("<hr /><address>");
            pw.println(ResponseUtil.escapeXml(serverInfo));
            pw.println("</address></body></html>");

            // commit the response
            response.flushBuffer();
            // close the response (SLING-2724)
            pw.close();
        }
    }
}
