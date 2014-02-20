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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.slf4j.LoggerFactory;

/**
 * The <code>ParameterMap</code> TODO
 */
public class ParameterMap extends LinkedHashMap<String, RequestParameter[]> implements
        RequestParameterMap {

    private static final long serialVersionUID = -3984737679401682171L;

    static final int DEFAULT_MAX_PARAMS = 10000;

    private static int maxParameters = DEFAULT_MAX_PARAMS;

    private Map<String, String[]> stringParameterMap;

    private List<RequestParameter> requestParameters = new ArrayList<RequestParameter>();

    static void setMaxParameters(final int maxParameters) {
    ParameterMap.maxParameters = (maxParameters > 0) ? maxParameters : -1;
    }

    public RequestParameter getValue(String name) {
        RequestParameter[] params = getValues(name);
        return (params != null && params.length > 0) ? params[0] : null;
    }

    public RequestParameter[] getValues(String name) {
        return super.get(name);
    }

    void renameParameter(String oldName, String newName) {
        RequestParameter[] params = super.remove(oldName);

        for (RequestParameter param : params) {
            ((AbstractRequestParameter) param).setName(newName);
        }

        super.put(newName, params);
    }

    void addParameter(RequestParameter parameter, boolean prependNew) {

        // check number of parameters
        if (this.requestParameters.size() == maxParameters) {
            // TODO: how to handle this situation ?? just ignore or throw or what ??
            LoggerFactory.getLogger(Util.class).warn("Too many name/value pairs, stopped processing after " + maxParameters + " entries");
            return;
        }

        // map of parameters
        final String name = parameter.getName();
        RequestParameter[] current = this.get(name);
        if (current == null) {
            super.put(name, new RequestParameter[] { parameter });
        } else {
            RequestParameter[] ppn = new RequestParameter[current.length + 1];
            System.arraycopy(current, 0, ppn, prependNew ? 1 : 0, current.length);
            ppn[prependNew ? 0 : current.length] = parameter;

            super.put(name, ppn);
        }

        // list of parameters
        this.requestParameters.add(parameter);
    }

    void setParameters(String name, RequestParameter[] parameters) {
        super.put(name, parameters);
    }

    //---------- String parameter support

    public String getStringValue(final String name) {
        final RequestParameter param = getValue(name);
        return (param != null) ? param.getString() : null;
    }

    public String[] getStringValues(final String name) {
        return toStringArray(getValues(name));
    }

    public Map<String, String[]> getStringParameterMap() {
        if (this.stringParameterMap == null) {
            LinkedHashMap<String, String[]> pm = new LinkedHashMap<String, String[]>();
            for (Map.Entry<String, RequestParameter[]> ppmEntry : entrySet()) {
                pm.put(ppmEntry.getKey(), toStringArray(ppmEntry.getValue()));
            }
            this.stringParameterMap = Collections.unmodifiableMap(pm);
        }
        return stringParameterMap;
    }

    // ---------- Servlet API 3.0 Part

    public Object getPart(final String name) {
        final RequestParameter p = this.getValue(name);
        if (p instanceof MultipartRequestParameter) {
            return new SlingPart((MultipartRequestParameter) p);
        }

        // no such part
        return null;
    }

    public Collection<?> getParts() {
        final ArrayList<Part> parts = new ArrayList<Part>(this.size());
        for (RequestParameter[] param : this.values()) {
            if (param.length >= 1 && param[0] instanceof MultipartRequestParameter) {
                parts.add(new SlingPart((MultipartRequestParameter) param[0]));
            }
        }
        return parts;
    }

    // ---------- Order Request Parameter access

    public List<RequestParameter> getRequestParameterList() {
        return Collections.unmodifiableList(requestParameters);
    }

    // ---------- Prohibited Write Access --------------------------------------

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public RequestParameter[] put(String key, RequestParameter[] value) {
        throw new UnsupportedOperationException("put");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends RequestParameter[]> t) {
        throw new UnsupportedOperationException("putAll");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public RequestParameter[] remove(Object key) {
        throw new UnsupportedOperationException("remove");
    }

    //---------- internal

    private static String[] toStringArray(final RequestParameter[] params) {
        if (params == null) {
            return null;
        }

        final String[] ps = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            ps[i] = params[i].getString();
        }
        return ps;
    }
}
