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
package org.apache.sling.rewriter.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.rewriter.ProcessingContext;

/**
 * An implementation of a processing context for a servlet.
 */
public class ServletProcessingContext implements ProcessingContext {

    /** The current request. */
    private final SlingHttpServletRequest request;

    /** The current response. */
    private final SlingHttpServletResponse response;

    /** The original response. */
    private final SlingHttpServletResponse originalResponse;

    /** response content type */
    private final String contentType;

    /**
     * Initializes a new instance.
     */
    public ServletProcessingContext(SlingHttpServletRequest request,
                                    SlingHttpServletResponse response,
                                    SlingHttpServletResponse originalResponse,
                                    String contentType) {
        this.request = request;
        this.response = response;
        this.originalResponse = originalResponse;
        this.contentType = contentType;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingContext#getContentType()
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingContext#getRequest()
     */
    public SlingHttpServletRequest getRequest() {
        return this.request;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingContext#getResponse()
     */
    public SlingHttpServletResponse getResponse() {
        return this.response;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingContext#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        return this.originalResponse.getWriter();
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingContext#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException {
        return this.originalResponse.getOutputStream();
    }
}
