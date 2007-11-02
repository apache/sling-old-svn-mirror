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
package org.apache.sling.microsling.contenttype;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.microsling.request.helpers.AbstractFilter;

/** A Filter that sets the desired Response Content-Type based
 *  on the Request attributes.
 */
public class ResponseContentTypeResolverFilter extends AbstractFilter {

    // TODO: Is text/plain ok or should this rather be text/html ??
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "text/plain";

    @Override
    protected void init() {
        // no further initialization
    }

    public void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        request = new ContentTypedRequest((SlingHttpServletRequest) request);
        filterChain.doFilter(request, response);

    }

    private class ContentTypedRequest extends SlingHttpServletRequestWrapper {

        private String responseContentType;

        ContentTypedRequest(SlingHttpServletRequest request) {
            super(request);
        }

        @Override
        public String getResponseContentType() {
            if (responseContentType == null) {
                String ext = getSlingRequest().getRequestPathInfo().getExtension();
                if (ext != null) {
                    String file = "dummy." + ext;
                    final String contentType = getFilterConfig().getServletContext().getMimeType(
                        file);
                    if (contentType != null) {
                        responseContentType = contentType;
                    } else {
                        responseContentType = DEFAULT_RESPONSE_CONTENT_TYPE;
                    }
                } else {
                    responseContentType = DEFAULT_RESPONSE_CONTENT_TYPE;
                }
            }

            return responseContentType;
        }

        @Override
        public Enumeration<String> getResponseContentTypes() {
            Collection<String> c = Collections.singleton(getResponseContentType());
            return Collections.enumeration(c);
        }
    }
}
