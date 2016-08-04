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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

/**
 * Helper base class for read-only Servlets used in Sling. This base class is
 * actually just a better implementation of the Servlet API <em>HttpServlet</em>
 * class which accounts for extensibility. So extensions of this class have
 * great control over what methods to overwrite.
 * <p>
 * If any of the default HTTP methods is to be implemented just overwrite the
 * respective doXXX method. If additional methods should be supported implement
 * appropriate doXXX methods and overwrite the
 * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method
 * to dispatch to the doXXX methods as appropriate and overwrite the
 * {@link #getAllowedRequestMethods(Map)} to add the new method names.
 * <p>
 * Please note, that this base class is intended for applications where data is
 * only read. As such, this servlet by itself does not support the <em>POST</em>,
 * <em>PUT</em> and <em>DELETE</em> methods. Extensions of this class should
 * either overwrite any of the doXXX methods of this class or add support for
 * other read-only methods only. Applications wishing to support data
 * modification should rather use or extend the {@link SlingAllMethodsServlet}
 * which also contains support for the <em>POST</em>, <em>PUT</em> and
 * <em>DELETE</em> methods. This latter class should also be overwritten to
 * add support for HTTP methods modifying data.
 * <p>
 * Implementors note: The methods in this class are all declared to throw the
 * exceptions according to the intentions of the Servlet API rather than
 * throwing their Sling RuntimeException counter parts. This is done to ease the
 * integration with traditional servlets.
 *
 * @see SlingAllMethodsServlet
 */
public class SlingSafeMethodsServlet extends GenericServlet {

    private static final long serialVersionUID = 3620512288346703072L;

