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
package org.apache.sling.engine.impl.parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.slf4j.LoggerFactory;

public class ParameterSupport {

    /**
     * The name of the form encoding request parameter indicating the character
     * encoding of submitted request parameters. This request parameter
     * overwrites any value of the {@code ServletRequest.getCharacterEncoding()}
     * method (which unfortunately happens to be returning {@code null} most of
     * the time.
     */
    public final static String PARAMETER_FORMENCODING = "_charset_";

    // name of the request attribute caching the ParameterSupport instance
    // used during the request
    private static final String ATTR_NAME = ParameterSupport.class.getName();

    /**
     * The name of the request attribute to set to get the Jetty 6 (and older)
     * servlet container to decode the request query using ISO-8859-1 encoding
     * (value is "org.mortbay.jetty.Request.queryEncoding").
     */
    private static final String ATTR_JETTY_QUERY_ENCODING_1 = "org.mortbay.jetty.Request.queryEncoding";

    /**
     * The name of the request attribute to set to get the Jetty 7 (and newer)
     * servlet container to decode the request query using ISO-8859-1 encoding
     * (value is "org.eclipse.jetty.server.Request.queryEncoding").
     */
    private static final String ATTR_JETTY_QUERY_ENCODING_2 = "org.eclipse.jetty.server.Request.queryEncoding";

    /** Content type signaling parameters in request body */
    private static final String WWW_FORM_URL_ENC = "application/x-www-form-urlencoded";

    /**
     * The maximum size allowed for <tt>multipart/form-data</tt>
     * requests
     *
     * <p>The default is <tt>-1L</tt>, which means unlimited.
     */
    private static long maxRequestSize = -1L;

    /**
     * The directory location where files will be stored
     */
    private static File location = null;

    /**
     * The maximum size allowed for uploaded files.
     *
     * <p>The default is <tt>-1L</tt>, which means unlimited.
     */
    private static long maxFileSize = -1L;

    /**
     * The size threshold after which the file will be written to disk
     */
    private static int fileSizeThreshold = 256000;

    private final HttpServletRequest servletRequest;

    private ParameterMap postParameterMap;

    private boolean requestDataUsed;

    /**
     * Returns the {@code ParameterSupport} instance supporting request
     * parameter for the give {@code request}. For a single request only a
     * single instance is actually used. This single instance is cached as a
     * request attribute. If such an attribute already exists which is not an
     * instance of this class, the request parameter is replaced.
     *
     * @param request The {@code HttpServletRequest} for which to return request
     *            parameter support.
     * @return The {@code ParameterSupport} for the given request.
     */
    public static ParameterSupport getInstance(HttpServletRequest request) {
        Object instance = request.getAttribute(ATTR_NAME);
        if (!(instance instanceof ParameterSupport)) {
            instance = new ParameterSupport(request);
            request.setAttribute(ATTR_NAME, instance);
        }
        return (ParameterSupport) instance;
    }

    /**
     * Returns a {@code HttpServletRequestWrapper} which implements request
     * parameter access backed by an instance of the {@code ParameterSupport}
     * class.
     * <p>
     * If used in a Servlet API 3 context, this method supports the additional
     * {@code Part} API introduced with Servlet API 3.
     *
     * @param request The {@code HttpServletRequest} to wrap
     * @return The wrapped {@code request}
     */
    public static HttpServletRequestWrapper getParameterSupportRequestWrapper(final HttpServletRequest request) {

        try {
            if (request.getClass().getMethod("getServletContext") != null) {
                return new ParameterSupportHttpServletRequestWrapper3(request);
            }
        } catch (Exception e) {
            // If the getServletContext method does not exist or
            // is not visible, fall back to a Servlet API 2.x wrapper
        }

        return new ParameterSupportHttpServletRequestWrapper2x(request);
    }

    static void configure(final long maxRequestSize, final String location, final long maxFileSize,
            final int fileSizeThreshold) {
        ParameterSupport.maxRequestSize = (maxRequestSize > 0) ? maxRequestSize : -1;
        ParameterSupport.location = (location != null) ? new File(location) : null;
        ParameterSupport.maxFileSize = (maxFileSize > 0) ? maxFileSize : -1;
        ParameterSupport.fileSizeThreshold = (fileSizeThreshold > 0) ? fileSizeThreshold : 256000;
    }

    private ParameterSupport(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;

            // SLING-559: Hack to get Jetty into decoding the request
            // query with ISO-8859-1 as stipulated by the servlet
            // spec. Other containers ignore this parameter
            servletRequest.setAttribute(ATTR_JETTY_QUERY_ENCODING_1, Util.ENCODING_DIRECT);
            servletRequest.setAttribute(ATTR_JETTY_QUERY_ENCODING_2, Util.ENCODING_DIRECT);
        }

    private HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public boolean requestDataUsed() {
        return this.requestDataUsed;
    }

    public String getParameter(String name) {
        return getRequestParameterMapInternal().getStringValue(name);
    }

    public String[] getParameterValues(String name) {
        return getRequestParameterMapInternal().getStringValues(name);
    }

