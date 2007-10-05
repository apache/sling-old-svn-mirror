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
package org.apache.sling.core.resolver;

import org.apache.sling.content.ContentManager;

/**
 * The <code>ContentResolver</code> interface defines the service API of the
 * Sling content resolver. It is used for request processing to resolve the
 * client request URL to the <code>Content</code>, selectors, extension and
 * suffix part of the URL. In addition, the Sling <code>RequestDispatcher</code>
 * implementation uses the content resolver to resolve paths of included
 * content. Finally, the content resolver may be used to create URLs which when
 * applied to the resolver method yield the <code>Content</code> object.
 */
public interface ContentResolver {

    /**
     * Resolves the request URI to a {@link ResolvedURL} providing the Content
     * object as well as the selector string, extension and suffix.
     *
     * @param cm The ContentManager used to retrieve the Content object
     * @param requestURI The request URI to be decomposed
     * @return A {@link ResolvedURL} containing the decomposed data of the
     *         request URI or <code>null</code> if the request URI cannot be
     *         mapped to a Content object.
     */
    ResolvedURL resolveURL(ContentManager cm, String requestURI);

    /**
     * Maps a Content path to an URI, which when fed to the
     * {@link #resolveURL(ContentManager, String)} method returns the original
     * Content object. This method may be used to get the external (URI)
     * representation of a Content path, which is guaranteed to map back to the
     * same handle, when used in a request.
     *
     * @param path The Content path to map to an URI
     * @return The external (URI) representation of the Content path.
     */
    String pathToURL(String path);

    /**
     * Maps a Content path to an URI, which when fed to the
     * {@link #resolveURL(ContentManager, String)} method returns the original
     * Content object. This method may be used to get the external (URI)
     * representation of a Content path, which is guaranteed to map back to the
     * same handle, when used in a request.
     *
     * @param prefix A prefix to prepend to the URI, ignored if empty or
     *            <code>null</code>
     * @param path The Content path to map to an URI
     * @param suffix A suffix to append to the URI, ignored if empty or
     *            <code>null</code>
     * @return The external (URI) representation of the Content path.
     */
    String pathToURL(String prefix, String path, String suffix);

}