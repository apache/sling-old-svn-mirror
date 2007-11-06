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

import org.apache.sling.component.Content;

/**
 * The <code>ResolvedURL</code> class represents the URL after resolution to
 * the Content. It also contains the selectors and extension extracted from the
 * request URL. Instances of this interface are returned by the
 * {@link ContentResolver#resolveURL(org.apache.sling.content.ContentManager, String)}
 * method.
 * <p>
 * The original URL of this instance is decomposed as follows:
 * <pre>
 * &lt;contentpath&gt;[ [ . &lt;selectors&gt; ] . &lt;extension&gt; ] ] [ /&lt;suffix&gt; ]
 * </pre>
 * See the JavaDoc of the <code>ComponentRequest</code> interface for more
 * information on the decomposition of the request URL.
 * <p>
 * Clients do not generally need to implement this interface.
 */
public interface ResolvedURL {

    /**
     * The original URL fed into the <code>resolveURL</code> method.
     */
    String getOriginalURL();

    /**
     * The <code>Content</code> object resolved from the
     * {@link #getOriginalURL() original URL}
     */
    Content getContent();

    /**
     * Returns the selectors decoded from the request URL as string. Returns an
     * empty string if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the JavaDoc to the
     * <code>ComponentRequest</code> class.
     *
     * @see #getSelectors()
     * @see #getSelector(int)
     */
    String getSelectorString();

    /**
     * Returns the selectors decoded from the request URL as an array of
     * strings. This array is derived from the
     * {@link #getSelectorString() selector string} by splitting the string on
     * dots. Returns an empty array if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the JavaDoc to the
     * <code>ComponentRequest</code> class.
     *
     * @see #getSelectorString()
     * @see #getSelector(int)
     */
    String[] getSelectors();

    /**
     * Returns the i-th selector of the selector string split on dots or
     * <code>null</code> if i&lt;0 or i&gt;getSelectors().length. Alyways
     * returns <code>null</code> if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the JavaDoc to the
     * <code>ComponentRequest</code> class.
     *
     * @param i The index of the selector to return.
     * @return The value of the selector if 0 &lt;= i &lt;
     *         <code>getSelectors().length</code> or <code>null</code>
     *         otherwise.
     * @see #getSelectorString()
     * @see #getSelectors()
     */
    String getSelector(int i);

    /**
     * Returns the extension from the URL or an empty string if the request URL
     * does not contain an extension.
     * <p>
     * Decomposition of the request URL is defined in the JavaDoc to the
     * <code>ComponentRequest</code> class.
     *
     * @return The extension from the request URL.
     */
    String getExtension();

    /**
     * Returns the suffix part of the URL or an empty string if the request URL
     * does not contain a suffix.
     * <p>
     * Decomposition of the request URL is defined in the JavaDoc to the
     * <code>ComponentRequest</code> class.
     *
     * @return The suffix part of the request URL.
     */
    String getSuffix();
}