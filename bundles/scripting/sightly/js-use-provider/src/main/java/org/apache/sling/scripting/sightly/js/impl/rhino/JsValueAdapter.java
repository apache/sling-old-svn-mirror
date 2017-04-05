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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncContainer;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncExtractor;

/**
 * Converts JS objects to Java objects
 */
public class JsValueAdapter {

    private static final Map<String, Class<?>> knownConversions = new HashMap<String, Class<?>>();

    static {
        knownConversions.put("String", String.class);
        knownConversions.put("Date", Date.class);
    }

    private final AsyncExtractor asyncExtractor;

    public JsValueAdapter(AsyncExtractor asyncExtractor) {
        this.asyncExtractor = asyncExtractor;
    }

    /**
     * Convert a given JS value to a Java object
     * @param jsValue the original JS value
     * @return the Java correspondent
     */
    @SuppressWarnings("unchecked")
    public Object adapt(Object jsValue) {
        if (jsValue == null || jsValue == Context.getUndefinedValue() || jsValue == ScriptableObject.NOT_FOUND) {
            return null;
        }
        if (jsValue instanceof Wrapper) {
            return adapt(((Wrapper) jsValue).unwrap());
        }
        if (asyncExtractor.isPromise(jsValue)) {
            return adapt(forceAsync(jsValue));
        }
        if (jsValue instanceof ScriptableObject) {
            return extractScriptable((ScriptableObject) jsValue);
        }
        if (jsValue instanceof CharSequence) {
            //convert any string-like type to plain java strings
            return jsValue.toString();
        }
        if (jsValue instanceof Map) {
            return convertMap((Map) jsValue);
        }
        if (jsValue instanceof Iterable) {
            return convertIterable((Iterable) jsValue);
        }
        if (jsValue instanceof Number) {
            return convertNumber((Number) jsValue);
        }
        if (jsValue instanceof Object[]) {
            return convertIterable(Arrays.asList((Object[]) jsValue));
        }
        return jsValue;
    }

    private Object convertNumber(Number numValue) {
        if (numValue instanceof Double) {
            if (isLong((Double) numValue)) {
                return numValue.longValue();
            }
        }
        if (numValue instanceof Float) {
            if (isLong((Float) numValue)) {
                return numValue.longValue();
            }
        }
        return numValue;
    }

    private boolean isLong(double x) {
        return x == Math.floor(x);
    }

    private Object forceAsync(Object jsValue) {
        AsyncContainer asyncContainer = new AsyncContainer();
        asyncExtractor.extract(jsValue, asyncContainer.createCompletionCallback());
        return asyncContainer.getResult();
    }

    private Object extractScriptable(ScriptableObject scriptableObject) {
        Object obj = tryKnownConversion(scriptableObject);
        if (obj != null) { return obj; }
        if (scriptableObject instanceof NativeArray) {
            return convertNativeArray((NativeArray) scriptableObject);
        }
        if (scriptableObject instanceof Function) {
            return callFunction((Function) scriptableObject);
        }
        return new HybridObject(scriptableObject, this);
    }

    private Object callFunction(Function function) {
        Object result = JsUtils.callFn(function, null, function, function, new Object[0]);
        return adapt(result);
    }

    private Object[] convertNativeArray(NativeArray nativeArray) {
        int length = (int) nativeArray.getLength();
        Object[] objects = new Object[length];
        for (int i = 0; i < length; i++) {
            Object jsItem = nativeArray.get(i, nativeArray);
            objects[i] = adapt(jsItem);
        }
        return objects;
    }

    private Map<Object, Object> convertMap(Map<Object, Object> original) {
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : original.entrySet()) {
            map.put(entry.getKey(), adapt(entry.getValue()));
        }
        return map;
    }

    private List<Object> convertIterable(Iterable<Object> iterable) {
        List<Object> objects = new ArrayList<Object>();
        for (Object obj : iterable) {
            objects.add(adapt(obj));
        }
        return objects;
    }

    private static Object tryKnownConversion(ScriptableObject object) {
        String className = object.getClassName();
        Class<?> cls = knownConversions.get(className);
        return (cls == null) ? null : Context.jsToJava(object, cls);
    }
}
