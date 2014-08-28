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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Request related utility methods.
 * <p>
 * This class is not intended to be extended or instantiated because it just
 * provides static utility methods not intended to be overwritten.
 *
 * @since 2.1
 */
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
     * @param value
     * @return A Map indexed by the Token names where the values are Map
     *         instances indexed by parameter name
     */
    public static Map<String, Map<String, String>> parserHeader(String value) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        String[] tokens = value.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String[] parameters = tokens[i].split(";");
            String name = parameters[0].trim();
            Map<String, String> parMap;
            if (parameters.length > 0) {
                parMap = new HashMap<String, String>();
                for (int j = 1; j < parameters.length; j++) {
                    String[] content = parameters[j].split("=", 2);
                    if (content.length > 1) {
                        parMap.put(content[0].trim(), content[1].trim());
                    } else {
                        parMap.put(content[0].trim(), content[0].trim());
                    }
                }
            } else {
                parMap = Collections.emptyMap();
            }
            result.put(name, parMap);
        }
        return result;
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
     * @param value
     * @return A Map indexed by the Token names where the values are
     *         <code>Double</code> instances providing the value of the
     *         <code>q</code> parameter.
     */
    public static Map<String, Double> parserAcceptHeader(String value) {
        Map<String, Double> result = new HashMap<String, Double>();
        String[] tokens = value.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String[] parameters = tokens[i].split(";");
            String name = parameters[0];
            Double qVal = new Double(1.0);
            if (parameters.length > 1) {
                for (int j = 1; j < parameters.length; j++) {
                    String[] content = parameters[j].split("=", 2);
                    if (content.length > 1 && "q".equals(content[0])) {
                        try {
                            qVal = Double.valueOf(content[1]);
                        } catch (NumberFormatException nfe) {
                            // don't care
                        }
                    }
                }
            }
            if (qVal != null) {
                result.put(name, qVal);
            }
        }
        return result;
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
     */
    public static String getServletName(Servlet servlet) {
        String name = null;

        if (servlet.getServletConfig() != null) {
            name = servlet.getServletConfig().getServletName();
        }
        if (name == null || name.length() == 0) {
            name = servlet.getServletInfo();
        }
        if (name == null || name.length() == 0) {
            name = servlet.getClass().getName();
        }

        return name;
    }

    /**
     * Sets the named request attribute to the new value and returns the
     * previous value.
     *
     * @param request The request object whose attribute is to be set.
     * @param name The name of the attribute to be set.
     * @param value The new value of the attribute. If this is <code>null</code>
     *            the attribte is actually removed from the request.
     * @return The previous value of the named request attribute or
     *         <code>null</code> if it was not set.
     */
    public static Object setRequestAttribute(HttpServletRequest request,
            String name, Object value) {
        Object oldValue = request.getAttribute(name);
        if (value == null) {
            request.removeAttribute(name);
        } else {
            request.setAttribute(name, value);
        }
        return oldValue;
    }

    /**
     * Checks if the request contains a if-last-modified-since header and if the the
	 * request's underlying resource has a jcr:lastModified property. if the properties were modified
     * before the header a 304 is sent otherwise the response last modified header is set.
     * @param req the request
     * @param resp the response
     * @return <code>true</code> if the response was set
     */
    public static boolean handleIfModifiedSince(SlingHttpServletRequest req, HttpServletResponse resp){
        boolean responseSet=false;
        long lastModified=req.getResource().getResourceMetadata().getModificationTime();
        if (lastModified!=-1){
            long modifiedTime = lastModified/1000; //seconds
            long ims = req.getDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE)/1000; //seconds
            if (modifiedTime <= ims) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                responseSet=true;
            }
            resp.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified);
        }
        return responseSet;
    }

}
