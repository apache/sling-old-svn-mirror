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
package org.apache.sling.servlets.post.impl.helper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Generates a node name based on a set of well-known request parameters
 * like title, description, etc.
 * See SLING-128.
 */
public class DefaultNodeNameGenerator implements NodeNameGenerator {

    private final String[] parameterNames;
    private final NodeNameFilter filter = new NodeNameFilter();

    public static final int DEFAULT_MAX_NAME_LENGTH = 20;

    private int maxLength = DEFAULT_MAX_NAME_LENGTH;
    private int counter;

    public DefaultNodeNameGenerator() {
        this(null, -1);
    }

    public DefaultNodeNameGenerator(String[] parameterNames, int maxNameLength) {
        if (parameterNames == null) {
            this.parameterNames = new String[0];
        } else {
            this.parameterNames = parameterNames;
        }

        this.maxLength = (maxNameLength > 0)
                ? maxNameLength
                : DEFAULT_MAX_NAME_LENGTH;
    }

    /**
     * Get a "nice" node name, if possible, based on given request
     *
     * @param request the request
     * @param basePath the base path
     * @param requirePrefix <code>true</code> if the parameter names for
     *      properties requires a prefix
     * @param defaultNodeNameGenerator a default generator
     * @return a nice node name
     */
    public String getNodeName(SlingHttpServletRequest request, String basePath,
            boolean requirePrefix, NodeNameGenerator defaultNodeNameGenerator) {
        RequestParameterMap parameters = request.getRequestParameterMap();
        String valueToUse = null;
        boolean doFilter = true;

        // find the first request parameter that matches one of
        // our parameterNames, in order, and has a value
        if (parameters!=null) {
            // we first check for the special sling parameters
            RequestParameter specialParam = parameters.getValue(SlingPostConstants.RP_NODE_NAME);
            if ( specialParam != null ) {
                if ( specialParam.getString() != null && specialParam.getString().length() > 0 ) {
                    valueToUse = specialParam.getString();
                    doFilter = false;
                }
            }
            if ( valueToUse == null ) {
                specialParam = parameters.getValue(SlingPostConstants.RP_NODE_NAME_HINT);
                if ( specialParam != null ) {
                    if ( specialParam.getString() != null && specialParam.getString().length() > 0 ) {
                        valueToUse = specialParam.getString();
                    }
                }
            }

            if (valueToUse == null) {
                for (String param : parameterNames) {
                    if (valueToUse != null) {
                        break;
                    }
                    if (requirePrefix) {
                        param = SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT.concat(param);
                    }
                    final RequestParameter[] pp = parameters.get(param);
                    if (pp != null) {
                        for (RequestParameter p : pp) {
                            valueToUse = p.getString();
                            if (valueToUse != null && valueToUse.length() > 0) {
                                break;
                            }
                            valueToUse = null;
                        }
                    }
                }
            }
        }
        String result;
        // should we filter?
        if (valueToUse != null) {
            if ( doFilter ) {
                // filter value so that it works as a node name
                result = filter.filter(valueToUse);
            } else {
                result = valueToUse;
            }
        } else {
            // default value if none provided
            result = nextCounter() + "_" + System.currentTimeMillis();
        }

        if ( doFilter ) {
            // max length
            if (result.length() > maxLength) {
                result = result.substring(0,maxLength);
            }
        }

        return result;
    }

    public synchronized int nextCounter() {
        return ++counter;
    }
}
