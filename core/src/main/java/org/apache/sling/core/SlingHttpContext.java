/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.mime.MimeTypeService;
import org.osgi.service.http.HttpContext;

/**
 * The <code>SlingHttpContext</code> implements the OSGi
 * <code>HttpContext</code> interface to provide specialized support for
 * Sling.
 */
public class SlingHttpContext implements HttpContext {

    private MimeTypeService mimeTypeService;

    SlingHttpContext(MimeTypeService mimeTypeService) {
        this.mimeTypeService = mimeTypeService;
    }

    void dispose() {
        // replace the official implementation with a dummy one to prevent NPE
        this.mimeTypeService = new MimeTypeService() {
            public String getMimeType(String name) {
                return null;
            }

            public String getExtension(String mimeType) {
                return null;
            }

            public void registerMimeType(InputStream mimeTabStream) {
            }

            public void registerMimeType(String mimeType, String... extensions) {
            }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String name) {
        return this.mimeTypeService.getMimeType(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
     */
    public URL getResource(String name) {
        // This context cannot provide any resources, so we just return nothing
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) {

        /*
         * Currently we do not handle security in the context but in the
         * SlingServlet as an AuthenticationFilter. It might be worth it
         * considering to move the authentication from the AuthenticationFilter
         * to this context.
         */

        return true;
    }
}
