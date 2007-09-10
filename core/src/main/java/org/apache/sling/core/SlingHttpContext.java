/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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
        mimeTypeService = new MimeTypeService() {
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
        return mimeTypeService.getMimeType(name);
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
