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
package org.apache.sling.api.request;

import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ProviderType;

/**
 * Sling breaks the request URI into four parts: the path itself, optional
 * dot-separated selectors and extension that follow it, and an optional path
 * suffix.
 * <p id="decomp">
 * <b>Decomposition of a Request URL</b>
 * <ol>
 * <li>{@link #getResourcePath() content path} - The longest substring of the request
 * URI resolving to a {@link org.apache.sling.api.resource.Resource} object such
 * that the content path is either the complete request URI or the next
 * character in the request URI after the content path is either a dot (<code>.</code>)
 * or a slash (<code>/</code>).
 * <li>{@link #getSelectors() selectors} - If the first character in the
 * request URI after the content path is a dot, the string after the dot upto
 * but not including the last dot before the next slash character or the end of
 * the request URI. If the content path spans the complete request URI or if a
 * slash follows the content path in the request, then no seletors exist. If
 * only one dot follows the content path before the end of the request URI or
 * the next slash, no selectors exist. The selectors are available as
 * {@link #getSelectorString() a single string} and as an
 * {@link #getSelectors() array of strings}, which is the selector string
 * splitted on dots.
 * <li>{@link #getExtension() extension} - The string after the last dot after
 * the content path in the request uri but before the end of the request uri or
 * the next slash after the content path in the request uri. If the content path
 * spans the complete request URI or a slash follows the content path in the
 * request URI, the extension is empty.
 * <li>{@link #getSuffix() suffix path} - If the request URI contains a slash
 * character after the content path and optional selectors and extension, the
 * path starting with the slash upto the end of the request URI is the suffix
 * path.
 * </ol>
 * <p>
 * Examples: <table>
 * <tr>
 * <th>URI</th>
 * <th>Content Path</th>
 * <th>Selectors</th>
 * <th>Extension</th>
 * <th>Suffix</th>
 * </tr>
 * <tr>
 * <td>/a/b</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>null</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>/a/b.html</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>html</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.html</td>
 * <td>/a/b</td>
 * <td>s1</td>
 * <td>html</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.s2.html</td>
 * <td>/a/b</td>
 * <td>s1.s2</td>
 * <td>html</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>/a/b/c/d</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>null</td>
 * <td>/c/d</td>
 * </tr>
 * <tr>
 * <td>/a/b.html/c/d</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>html</td>
 * <td>/c/d</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.html/c/d</td>
 * <td>/a/b</td>
 * <td>s1</td>
 * <td>html</td>
 * <td>/c/d</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.s2.html/c/d</td>
 * <td>/a/b</td>
 * <td>s1.s2</td>
 * <td>html</td>
 * <td>/c/d</td>
 * </tr>
 * <tr>
 * <td>/a/b/c/d.s.txt</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>null</td>
 * <td>/c/d.s.txt</td>
 * </tr>
 * <tr>
 * <td>/a/b.html/c/d.s.txt</td>
 * <td>/a/b</td>
 * <td>null</td>
 * <td>html</td>
 * <td>/c/d.s.txt</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.html/c/d.s.txt</td>
 * <td>/a/b</td>
 * <td>s1</td>
 * <td>html</td>
 * <td>/c/d.s.txt</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.s2.html/c/d.s.txt</td>
 * <td>/a/b</td>
 * <td>s1.s2</td>
 * <td>html</td>
 * <td>/c/d.s.txt</td>
 * </tr>
 * </table>
 */
@ProviderType
public interface RequestPathInfo {

    /**
     * Return the "resource path" part of the URL, what comes before selectors,
     * extension and suffix. This string is part of the request URL and need not
     * be equal to the {@link org.apache.sling.api.resource.Resource#getPath()}.
     * Rather it is equal to the
     * {@link org.apache.sling.api.resource.ResourceMetadata#RESOLUTION_PATH resolution path metadata property}
     * of the resource.
     */
    String getResourcePath();

    /**
     * Returns the extension from the URL or <code>null</code> if the request
     * URL does not contain an extension.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The extension from the request URL.
     */
    String getExtension();

    /**
     * Returns the selectors decoded from the request URL as string. Returns
     * <code>null</code> if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectors()
     */
    String getSelectorString();

    /**
     * Returns the selectors decoded from the request URL as an array of
     * strings. This array is derived from the
     * {@link #getSelectorString() selector string} by splitting the string on
     * dots. Returns an empty array if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectorString()
     */
    String[] getSelectors();

    /**
     * Returns the suffix part of the URL or <code>null</code> if the request
     * URL does not contain a suffix.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The suffix part of the request URL.
     */
    String getSuffix();

    /**
     * Returns the resource addressed by the suffix or null if the request does
     * not have a suffix or the suffix does not address an accessible resource.
     * <p>
     * The suffix is expected to be the absolute path to the resource suitable
     * as an argument to the
     * {@link org.apache.sling.api.resource.ResourceResolver#getResource(String)}
     * method.
     *
     * @since 2.3 (Sling API 2.3.2)
     */
    Resource getSuffixResource();
}
