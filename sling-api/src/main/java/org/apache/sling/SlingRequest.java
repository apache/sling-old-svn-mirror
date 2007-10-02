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
package org.apache.sling;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * The <code>SlingRequest</code> defines the interface to provide client
 * request information to a servlet.
 * <p id="decomp">
 * <b>Decomposition of a Request URL</b>
 * <p>
 * The Sling Framework is responsible to decompose the
 * {@link #getRequestURI() request URI} into four parts:
 * <ol>
 * <li>{@link Content#getPath() content path} - The longest substring of the
 * request URI resolving to a {@link Content} object such that the content path
 * is either the complete request URI the next character in the request URI
 * after the content path is either a dot (<code>.</code>) or a slash (<code>/</code>).
 * <li>{@link #getSelectors() selectors} - If the first character in the
 * request URI after the content path is a dot, the string after the dot upto
 * but not including the last dot before the next slash character or the end of
 * the request URI. If the content path spans the complete request URI or if a
 * slash follows the content path in the request, the no seletors exist. If only
 * one dot follows the content path before the end of the request URI or the
 * next slash, no selectors exist. The selectors are available as
 * {@link #getSelectorString() a single string}, an
 * {@link #getSelectors() array of strings}, which is the selector string
 * splitted on dots, or
 * {@link #getSelector(int) selectively by zero-based index}.
 * <li>{@link #getExtension() extension} - The string after the last dot after
 * the content path in the request uri but before the end of the request uri or
 * the next slash after the content path in the request uri. If a slash follows
 * the content path in the request URI, the extension is empty.
 * <li>{@link #getSuffix() suffix path} - If the request URI contains more a
 * slash character after the content path and optional selectors and extension,
 * the path starting with the slash upto the end of the request URI is the
 * suffix path.
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
 * <td>""</td>
 * <td>""</td>
 * <td>""</td>
 * </tr>
 * <tr>
 * <td>/a/b.html</td>
 * <td>/a/b</td>
 * <td>""</td>
 * <td>html</td>
 * <td>""</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.html</td>
 * <td>/a/b</td>
 * <td>s1</td>
 * <td>html</td>
 * <td>""</td>
 * </tr>
 * <tr>
 * <td>/a/b.s1.s2.html</td>
 * <td>/a/b</td>
 * <td>s1.s2</td>
 * <td>html</td>
 * <td>""</td>
 * </tr>
 * <tr>
 * <td>/a/b/c/d</td>
 * <td>/a/b</td>
 * <td>""</td>
 * <td>""</td>
 * <td>/c/d</td>
 * </tr>
 * <tr>
 * <td>/a/b.html/c/d</td>
 * <td>/a/b</td>
 * <td>""</td>
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
 * <td>""</td>
 * <td>""</td>
 * <td>/c/d.s.txt</td>
 * </tr>
 * <tr>
 * <td>/a/b.html/c/d.s.txt</td>
 * <td>/a/b</td>
 * <td>""</td>
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
 * <p>
 * <b>Request Parameters</b> Generally request parameters are transmitted as
 * part of the URL string such as <code>GET /some/path?<b>param=value</b></code>
 * or as request data of content type <i>application/x-www-form-urlencoded</i>
 * or <i>multipart/form-data</i>. The Sling Framework must decode parameters
 * transferred as request data and make them available through the various
 * parameter accessor methods. Generally parameters transferred as
 * <i>multipart/form-data</i> will be accessed by one of the methods returning
 * {@link RequestParameter} instances.
 * <p>
 * In any case, the {@link #getReader()} and {@link #getInputStream()} methods
 * will throw an <code>IllegalStateException</code> if called when the request
 * content type is either <i>application/x-www-form-urlencoded</i> or
 * <i>multipart/form-data</i> because the request data has already been
 * processed.
 */
public interface SlingRequest extends HttpServletRequest {

    // ---------- Content Access Methods ---------------------------------------

    /**
     * Returns the {@link Content} object on whose behalf the servlet acts.
     *
     * @return The <code>Content</code> object of this request.
     */
    Content getContent();

    /**
     * Returns a content object for data located at the given path.
     * <p>
     * This specification does not define the location for content or the
     * semantics for content paths. For an implementation reading content from a
     * Java Content Repository, the path could be a <code>javax.jcr.Item</code>
     * path from which the content object is loaded.
     *
     * @param path The path to the content object to be loaded. If the path is
     *            relative, i.e. does not start with a slash (<code>/</code>),
     *            the content relative to this request's content is returned.
     * @return The <code>Content</code> object loaded from the path or
     *         <code>null</code> if no content object may be loaded from the
     *         path.
     * @throws SlingException If an error occurrs trying to load the content
     *             object from the path.
     */
    Content getContent(String path) throws SlingException;

    /**
     * Returns an <code>Iterator</code> of {@link Content} objects loaded from
     * the children of the given <code>content</code>.
     * <p>
     * This specification does not define what the term "child" means. This is
     * left to the implementation to define. For example an implementation
     * reading content from a Java Content Repository, the children could be the
     * {@link Content} objects loaded from child items of the
     * <code>javax.jcr.Item</code> of the given <code>content</code>.
     *
     * @param content The {@link Content content object} whose children are
     *            requested. If <code>null</code> the children of this
     *            request's content (see {@link #getContent()}) are returned.
     * @return
     * @throws SlingException
     */
    Enumeration<Content> getChildren(Content content) throws SlingException;

    // ---------- Request URL Information --------------------------------------

    /**
     * Returns the extension from the URL or an empty string if the request URL
     * does not contain an extension.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The extension from the request URL.
     */
    String getExtension();

    /**
     * Returns the i-th selector of the selector string split on dots or
     * <code>null</code> if i&lt;0 or i&gt;getSelectors().length. Alyways
     * returns <code>null</code> if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
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
     * Returns the selectors decoded from the request URL as an array of
     * strings. This array is derived from the
     * {@link #getSelectorString() selector string} by splitting the string on
     * dots. Returns an empty array if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectorString()
     * @see #getSelector(int)
     */
    String[] getSelectors();

    /**
     * Returns the selectors decoded from the request URL as string. Returns an
     * empty string if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectors()
     * @see #getSelector(int)
     */
    String getSelectorString();

    /**
     * Returns the suffix part of the URL or an empty string if the request URL
     * does not contain a suffix.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The suffix part of the request URL.
     */
    String getSuffix();

    // ---------- File oriented POST parameters --------------------------------

    /**
     * Returns the value of a request parameter as a {@link RequestParameter},
     * or <code>null</code> if the parameter does not exist.
     * <p>
     * This method should only be used if the parameter has only one value. If
     * the parameter might have more than one value, use
     * {@link #getRequestParameters(String)}.
     * <p>
     * If this method is used with a multivalued parameter, the value returned
     * is equal to the first value in the array returned by
     * <code>getRequestParameters</code>.
     *
     * @param name a <code>String</code> specifying the name of the parameter
     * @return a {@link RequestParameter} representing the single value of the
     *         parameter
     * @see #getRequestParameters(String)
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    public RequestParameter getRequestParameter(String name);

    /**
     * Returns a <code>Map</code> of the parameters of this request.
     * <p>
     * The values in the returned <code>Map</code> are from type
     * {@link RequestParameter} array (<code>RequestParameter[]</code>).
     * <p>
     * If no parameters exist this method returns an empty <code>Map</code>.
     *
     * @return an immutable <code>Map</code> containing parameter names as
     *         keys and parameter values as map values, or an empty
     *         <code>Map</code> if no parameters exist. The keys in the
     *         parameter map are of type String. The values in the parameter map
     *         are of type {@link RequestParameter} array (<code>RequestParameter[]</code>).
     */
    Map<String, RequestParameter> getRequestParameterMap();

    /**
     * Returns an array of {@link RequestParameter} objects containing all of
     * the values the given request parameter has, or <code>null</code> if the
     * parameter does not exist.
     * <p>
     * If the parameter has a single value, the array has a length of 1.
     *
     * @param name a <code>String</code> containing the name of the parameter
     *            the value of which is requested
     * @return an array of {@link RequestParameter} objects containing the
     *         parameter values.
     * @see #getRequestParameter(String)
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    public RequestParameter[] getRequestParameters(String name);

    // ---------- Request Dispatching ------------------------------------------

    /**
     * Returns a <code>RequestDispatcher</code> object that acts as a wrapper
     * for the content located at the given path. A
     * <code>RequestDispatcher</code> object can be used to include the
     * resource in a response.
     * <p>
     * This method returns <code>null</code> if the
     * <code>ServletContext</code> cannot return a
     * <code>RequestDispatcher</code> for any reason.
     *
     * @param content The {@link Content} instance whose response content may be
     *            included by the returned dispatcher.
     * @return a <code>RequestDispatcher</code> object that acts as a wrapper
     *         for the <code>content</code> or <code>null</code> if an error
     *         occurrs preparing the dispatcher.
     */
    RequestDispatcher getRequestDispatcher(Content content);

    // --------- Miscellaneous -------------------------------------------------

    /**
     * Returns the named cookie from the HTTP request or <code>null</code> if
     * no such cookie exists in the request.
     *
     * @param name The name of the cookie to return.
     * @return The named cookie or <code>null</code> if no such cookie exists.
     */
    Cookie getCookie(String name);

    /**
     * Returns the framework preferred content type for the response.
     * <p>
     * The content type only includes the MIME type, not the character set.
     *
     * @return preferred MIME type of the response
     */
    String getResponseContentType();

    /**
     * Gets a list of content types which the framework accepts for the
     * response. This list is ordered with the most preferable types listed
     * first.
     * <p>
     * The content type only includes the MIME type, not the character set.
     *
     * @return ordered list of MIME types for the response
     */
    Enumeration<String> getResponseContentTypes();

    /**
     * Returns the resource bundle for the given locale.
     *
     * @param locale the locale for which to retrieve the resource bundle. If
     *            this is <code>null</code>, the locale returned by
     *            {@link #getLocale()} is used to select the resource bundle.
     * @return the resource bundle for the given locale
     */
    ResourceBundle getResourceBundle(Locale locale);
}
