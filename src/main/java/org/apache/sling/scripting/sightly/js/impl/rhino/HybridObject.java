/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl.rhino;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.scripting.sightly.Record;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;


/**
 * Instances of this class can be used in both Sightly & JS scripts
 */
public class HybridObject implements Scriptable, Record<Object> {

    private final Scriptable scriptable;
    private final JsValueAdapter jsValueAdapter;

    public HybridObject(Scriptable scriptable, JsValueAdapter jsValueAdapter) {
        this.scriptable = scriptable;
        this.jsValueAdapter = jsValueAdapter;
    }

    // Record implementation

    @Override
    public Object getProperty(String name) {
        if (name == null) {
            return null;
        }
        Context.enter();
        try {
            return getAdapted(name);
        } finally {
            Context.exit();
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        Object[] properties = scriptable.getIds();
        Set<String> keys = new HashSet<String>();
        for (Object property: properties) {
            if (property instanceof String) {
                keys.add((String) property);
            }
        }
        return keys;
    }

    private Object getAdapted(String key) {
        Object obj = ScriptableObject.getProperty(scriptable, key);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Function) {
            return jsValueAdapter.adapt(JsUtils.callFn((Function) obj, null, scriptable, scriptable, new Object[0]));
        }
        return jsValueAdapter.adapt(obj);
    }

    // Scriptable implementation

    @Override
    public String getClassName() {
        return scriptable.getClassName();
    }

    @Override
    public Object get(String name, Scriptable start) {
        return scriptable.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        return scriptable.get(index, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return scriptable.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return scriptable.has(index, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        scriptable.put(name, start, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        scriptable.put(index, start, value);
    }

    @Override
    public void delete(String name) {
        scriptable.delete(name);
    }

    @Override
    public void delete(int index) {
        scriptable.delete(index);
    }

    @Override
    public Scriptable getPrototype() {
        return scriptable.getPrototype();
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        scriptable.setPrototype(prototype);
    }

    @Override
    public Scriptable getParentScope() {
        return scriptable.getParentScope();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        scriptable.setParentScope(parent);
    }

    @Override
    public Object[] getIds() {
        return scriptable.getIds();
    }

    @Override
    public Object getDefaultValue(Class hint) {
        return scriptable.getDefaultValue(hint);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return scriptable.hasInstance(instance);
    }
}
