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
package org.apache.sling.core.impl.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.core.impl.helper.RequestData;

public class ParameterSupport {

    private RequestData requestData;

    private ParameterMap postParameterMap;

    private boolean requestDataUsed;

    public ParameterSupport(RequestData servletRequest) {
        this.requestData = servletRequest;
    }

    protected RequestData getRequestData() {
        return this.requestData;
    }

    protected HttpServletRequest getServletRequest() {
        return this.getRequestData().getServletRequest();
    }

    public boolean requestDataUsed() {
        return this.requestDataUsed;
    }

    public String getParameter(String name) {
        RequestParameter param = this.getRequestParameter(name);
        return (param != null) ? param.getString() : null;
    }

    public String[] getParameterValues(String name) {
        return this.getParameterMap().get(name);
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
            // actually we also have to integrate the standard servlet
            // parameters from the URL queryString !!
            ParameterMap parameters = new ParameterMap();
            this.parseQueryString(parameters);

            // read post parametervalues
            if ("POST".equals(this.getServletRequest().getMethod())) {
                if (ServletFileUpload.isMultipartContent(new ServletRequestContext(
                    this.getServletRequest()))) {
                    this.parseMultiPartPost(parameters);
                    this.requestDataUsed = true;
                } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(this.getServletRequest().getContentType())) {
                    this.parseFormEncodedPost(parameters);
                    this.requestDataUsed = true;
                }
            }

            // apply any FormEncoding in the parameter map
            Util.fixEncoding(parameters);

            this.postParameterMap = parameters;
        }
        return this.postParameterMap;
    }

    private void parseQueryString(ParameterMap parameters) {
        String queryString = SlingRequestPaths.getQueryString(requestData.getSlingRequest());
        InputStream input = Util.getInputStream(queryString);
        try {
            Util.parse(input, Util.ENCODING_DEFAULT, parameters, true);
        } catch (IOException ioe) {
            // TODO: log
        }
    }

    protected void parseFormEncodedPost(ParameterMap parameters) {
        try {
            Util.parse(this.getServletRequest().getInputStream(),
                this.getServletRequest().getCharacterEncoding(), parameters, false);
        } catch (IOException ioe) {
            // TODO: log
        } catch (Throwable t) {
            // TODO: log
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
        try {
            List<?> /* FileItem */items = upload.parseRequest(rc);
            for (Iterator<?> ii = items.iterator(); ii.hasNext();) {
                FileItem fileItem = (FileItem) ii.next();
                RequestParameter pp = new MultipartRequestParameter(fileItem);
                parameters.addParameter(fileItem.getFieldName(), pp);
            }
        } catch (FileUploadException fue) {
            // TODO: log
        }
    }

}