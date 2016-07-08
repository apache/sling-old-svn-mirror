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
package org.apache.sling.engine;

import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

/**
 * @deprecated Use {@link org.apache.sling.api.request.RequestUtil}
 */
@Deprecated
public class RequestUtil {

    /**
     * Parses a header of the form:
     *
     * <pre>
     *            Header = Token { &quot;,&quot; Token } .
     *            Token = name { &quot;;&quot; Parameter } .
     *            Paramter = name [ &quot;=&quot; value ] .
     * </pre>
     *
     * "," and ";" are not allowed within name and value
     *
     * @param value The header value to parse
     * @return A Map indexed by the Token names where the values are Map
     *         instances indexed by parameter name
     */
    public static Map<String, Map<String, String>> parserHeader(final String value) {
        return org.apache.sling.api.request.RequestUtil.parserHeader(value);
    }

    /**
     * Parses an <code>Accept-*</code> header of the form:
     *
     * <pre>
     *            Header = Token { &quot;,&quot; Token } .
     *            Token = name { &quot;;&quot; &quot;q&quot; [ &quot;=&quot; value ] } .
     *            Paramter =  .
     * </pre>
     *
     * "," and ";" are not allowed within name and value
     *
     * @param value The header value to parse
     * @return A Map indexed by the Token names where the values are
     *         <code>Double</code> instances providing the value of the
     *         <code>q</code> parameter.
     */
    public static Map<String, Double> parserAcceptHeader(final String value) {
        return org.apache.sling.api.request.RequestUtil.parserAcceptHeader(value);
    }

    /**
     * Utility method to return a name for the given servlet. This method
     * applies the following algorithm to find a non-<code>null</code>,
     * non-empty name:
     * <ol>
     * <li>If the servlet has a servlet config, the servlet name from the
     * servlet config is taken.
     * <li>Otherwise check the servlet info
     * <li>Otherwise use the fully qualified name of the servlet class
     * </ol>
     *
     * @param servlet The servlet instance
     * @return The name of the servlet
     */
    public static String getServletName(final Servlet servlet) {
        return org.apache.sling.api.request.RequestUtil.getServletName(servlet);
    }

    /**
     * Sets the named request attribute to the new value and returns the
     * previous value.
     *
     * @param request The request object whose attribute is to be set.
     * @param name The name of the attribute to be set.
     * @param value The new value of the attribute. If this is <code>null</code>
     *            the attribute is actually removed from the request.
     * @return The previous value of the named request attribute or
     *         <code>null</code> if it was not set.
     */
    public static Object setRequestAttribute(final HttpServletRequest request,
            final String name, final Object value) {
        return org.apache.sling.api.request.RequestUtil.setRequestAttribute(request, name, value);
    }
}
