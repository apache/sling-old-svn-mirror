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
package org.apache.sling.api.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>RequestParameter</code> class represents a single parameter sent
 * with the client request. Instances of this class are returned by the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(String)},
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(String)} and
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()} method.
 *
 * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(String)
 * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(String)
 * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
 */
@ProviderType
public interface RequestParameter {

    /**
     * Determines whether or not this instance represents a simple form field or
     * an uploaded file.
     *
     * @return <code>true</code> if the instance represents a simple form
     *         field; <code>false</code> if it represents an uploaded file.
     */
    boolean isFormField();

    /**
     * Returns the content type passed by the browser or <code>null</code> if
     * not defined.
     *
     * @return The content type passed by the browser or <code>null</code> if
     *         not defined.
     */
    String getContentType();

    /**
     * Returns the size in bytes of the parameter.
     *
     * @return The size in bytes of the parameter.
     */
    long getSize();

    /**
     * Returns the contents of the parameter as an array of bytes.
     *
     * @return The contents of the parameter as an array of bytes.
     */
    byte[] get();

    /**
     * Returns an InputStream that can be used to retrieve the contents of the
     * file.
     * <p>
     * Each call to this method returns a new {@code InputStream} to the
     * request parameter data. Make sure to close the stream to prevent
     * leaking resources.
     *
     * @return An InputStream that can be used to retrieve the contents of the
     *         file.
     * @throws IOException if an error occurs.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns the original filename in the client's filesystem, as provided by
     * the browser (or other client software). In most cases, this will be the
     * base file name, without path information. However, some clients, such as
     * the Opera browser, do include path information.
     *
     * @return The original filename in the client's filesystem.
     */
    String getFileName();

    /**
     * Returns the contents of the parameter as a String, using the default
     * character encoding. This method uses {@link #get()} to retrieve the
     * contents of the item.
     *
     * @return The contents of the parameter, as a string.
     */
    String getString();

    /**
     * Returns the contents of the parameter as a String, using the specified
     * encoding. This method uses link {@link #get()} to retrieve the contents
     * of the item.
     *
     * @param encoding The character encoding to use.
     * @return The contents of the parameter, as a string.
     * @throws UnsupportedEncodingException if the requested character encoding
     *             is not available.
     */
    String getString(String encoding) throws UnsupportedEncodingException;

}
