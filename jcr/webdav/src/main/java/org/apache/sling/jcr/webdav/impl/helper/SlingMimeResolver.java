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
package org.apache.sling.jcr.webdav.impl.helper;

import javax.servlet.ServletContext;

import org.apache.jackrabbit.server.io.MimeResolver;

public class SlingMimeResolver extends MimeResolver {

    private final ServletContext servletContext;
    
    public SlingMimeResolver(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public String getMimeType(String filename) {
        String type = servletContext.getMimeType(filename);
        if (type == null) {
            type = getDefaultMimeType();
        }
        return type;
    }
    
}
