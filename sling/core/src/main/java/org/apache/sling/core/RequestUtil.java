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
package org.apache.sling.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

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
}
