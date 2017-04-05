/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.javascript.wrapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.javascript.SlingWrapper;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * The {@code ScriptableMap} wrapper provides easier access to a map's values by setting the map's keys as properties to the JavaScript
 * object representing the {@link Map}.
 */
public class ScriptableMap extends ScriptableBase implements SlingWrapper {

    public static final String CLASSNAME = "Map";
    private static final Class<?> [] WRAPPED_CLASSES = { Map.class };

    private Map<String, Object> map = new HashMap<String, Object>();

    public void jsConstructor(Object map) {
        this.map = (Map) map;
    }

    @Override
    public Object get(String name, Scriptable start) {
        final Object fromSuperClass = super.get(name, start);
        if (fromSuperClass != Scriptable.NOT_FOUND) {
            return fromSuperClass;
        }

        if (map == null) {
            return Undefined.instance;
        }

        Object result = map.get(name);
        if (result == null) {
            result = getNative(name, start);
        }
        return result;
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return map;
    }

    @Override
    protected Object getWrappedObject() {
        return map;
    }

    @Override
    protected Class<?> getStaticType() {
        return Map.class;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    @Override
    public Class<?>[] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }

    @Override
    public Object unwrap() {
        return map;
    }
}
