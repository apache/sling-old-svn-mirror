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
package org.apache.sling.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>MimeTypeService</code> TODO
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 */
public interface MimeTypeService {

    /**
     * Returns the MIME type of the extension of the given <code>name</code>.
     * The extension is the part of the name after the last dot. If the name
     * does not contain a dot, the name as a whole is assumed to be the
     * extension.
     * 
     * @param name The name for which the MIME type is to be returned.
     * @return The MIME type for the extension of the name. If the extension
     *         cannot be mapped to a MIME type or <code>name</code> is
     *         <code>null</code>, <code>null</code> is returned.
     * @see #getExtension(String)
     */
    String getMimeType(String name);

    /**
     * Returns the primary name extension to which the given
     * <code>mimeType</code> maps. The returned extension must map to the
     * given <code>mimeType</code> when fed to the
     * {@link #getMimeType(String)} method. In other words, the expression
     * <code>mimeType.equals(getMimeType(getExtension(mimeType)))</code> must
     * always be <code>true</code> for any non-<code>null</code> MIME type.
     * <p>
     * A MIME type may be mapped to multiple extensions (e.g.
     * <code>text/plain</code> to <code>txt</code>, <code>log</code>,
     * ...). This method is expected to returned one of those extensions. It is
     * up to the implementation to select an appropriate extension if multiple
     * mappings exist for a single MIME type.
     * 
     * @param mimeType The MIME type whose primary extension is requested.
     * @return A extension which maps to the given MIME type or
     *         <code>null</code> if no such mapping exists.
     * @see #getMimeType(String)
     */
    String getExtension(String mimeType);

    void registerMimeType(String mimeType, String... extensions);
    
    void registerMimeType(InputStream mimeTabStream) throws IOException;
}