    public Map<String, String[]> getParameterMap() {
        return getRequestParameterMapInternal().getStringParameterMap();
    }

    public Enumeration<String> getParameterNames() {
        return new Enumeration<String>() {
            private final Iterator<String> base = ParameterSupport.this.getRequestParameterMapInternal().keySet().iterator();

            public boolean hasMoreElements() {
                return this.base.hasNext();
            }

            public String nextElement() {
                return this.base.next();
            }
        };
    }

    public RequestParameter getRequestParameter(String name) {
        return getRequestParameterMapInternal().getValue(name);
    }

    public RequestParameter[] getRequestParameters(String name) {
        return getRequestParameterMapInternal().getValues(name);
    }

    public Object getPart(String name) {
        return getRequestParameterMapInternal().getPart(name);
    }

    public Collection<?> getParts() {
        return getRequestParameterMapInternal().getParts();
    }

    public RequestParameterMap getRequestParameterMap() {
        return getRequestParameterMapInternal();
    }

    public List<RequestParameter> getRequestParameterList() {
        return getRequestParameterMapInternal().getRequestParameterList();
    }

    private ParameterMap getRequestParameterMapInternal() {
        if (this.postParameterMap == null) {

            // SLING-152 Get parameters from the servlet Container
            ParameterMap parameters = new ParameterMap();
            getContainerParameters(parameters);

            // only read input in case of multipart-POST not handled
            // by the servlet container
            if ("POST".equals(this.getServletRequest().getMethod())) {
                if (ServletFileUpload.isMultipartContent(new ServletRequestContext(
                    this.getServletRequest()))) {
                    this.parseMultiPartPost(parameters);
                    this.requestDataUsed = true;
                }
            }

            // apply any form encoding (from '_charset_') in the parameter map
            Util.fixEncoding(parameters);

            this.postParameterMap = parameters;
        }
        return this.postParameterMap;
    }

    private void getContainerParameters(ParameterMap parameters) {

        // SLING-508 Try to force servlet container to decode parameters
        // as ISO-8859-1 such that we can recode later
        String encoding = getServletRequest().getCharacterEncoding();
        if (encoding == null) {
            encoding = Util.ENCODING_DIRECT;
            try {
                getServletRequest().setCharacterEncoding(encoding);
            } catch (UnsupportedEncodingException uee) {
                throw new SlingUnsupportedEncodingException(uee);
            }
        }

        final String query = getServletRequest().getQueryString();
        if (query != null) {
            try {
                InputStream input = Util.toInputStream(query);
                Util.parseQueryString(input, encoding, parameters, false);
            } catch (UnsupportedEncodingException e) {
                // TODO: don't expect this, thus log !!
            } catch (IOException e) {
                // TODO: don't expect this, thus log !!
            }
        }

        // only read input in case of multipart-POST not handled
        // by the servlet container
        if ("POST".equals(this.getServletRequest().getMethod())
            && WWW_FORM_URL_ENC.equalsIgnoreCase(this.getServletRequest().getContentType())) {
            try {
                InputStream input = this.getServletRequest().getInputStream();
                Util.parseQueryString(input, encoding, parameters, false);
            } catch (IllegalArgumentException e) {
                // TODO: don't expect this, thus log !!
            } catch (UnsupportedEncodingException e) {
                // TODO: don't expect this, thus log !!
            } catch (IOException e) {
                // TODO: don't expect this, thus log !!
            }
            this.requestDataUsed = true;
        }

//        final Map<?, ?> pMap = getServletRequest().getParameterMap();
//        for (Map.Entry<?, ?> entry : pMap.entrySet()) {
//
//            final String name = (String) entry.getKey();
//            final String[] values = (String[]) entry.getValue();
//
//            for (int i = 0; i < values.length; i++) {
//                parameters.addParameter(name, new ContainerRequestParameter(
//                    values[i], encoding));
//            }
//
//        }
        }

    private void parseMultiPartPost(ParameterMap parameters) {

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();
        upload.setSizeMax(ParameterSupport.maxRequestSize);
        upload.setFileSizeMax(ParameterSupport.maxFileSize);
        upload.setFileItemFactory(new DiskFileItemFactory(ParameterSupport.fileSizeThreshold,
            ParameterSupport.location));

        RequestContext rc = new ServletRequestContext(this.getServletRequest()) {
            public String getCharacterEncoding() {
                String enc = super.getCharacterEncoding();
                return (enc != null) ? enc : Util.ENCODING_DIRECT;
            }
        };

        // Parse the request
        List<?> /* FileItem */items = null;
        try {
            items = upload.parseRequest(rc);
        } catch (FileUploadException fue) {
            LoggerFactory.getLogger(getClass()).error("parseMultiPartPost: Error parsing request", fue);
        }

        if (items != null && items.size() > 0) {
            for (Iterator<?> ii = items.iterator(); ii.hasNext();) {
                FileItem fileItem = (FileItem) ii.next();
                RequestParameter pp = new MultipartRequestParameter(fileItem);
                parameters.addParameter(pp, false);
            }
        }
    }

}