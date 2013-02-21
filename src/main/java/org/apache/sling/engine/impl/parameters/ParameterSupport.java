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

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.iterators.IteratorEnumeration;
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

    private static final String ATTR_NAME = ParameterSupport.class.getName();

    /**
     * The name of the request attribute to set to get the Jetty servlet
     * container to decode the request query using ISO-8859-1 encoding (value is
     * "org.mortbay.jetty.Request.queryEncoding").
     */
    private static final String ATTR_JETTY_QUERY_ENCODING = "org.mortbay.jetty.Request.queryEncoding";

    private final HttpServletRequest servletRequest;

    private ParameterMap postParameterMap;

    private boolean requestDataUsed;

    /**
     * Sets the default encoding used to decode request parameters if the
     * <code>_charset_</code> request parameter is not set (or is not set to an
     * encoding supported by the platform). By default this default encoding is
     * <code>ISO-8859-1</code>. For applications which alway use the same
     * encoding this default can be changed.
     *
     * @param encoding The default encoding to be used. If this encoding is
     *            <code>null</code> or not supported by the platform the current
     *            default encoding remains unchanged.
     */
    public static void setDefaultParameterEncoding(final String encoding) {
        Util.setDefaultFixEncoding(encoding);
    }

    public static ParameterSupport getInstance(ServletRequest servletRequest) {
        ParameterSupport instance = (ParameterSupport) servletRequest.getAttribute(ATTR_NAME);
        if (instance == null) {
            instance = new ParameterSupport((HttpServletRequest) servletRequest);
            servletRequest.setAttribute(ATTR_NAME, instance);

            // SLING-559: Hack to get Jetty into decoding the request
            // query with ISO-8859-1 as stipulated by the servlet
            // spec. Other containers ignore this parameter
            servletRequest.setAttribute(ATTR_JETTY_QUERY_ENCODING,
                Util.ENCODING_DIRECT);
        }
        return instance;
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

    @SuppressWarnings("unchecked")
    public Enumeration<String> getParameterNames() {
        return new IteratorEnumeration(
            this.getRequestParameterMapInternal().keySet().iterator());
    }

    public RequestParameter getRequestParameter(String name) {
        return getRequestParameterMapInternal().getValue(name);
    }

    public RequestParameter[] getRequestParameters(String name) {
        return getRequestParameterMapInternal().getValues(name);
    }

    public RequestParameterMap getRequestParameterMap() {
        return getRequestParameterMapInternal();
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

        final Map<?, ?> pMap = getServletRequest().getParameterMap();
        for (Map.Entry<?, ?> entry : pMap.entrySet()) {

            final String name = (String) entry.getKey();
            final String[] values = (String[]) entry.getValue();

            for (int i = 0; i < values.length; i++) {
                parameters.addParameter(name, new ContainerRequestParameter(
                    values[i], encoding));
            }

        }
    }

    private void parseMultiPartPost(ParameterMap parameters) {
        // parameters not read yet, read now
        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(256000);

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(-1);

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
                parameters.addParameter(fileItem.getFieldName(), pp);
            }
        }
    }

}