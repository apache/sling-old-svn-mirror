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
package org.apache.sling.engine.impl.parameters;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ParameterSupportHttpServletRequestWrapper2x extends HttpServletRequestWrapper {
    public ParameterSupportHttpServletRequestWrapper2x(HttpServletRequest request) {
        super(request);
    }

    protected final ParameterSupport getParameterSupport() {
        return ParameterSupport.getInstance((HttpServletRequest) getRequest());
    }

    @Override
    public String getParameter(String name) {
        return getParameterSupport().getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return getParameterSupport().getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return getParameterSupport().getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return getParameterSupport().getParameterValues(name);
    }
}