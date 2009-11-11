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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

/**
 * The <code>ParameterMap</code> TODO
 */
class ParameterMap extends LinkedHashMap<String, RequestParameter[]> implements
        RequestParameterMap {

    private Map<String, String[]> stringParameterMap;

    ParameterMap() {
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
        super.put(newName, params);
    }

    void addParameter(String name, RequestParameter parameter) {
        Object current = this.get(name);
        if (current == null) {
            super.put(name, new RequestParameter[] { parameter });
        } else {
            RequestParameter[] ppo = (RequestParameter[]) current;
            RequestParameter[] ppn = new RequestParameter[ppo.length + 1];
            System.arraycopy(ppo, 0, ppn, 0, ppo.length);
            ppn[ppo.length] = parameter;

            super.put(name, ppn);
        }
    }

    void setParameters(String name, RequestParameter[] parameters) {
        super.put(name, parameters);
    }

    //---------- String parameter support

    String getStringValue(final String name) {
        final RequestParameter param = getValue(name);
        return (param != null) ? param.getString() : null;
    }

    String[] getStringValues(final String name) {
        return toStringArray(getValues(name));
    }

    Map<String, String[]> getStringParameterMap() {
        if (this.stringParameterMap == null) {
            LinkedHashMap<String, String[]> pm = new LinkedHashMap<String, String[]>();
            for (Map.Entry<String, RequestParameter[]> ppmEntry : entrySet()) {
                pm.put(ppmEntry.getKey(), toStringArray(ppmEntry.getValue()));
            }
            this.stringParameterMap = Collections.unmodifiableMap(pm);
        }
        return stringParameterMap;
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