    /**
     * Handles the <em>HEAD</em> method.
     * <p>
     * This base implementation just calls the
     * {@link #doGet(SlingHttpServletRequest, SlingHttpServletResponse)} method dropping
     * the output. Implementations of this class may overwrite this method if
     * they have a more performing implementation. Otherwise, they may just keep
     * this base implementation.
     *
     * @param request The HTTP request
     * @param response The HTTP response which only gets the headers set
     * @throws ServletException Forwarded from the
     *             {@link #doGet(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             method called by this implementation.
     * @throws IOException Forwarded from the
     *             {@link #doGet(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             method called by this implementation.
     */
    protected void doHead(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {

        // the null-output wrapper
        NoBodyResponse wrappedResponse = new NoBodyResponse(response);

        // do a normal get request, dropping the output
        doGet(request, wrappedResponse);

        // ensure the content length is set as gathered by the null-output
        wrappedResponse.setContentLength();
    }

    /**
     * Called by the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method to
     * handle an HTTP <em>GET</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>GET</em> method support.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException If the error status cannot be reported back to the
     *             client.
     */
    @SuppressWarnings("unused")
    protected void doGet(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Handles the <em>OPTIONS</em> method by setting the HTTP
     * <code>Allow</code> header on the response depending on the methods
     * declared in this class.
     * <p>
     * Extensions of this class should generally not overwrite this method but
     * rather the {@link #getAllowedRequestMethods(Map)} method. This method
     * gathers all declared public and protected methods for the concrete class
     * (upto but not including this class) and calls the
     * {@link #getAllowedRequestMethods(Map)} method with the methods gathered.
     * The returned value is then used as the value of the <code>Allow</code>
     * header set.
     *
     * @param request The HTTP request object. Not used.
     * @param response The HTTP response object on which the header is set.
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException Not thrown by this implementation.
     */
    @SuppressWarnings("unused")
    protected void doOptions(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        Map<String, Method> methods = getAllDeclaredMethods(getClass());
        StringBuffer allowBuf = getAllowedRequestMethods(methods);
        response.setHeader("Allow", allowBuf.toString());
    }

    /**
     * Handles the <em>TRACE</em> method by just returning the list of all
     * header values in the response body.
     * <p>
     * Extensions of this class do not generally need to overwrite this method
     * as it contains all there is to be done to the <em>TRACE</em> method.
     *
     * @param request The HTTP request whose headers are returned.
     * @param response The HTTP response into which the request headers are
     *            written.
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException May be thrown if there is an problem sending back the
     *             request headers in the response stream.
     */
    @SuppressWarnings("unused")
    protected void doTrace(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {

        String CRLF = "\r\n";

        StringBuffer responseString = new StringBuffer();
        responseString.append("TRACE ").append(request.getRequestURI());
        responseString.append(' ').append(request.getProtocol());

        Enumeration<?> reqHeaderEnum = request.getHeaderNames();
        while (reqHeaderEnum.hasMoreElements()) {
            String headerName = (String) reqHeaderEnum.nextElement();

            Enumeration<?> reqHeaderValEnum = request.getHeaders(headerName);
            while (reqHeaderValEnum.hasMoreElements()) {
                responseString.append(CRLF);
                responseString.append(headerName).append(": ");
                responseString.append(reqHeaderValEnum.nextElement());
            }
        }

        responseString.append(CRLF);

        String charset = "UTF-8";
        byte[] rawResponse = responseString.toString().getBytes(charset);
        int responseLength = rawResponse.length;

        response.setContentType("message/http");
        response.setCharacterEncoding(charset);
        response.setContentLength(responseLength);

        ServletOutputStream out = response.getOutputStream();
        out.write(rawResponse);
    }

    /**
     * Called by the {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     * method to handle a request for an HTTP method, which is not known and
     * handled by this class or its extension.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * This method should be overwritten with great care. It is better to
     * overwrite the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method and
     * add support for any extension HTTP methods through an additional doXXX
     * method.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException If the error status cannot be reported back to the
     *             client.
     */
    @SuppressWarnings("unused")
    protected void doGeneric(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Tries to handle the request by calling a Java method implemented for the
     * respective HTTP request method.
     * <p>
     * This base class implentation dispatches the <em>HEAD</em>,
     * <em>GET</em>, <em>OPTIONS</em> and <em>TRACE</em> to the
     * respective <em>doXXX</em> methods and returns <code>true</code> if
     * any of these methods is requested. Otherwise <code>false</code> is just
     * returned.
     * <p>
     * Implementations of this class may overwrite this method but should first
     * call this base implementation and in case <code>false</code> is
     * returned add handling for any other method and of course return whether
     * the requested method was known or not.
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
        boolean methodKnown = true;

        String method = request.getMethod();
        if (HttpConstants.METHOD_HEAD.equals(method)) {
            doHead(request, response);
        } else if (HttpConstants.METHOD_GET.equals(method)) {
            doGet(request, response);
        } else if (HttpConstants.METHOD_OPTIONS.equals(method)) {
            doOptions(request, response);
        } else if (HttpConstants.METHOD_TRACE.equals(method)) {
            doTrace(request, response);
        } else {
            // actually we do not know the method
            methodKnown = false;
        }

        // return whether we actually knew the request method or not
        return methodKnown;
    }

    /**
     * Helper method which causes an appropriate HTTP response to be sent for an
     * unhandled HTTP request method. In case of HTTP/1.1 a 405 status code
     * (Method Not Allowed) is returned, otherwise a 400 status (Bad Request) is
     * returned.
     *
     * @param request The HTTP request from which the method and protocol values
     *            are extracted to build the appropriate message.
     * @param response The HTTP response to which the error status is sent.
     * @throws IOException Thrown if the status cannot be sent to the client.
     */
    protected void handleMethodNotImplemented(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws IOException {
        String protocol = request.getProtocol();
        String msg = "Method " + request.getMethod() + " not supported";

        if (protocol.endsWith("1.1")) {

            // for HTTP/1.1 use 405 Method Not Allowed
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);

        } else {

            // otherwise use 400 Bad Request
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);

        }
    }

    /**
     * Called by the {@link #service(ServletRequest, ServletResponse)} method to
     * handle the HTTP request. This implementation calls the
     * {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method and
     * depedending on its return value call the
     * {@link #doGeneric(SlingHttpServletRequest, SlingHttpServletResponse)} method. If
     * the {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method
     * can handle the request, the
     * {@link #doGeneric(SlingHttpServletRequest, SlingHttpServletResponse)} method is not
     * called otherwise it is called.
     * <p>
     * Implementations of this class should not generally overwrite this method.
     * Rather the {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)}
     * method should be overwritten to add support for more HTTP methods.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException Forwarded from the
     *             {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             or
     *             {@link #doGeneric(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             methods.
     * @throws IOException Forwarded from the
     *             {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             or
     *             {@link #doGeneric(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             methods.
     */
    protected void service(@Nonnull SlingHttpServletRequest request,
            @Nonnull SlingHttpServletResponse response) throws ServletException,
            IOException {

        // first try to handle the request by the known methods
        boolean methodKnown = mayService(request, response);

        // otherwise try to handle it through generic means
        if (!methodKnown) {
            doGeneric(request, response);
        }
    }

    /**
     * Forwards the request to the
     * {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     * method if the request is a HTTP request.
     * <p>
     * Implementations of this class will not generally overwrite this method.
     *
     * @param req The Servlet request
     * @param res The Servlet response
     * @throws ServletException If the request is not a HTTP request or
     *             forwarded from the
     *             {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             called.
     * @throws IOException Forwarded from the
     *             {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     *             called.
     */
    @Override
    public void service(@Nonnull ServletRequest req, @Nonnull ServletResponse res)
            throws ServletException, IOException {

        if ((req instanceof SlingHttpServletRequest)
            && (res instanceof SlingHttpServletResponse)) {

            service((SlingHttpServletRequest) req,
                (SlingHttpServletResponse) res);

        } else {

            throw new ServletException("Not a Sling HTTP request/response");

        }
    }

    /**
     * Returns the simple class name of this servlet class. Extensions of this
     * class may overwrite to return more specific information.
     */
    @Override
    public @Nonnull String getServletInfo() {
        return getClass().getSimpleName();
    }

    /**
     * Helper method called by
     * {@link #doOptions(SlingHttpServletRequest, SlingHttpServletResponse)} to calculate
     * the value of the <em>Allow</em> header sent as the response to the HTTP
     * <em>OPTIONS</em> request.
     * <p>
     * This base class implementation checks whether any doXXX methods exist for
     * <em>GET</em> and <em>HEAD</em> and returns the list of methods
     * supported found. The list returned always includes the HTTP
     * <em>OPTIONS</em> and <em>TRACE</em> methods.
     * <p>
     * Implementations of this class may overwrite this method check for more
     * methods supported by the extension (generally the same list as used in
     * the {@link #mayService(SlingHttpServletRequest, SlingHttpServletResponse)} method).
     * This base class implementation should always be called to make sure the
     * default HTTP methods are included in the list.
     *
     * @param declaredMethods The public and protected methods declared in the
     *            extension of this class.
     * @return A <code>StringBuffer</code> containing the list of HTTP methods
     *         supported.
     */
    protected StringBuffer getAllowedRequestMethods(
            Map<String, Method> declaredMethods) {
        StringBuffer allowBuf = new StringBuffer();

        // OPTIONS and TRACE are always supported by this servlet
        allowBuf.append(HttpConstants.METHOD_OPTIONS);
        allowBuf.append(", ").append(HttpConstants.METHOD_TRACE);

        // add more method names depending on the methods found
        if (declaredMethods.containsKey("doHead")
            && !declaredMethods.containsKey("doGet")) {
            allowBuf.append(", ").append(HttpConstants.METHOD_HEAD);

        } else if (declaredMethods.containsKey("doGet")) {
            allowBuf.append(", ").append(HttpConstants.METHOD_GET);
            allowBuf.append(", ").append(HttpConstants.METHOD_HEAD);

        }

        return allowBuf;
    }

    /**
     * Returns a map of methods declared by the class indexed by method name.
     * This method is called by the
     * {@link #doOptions(SlingHttpServletRequest, SlingHttpServletResponse)} method to
     * find the methods to be checked by the
     * {@link #getAllowedRequestMethods(Map)} method. Note, that only extension
     * classes of this class are considered to be sure to not account for the
     * default implementations of the doXXX methods in this class.
     *
     * @param c The <code>Class</code> to get the declared methods from
     * @return The Map of methods considered for support checking.
     */
    private Map<String, Method> getAllDeclaredMethods(Class<?> c) {
        // stop (and do not include) the AbstractSlingServletClass
        if (c == null
            || c.getName().equals(SlingSafeMethodsServlet.class.getName())) {
            return new HashMap<String, Method>();
        }

        // get the declared methods from the base class
        Map<String, Method> methodSet = getAllDeclaredMethods(c.getSuperclass());

        // add declared methods of c (maybe overwrite base class methods)
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method method : declaredMethods) {
            // only consider public and protected methods
            if (Modifier.isProtected(method.getModifiers())
                || Modifier.isPublic(method.getModifiers())) {
                methodSet.put(method.getName(), method);
            }
        }

        return methodSet;
    }

    /**
     * A response that includes no body, for use in (dumb) "HEAD" support. This
     * just swallows that body, counting the bytes in order to set the content
     * length appropriately.
     */
    private class NoBodyResponse extends SlingHttpServletResponseWrapper {

        /** The byte sink and counter */
        private NoBodyOutputStream noBody;

        /** Optional writer around the byte sink */
        private PrintWriter writer;

        /** Whether the request processor set the content length itself or not. */
        private boolean didSetContentLength;

        NoBodyResponse(SlingHttpServletResponse wrappedResponse) {
            super(wrappedResponse);
            noBody = new NoBodyOutputStream();
        }

        /**
         * Called at the end of request processing to ensure the content length
         * is set. If the processor already set the length, this method does not
         * do anything. Otherwise the number of bytes written through the
         * null-output is set on the response.
         */
        void setContentLength() {
            if (!didSetContentLength) {
                setContentLength(noBody.getContentLength());
            }
        }

        /**
         * Overwrite this to prevent setting the content length at the end of
         * the request through {@link #setContentLength()}
         */
        @Override
        public void setContentLength(int len) {
            super.setContentLength(len);
            didSetContentLength = true;
        }

        /**
         * Just return the null output stream and don't check whether a writer
         * has already been acquired.
         */
        @Override
        public ServletOutputStream getOutputStream() {
            return noBody;
        }

        /**
         * Just return the writer to the null output stream and don't check
         * whether an output stram has already been acquired.
         */
        @Override
        public PrintWriter getWriter() throws UnsupportedEncodingException {
            if (writer == null) {
                OutputStreamWriter w;

                w = new OutputStreamWriter(noBody, getCharacterEncoding());
                writer = new PrintWriter(w);
            }
            return writer;
        }
    }

    /**
     * Simple ServletOutputStream which just does not write but counts the bytes
     * written through it. This class is used by the NoBodyResponse.
     */
    private class NoBodyOutputStream extends ServletOutputStream {

        private int contentLength = 0;

        /**
         * @return the number of bytes "written" through this stream
         */
        int getContentLength() {
            return contentLength;
        }

        @Override
        public void write(int b) {
            contentLength++;
        }

        @Override
        public void write(byte buf[], int offset, int len) {
            if (len >= 0) {
                contentLength += len;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // nothing to do
        }
    }

}
