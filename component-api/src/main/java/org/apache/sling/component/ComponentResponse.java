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
package org.apache.sling.component;

import javax.servlet.http.HttpServletResponse;

/**
 * The <code>ComponentResponse</code> defines the interface to assist a
 * component in creating and sending a response to the client.
 */
public interface ComponentResponse extends HttpServletResponse {

    /**
     * Property to set the expiration time in seconds for this response using
     * the <code>setProperty</code> method (value is
     * "component.expiration-cache").
     * <p>
     * If the expiration value is set to 0, caching is disabled for this
     * component; if the value is set to -1, the cache does not expire. The
     * Component Framework is free to apply more heuristics on the response
     * caching process. In no case must the response be cached if this property
     * is set to zero.
     */
    static final String EXPIRATION_CACHE = "component.expiration-cache";

    /**
     * Returns the content type used for the MIME body sent in this response.
     * The content type proper must have been specified using
     * <code>setContentType(String)</code> before the response is committed.
     * If no content type has been specified, this method returns null. If a
     * content type has been specified and a character encoding has been
     * explicitly or implicitly specified as described in
     * <code>getCharacterEncoding()</code>, the charset parameter is included
     * in the string returned. If no character encoding has been specified, the
     * charset parameter is omitted.
     *
     * @return a String specifying the content type, for example, <code>text/html;
     *         charset=UTF-8</code>, or null
     */
    String getContentType();

    /**
     * The value returned by this method should be prefixed or appended to
     * elements, such as JavaScript variables or function names, to ensure they
     * are unique in the context of the component page.
     *
     * @return the namespace
     */
    String getNamespace();

    /**
     * Sets the character encoding (MIME charset) of the response being sent to
     * the client, for example, to UTF-8. If the character encoding has already
     * been set by <code>setContentType(String)</code> or
     * <code>setLocale(Locale)</code>, this method overrides it. Calling
     * <code>setContentType(String)</code> with the String of
     * <code>text/html</code> and calling this method with the String of UTF-8
     * is equivalent with calling <code>setContentType</code> with the String
     * of <code>text/html; charset=UTF-8</code>.
     * <p>
     * This method can be called repeatedly to change the character encoding.
     * This method has no effect if it is called after getWriter has been called
     * or after the response has been committed.
     * <p>
     * Containers must communicate the character encoding used for the servlet
     * response’s writer to the client if the protocol provides a way for doing
     * so. In the case of HTTP, the character encoding is communicated as part
     * of the Content-Type header for text media types. Note that the character
     * encoding cannot be communicated via HTTP headers if the servlet does not
     * specify a content type; however, it is still used to encode text written
     * via the servlet response’s writer.
     *
     * @param charset a String specifying only the character set defined by IANA
     *            Character Sets
     *            (http://www.iana.org/assignments/character-sets) Since: 2.4
     *            See Also: setContentType(String)
     */
    void setCharacterEncoding(String charset);

}
