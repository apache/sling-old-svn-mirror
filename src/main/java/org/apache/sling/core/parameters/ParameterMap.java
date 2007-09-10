/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.parameters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.component.RequestParameter;

/**
 * The <code>ParameterMap</code> TODO
 */
class ParameterMap implements Map {

    private Map delegatee;
    private Map stringParameterMap;

    ParameterMap() {
        this.delegatee = new HashMap();
    }

    void renameParameter(Object oldName, Object newName) {
        Object params = delegatee.remove(oldName);
        delegatee.put(newName, params);
    }
    
    void addParameter(Object name, RequestParameter parameter) {
        Object current = get(name);
        if (current == null) {
            delegatee.put(name, new RequestParameter[] { parameter });
        } else {
            RequestParameter[] ppo = (RequestParameter[]) current;
            RequestParameter[] ppn = new RequestParameter[ppo.length+1];
            System.arraycopy(ppo, 0, ppn, 0, ppo.length);
            ppn[ppo.length] = parameter;

            delegatee.put(name, ppn);
        }
    }

    void setParameters(Object name, Object parameters) {
        delegatee.put(name, parameters);
    }

    Map getStringParameterMap() {
        if (stringParameterMap == null) {
            Map pm = new HashMap();
            for (Iterator pi = entrySet().iterator(); pi.hasNext();) {
                Map.Entry ppmEntry = (Map.Entry) pi.next();
                RequestParameter[] pps = (RequestParameter[]) ppmEntry.getValue();
                String[] ps = new String[pps.length];
                for (int i = 0; i < pps.length; i++) {
                    ps[i] = pps[i].getString();
                }
                pm.put(ppmEntry.getKey(), ps);
            }
            stringParameterMap = Collections.unmodifiableMap(pm);
        }
        return stringParameterMap;
    }
    
    // ---------- Stanard Map interface ----------------------------------------

    /**
     * @param key
     * @return
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return delegatee.containsKey(key);
    }

    /**
     * @param value
     * @return
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return delegatee.containsValue(value);
    }

    /**
     * @return
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        return delegatee.entrySet();
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        return delegatee.get(key);
    }

    /**
     * @return
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return delegatee.isEmpty();
    }

    /**
     * @return
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return delegatee.keySet();
    }

    /**
     * @return
     * @see java.util.Map#size()
     */
    public int size() {
        return delegatee.size();
    }

    /**
     * @return
     * @see java.util.Map#values()
     */
    public Collection values() {
        return delegatee.values();
    }

    // ---------- Prohibited Write Access --------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException("put");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map t) {
        throw new UnsupportedOperationException("putAll");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException("remove");
    }
}
