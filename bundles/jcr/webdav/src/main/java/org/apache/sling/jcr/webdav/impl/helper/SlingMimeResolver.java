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

import org.apache.jackrabbit.server.io.MimeResolver;
import org.apache.sling.commons.mime.MimeTypeService;

public class SlingMimeResolver extends MimeResolver {

    private final MimeTypeService mimeTypeService;
    
    public SlingMimeResolver(MimeTypeService mimeTypeService) {
        this.mimeTypeService = mimeTypeService;
    }

    @Override
    public String getMimeType(String filename) {
        String type = mimeTypeService.getMimeType(filename);
        if (type == null) {
            type = getDefaultMimeType();
        }
        return type;
    }
    
}
