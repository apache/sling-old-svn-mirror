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
package org.apache.sling.ujax;

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

/** Generates a node name based on a set of well-known request parameters
 *  like title, description, etc.
 *  See SLING-128.
 */
public class NodeNameGenerator {
    private List<String> parameterNames;
    private final NodeNameFilter filter = new NodeNameFilter();
    public static final int DEFAULT_MAX_NAME_LENGTH = 20;
    private int maxLength = DEFAULT_MAX_NAME_LENGTH;
    private int counter;

    public NodeNameGenerator() {
        this(null);
    }

    public NodeNameGenerator(List<String> parameterNames) {
        if(parameterNames == null) {
            this.parameterNames = new LinkedList<String>();
            this.parameterNames.add("title");
            this.parameterNames.add("jcr:title");
            this.parameterNames.add("name");
            this.parameterNames.add("description");
            this.parameterNames.add("jcr:description");
            this.parameterNames.add("abstract");
        } else {
            this.parameterNames = parameterNames;
        }

    }

    /**
     * Get a "nice" node name, if possible, based on given request
     *
     * @param parameters the request parameters
     * @param prefix if provided, added in front of our parameterNames
     *        when looking for request parameters
     * @return a nice node name
     */
    public String getNodeName(RequestParameterMap parameters, String prefix) {
        if (prefix==null) {
            prefix = "";
        }

        // find the first request parameter that matches one of
        // our parameterNames, in order, and has a value
        String valueToUse = null;
        if(parameters!=null) {
            for(String param : parameterNames) {
                if(valueToUse != null) {
                    break;
                }
                final RequestParameter[] pp = parameters.get(prefix + param);
                if(pp!=null) {
                    for(RequestParameter p : pp) {
                        valueToUse = p.getString();
                        if(valueToUse != null && valueToUse.length() > 0) {
                            break;
                        }
                        valueToUse = null;
                    }
                }
            }
        }
        String result;
        if(valueToUse != null) {
            // filter value so that it works as a node name
            result = filter.filter(valueToUse);
        } else {
            // default value if none provided
            result = nextCounter() + "_" + System.currentTimeMillis();
        }

        // max length
        if(result.length() > maxLength) {
            result = result.substring(0,maxLength);
        }

        return result;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public synchronized int nextCounter() {
        return ++counter;
    }
}
