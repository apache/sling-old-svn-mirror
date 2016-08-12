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
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
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

    /**
     * Request attribute which is set if the current request is "started"
     * by calling {@link org.apache.sling.engine.SlingRequestProcessor#processRequest(HttpServletRequest, HttpServletResponse, ResourceResolver)}
     * This marker is evaluated in {@link #getRequestParameterMapInternal()}.
     */
    public final static String MARKER_IS_SERVICE_PROCESSING = ParameterSupport.class.getName() + "/ServiceProcessingMarker";

    // name of the request attribute caching the ParameterSupport instance
    // used during the request
    private static final String ATTR_NAME = ParameterSupport.class.getName();

    /** Content type signaling parameters in request body */
    private static final String WWW_FORM_URL_ENC = "application/x-www-form-urlencoded";

    /** name of the header used to identify an upload mode */
    public static final String SLING_UPLOADMODE_HEADER = "Sling-uploadmode";
    /** name of the parameter used to identify upload mode */
    public static final String UPLOADMODE_PARAM = "uploadmode";
    /** request attribute that stores the parts iterator when streaming */
    public static final String REQUEST_PARTS_ITERATOR_ATTRIBUTE = "request-parts-iterator";
    /** value of upload mode header/parameter indicating streaming is requested */
    public static final String STREAM_UPLOAD = "stream";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

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

    /**
     * Check for additional parameters from the container.
     * TODO - We can remove this once we move engine to Servlet 3.1
     */
    private static boolean checkForAdditionalParameters = false;

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
     *
     * @param request The {@code HttpServletRequest} to wrap
     * @return The wrapped {@code request}
     */
    public static HttpServletRequestWrapper getParameterSupportRequestWrapper(final HttpServletRequest request) {

        return new ParameterSupportHttpServletRequestWrapper(request);
    }

    static void configure(final long maxRequestSize, final String location, final long maxFileSize,
            final int fileSizeThreshold,
            final boolean checkForAdditionalParameters) {
        ParameterSupport.maxRequestSize = (maxRequestSize > 0) ? maxRequestSize : -1;
        ParameterSupport.location = (location != null) ? new File(location) : null;
        ParameterSupport.maxFileSize = (maxFileSize > 0) ? maxFileSize : -1;
        ParameterSupport.fileSizeThreshold = (fileSizeThreshold > 0) ? fileSizeThreshold : 256000;
        ParameterSupport.checkForAdditionalParameters = checkForAdditionalParameters;
    }

    private ParameterSupport(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
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

            @Override
            public boolean hasMoreElements() {
                return this.base.hasNext();
            }

            @Override
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

            // SLING-152 Get parameters from the servlet Container
            ParameterMap parameters = new ParameterMap();

            // fallback is only used if this request has been started by a service call
            boolean useFallback = getServletRequest().getAttribute(MARKER_IS_SERVICE_PROCESSING) != null;
            boolean addContainerParameters = false;
            // Query String
            final String query = getServletRequest().getQueryString();
            if (query != null) {
                try {
                    InputStream input = Util.toInputStream(query);
                    Util.parseQueryString(input, encoding, parameters, false);
                    addContainerParameters = checkForAdditionalParameters;
                } catch (IllegalArgumentException e) {
                    this.log.error("getRequestParameterMapInternal: Error parsing request", e);
                } catch (UnsupportedEncodingException e) {
                    throw new SlingUnsupportedEncodingException(e);
                } catch (IOException e) {
                    this.log.error("getRequestParameterMapInternal: Error parsing request", e);
                }
                useFallback = false;
            } else {
                addContainerParameters = checkForAdditionalParameters;
                useFallback = true;
            }

            // POST requests
            final boolean isPost = "POST".equals(this.getServletRequest().getMethod());
            if (isPost) {
                // WWW URL Form Encoded POST
                if (isWWWFormEncodedContent(this.getServletRequest())) {
                    try {
                        InputStream input = this.getServletRequest().getInputStream();
                        Util.parseQueryString(input, encoding, parameters, false);
                        addContainerParameters = checkForAdditionalParameters;
                    } catch (IllegalArgumentException e) {
                        this.log.error("getRequestParameterMapInternal: Error parsing request", e);
                    } catch (UnsupportedEncodingException e) {
                        throw new SlingUnsupportedEncodingException(e);
                    } catch (IOException e) {
                        this.log.error("getRequestParameterMapInternal: Error parsing request", e);
                    }
                    this.requestDataUsed = true;
                    useFallback = false;
                }

                // Multipart POST
                if (ServletFileUpload.isMultipartContent(new ServletRequestContext(this.getServletRequest()))) {
                    if (isStreamed(parameters, this.getServletRequest())) {
                        // special case, the request is Mutipart and streamed processing has been requested
                        try {
                            this.getServletRequest().setAttribute(REQUEST_PARTS_ITERATOR_ATTRIBUTE, new RequestPartsIterator(this.getServletRequest()));
                            this.log.debug("getRequestParameterMapInternal: Iterator<javax.servlet.http.Part> available as request attribute  named request-parts-iterator");
                        } catch (IOException e) {
                            this.log.error("getRequestParameterMapInternal: Error parsing mulmultipart streamed requesttipart streamed request", e);
                        } catch (FileUploadException e) {
                            this.log.error("getRequestParameterMapInternal: Error parsing mulmultipart streamed request", e);
                        }
                        // The request data has been passed to the RequestPartsIterator, hence from a RequestParameter pov its been used, and must not be used again.
                        this.requestDataUsed = true;
                        // must not try and get anything from the request at this point so avoid jumping through the stream.
                        addContainerParameters = false;
                        useFallback = false;
                    } else {
                        this.parseMultiPartPost(parameters);
                        this.requestDataUsed = true;
                        addContainerParameters = checkForAdditionalParameters;
                        useFallback = false;
                    }
                }
            }
            if ( useFallback ) {
                getContainerParameters(parameters, encoding, true);
            } else  if ( addContainerParameters ) {
                getContainerParameters(parameters, encoding, false);
            }
            // apply any form encoding (from '_charset_') in the parameter map
            Util.fixEncoding(parameters);

            this.postParameterMap = parameters;
        }
        return this.postParameterMap;
    }


    /**
     * Checks to see if there is an upload mode header or uploadmode parameter indicating the request is
     * to be streamed from the client to the server.
     * @param parameters parameters processed from the query string only.
     * @param servletRequest the servlet request, where the body has not been processed.
     * @return true if the request was made with streaming in mind.
     */
    private boolean isStreamed(ParameterMap parameters, HttpServletRequest servletRequest) {
        if ( STREAM_UPLOAD.equals(servletRequest.getHeader(SLING_UPLOADMODE_HEADER)) ) {
            return true;
        }
        RequestParameter[] rp = parameters.get(UPLOADMODE_PARAM);
        return ( rp != null && rp.length == 1 && STREAM_UPLOAD.equals(rp[0].getString()));
    }

    private void getContainerParameters(final ParameterMap parameters, final String encoding, final boolean alwaysAdd) {
        final Map<?, ?> pMap = getServletRequest().getParameterMap();
        for (Map.Entry<?, ?> entry : pMap.entrySet()) {

            final String name = (String) entry.getKey();
            if ( alwaysAdd || !parameters.containsKey(name) ) {
                final String[] values = (String[]) entry.getValue();

                for (int i = 0; i < values.length; i++) {
                    parameters.addParameter(new ContainerRequestParameter(
                        name, values[i], encoding), false);
                }
            }
        }
    }

    private static final boolean isWWWFormEncodedContent(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }

        // This check assumes the content type ends after the WWW_FORM_URL_ENC
        // or continues with blank or semicolon. It will probably break if
        // the content type is some string extension of WWW_FORM_URL_ENC
        // such as "application/x-www-form-urlencoded-bla"
        if (contentType.toLowerCase(Locale.ENGLISH).startsWith(WWW_FORM_URL_ENC)) {
            return true;
        }

        return false;
    }


    private void parseMultiPartPost(ParameterMap parameters) {

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();
        upload.setSizeMax(ParameterSupport.maxRequestSize);
        upload.setFileSizeMax(ParameterSupport.maxFileSize);
        upload.setFileItemFactory(new DiskFileItemFactory(ParameterSupport.fileSizeThreshold,
            ParameterSupport.location));

        RequestContext rc = new ServletRequestContext(this.getServletRequest()) {
            @Override
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
            this.log.error("parseMultiPartPost: Error parsing request", fue);
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