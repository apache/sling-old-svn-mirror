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
package org.apache.sling.api;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>SlingHttpServletRequest</code> defines the interface to provide
 * client request information to a servlet.
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
 * will throw an <code>IllegalStateException</code> if called after any methods
 * returning request parameters if the request content type is either
 * <i>application/x-www-form-urlencoded</i> or <i>multipart/form-data</i>
 * because the request data has already been processed.
 * <p>
 * Starting with Sling API 2.0.6, this interface als extends the
 * {@link Adaptable} interface.
 */
@ProviderType
public interface SlingHttpServletRequest extends HttpServletRequest, Adaptable {

    /**
     * Returns the {@link Resource} object on whose behalf the servlet acts.
     *
     * @return The <code>Resource</code> object of this request.
     */
    Resource getResource();

    /**
     * Returns the {@link ResourceResolver} which resolved the
     * {@link #getResource() resource} of this request.
     *
     * @return The resource resolver
     */
    ResourceResolver getResourceResolver();

    /**
     * Returns the {@link RequestPathInfo} pertaining to this request.
     *
     * @return the request path info.
     */
    RequestPathInfo getRequestPathInfo();

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
     * <p>
     * This method is a shortcut for
     * <code>getRequestParameterMap().getValue(String)</code>.
     *
     * @param name a <code>String</code> specifying the name of the parameter
     * @return a {@link RequestParameter} representing the single value of the
     *         parameter
     * @see #getRequestParameters(String)
     * @see RequestParameterMap#getValue(String)
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    RequestParameter getRequestParameter(String name);

    /**
     * Returns an array of {@link RequestParameter} objects containing all of
     * the values the given request parameter has, or <code>null</code> if the
     * parameter does not exist.
     * <p>
     * If the parameter has a single value, the array has a length of 1.
     * <p>
     * This method is a shortcut for
     * <code>getRequestParameterMap().getValues(String)</code>.
     *
     * @param name a <code>String</code> containing the name of the parameter
     *            the value of which is requested
     * @return an array of {@link RequestParameter} objects containing the
     *         parameter values.
     * @see #getRequestParameter(String)
     * @see RequestParameterMap#getValues(String)
     * @throws IllegalArgumentException if name is <code>null</code>.
     */
    RequestParameter[] getRequestParameters(String name);

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
    RequestParameterMap getRequestParameterMap();

    /**
     * Returns a <code>RequestDispatcher</code> object that acts as a wrapper
     * for the resource located at the given path. A
     * <code>RequestDispatcher</code> object can be used to include the
     * resource in a response.
     * <p>
     * Returns <code>null</code> if a <code>RequestDispatcher</code> cannot
     * be returned for any reason.
     *
     * @param path a <code>String</code> specifying the pathname to the
     *            resource. If it is relative, it must be relative against the
     *            current servlet.
     * @param options influence the rendering of the included Resource
     * @return a <code>RequestDispatcher</code> object that acts as a wrapper
     *         for the <code>resource</code> or <code>null</code> if an
     *         error occurs preparing the dispatcher.
     */
    RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options);

    /**
     * Returns a <code>RequestDispatcher</code> object that acts as a wrapper
     * for the resource located at the given resource. A
     * <code>RequestDispatcher</code> object can be used to include the
     * resource in a response.
     * <p>
     * Returns <code>null</code> if a <code>RequestDispatcher</code> cannot
     * be returned for any reason.
     *
     * @param resource The {@link Resource} instance whose response content may
     *            be included by the returned dispatcher.
     * @param options influence the rendering of the included Resource
     * @return a <code>RequestDispatcher</code> object that acts as a wrapper
     *         for the <code>resource</code> or <code>null</code> if an
     *         error occurs preparing the dispatcher.
     */
    RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options);

    /**
     * Same as {@link #getRequestDispatcher(Resource,RequestDispatcherOptions)}
     * but using empty options.
     */
    RequestDispatcher getRequestDispatcher(Resource resource);

    /**
     * Returns the named cookie from the HTTP request or <code>null</code> if
     * no such cookie exists in the request.
     *
     * @param name The name of the cookie to return.
     * @return The named cookie or <code>null</code> if no such cookie exists.
     */
    Cookie getCookie(String name);

    /**
     * Returns the framework preferred content type for the response. The
     * content type only includes the MIME type, not the character set.
     * <p>
     * For included resources this method will returned the same string as
     * returned by the <code>ServletResponse.getContentType()</code> without
     * the character set.
     *
     * @return preferred MIME type of the response
     */
    String getResponseContentType();

    /**
     * Gets a list of content types which the framework accepts for the
     * response. This list is ordered with the most preferable types listed
     * first. The content type only includes the MIME type, not the character
     * set.
     * <p>
     * For included resources this method will returned an enumeration
     * containing a single entry which is the same string as returned by the
     * <code>ServletResponse.getContentType()</code> without the character
     * set.
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

    /**
     * Returns the resource bundle of the given base name for the given locale.
     *
     * @param baseName The base name of the resource bundle to returned. If this
     *            parameter is <code>null</code>, the same resource bundle
     *            must be returned as if the {@link #getResourceBundle(Locale)}
     *            method is called.
     * @param locale the locale for which to retrieve the resource bundle. If
     *            this is <code>null</code>, the locale returned by
     *            {@link #getLocale()} is used to select the resource bundle.
     * @return the resource bundle for the given locale
     */
    ResourceBundle getResourceBundle(String baseName, Locale locale);

    /**
     * Returns the {@link RequestProgressTracker} of this request.
     */
    RequestProgressTracker getRequestProgressTracker();
}
