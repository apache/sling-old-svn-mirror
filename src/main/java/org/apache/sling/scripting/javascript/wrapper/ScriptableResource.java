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
package org.apache.sling.scripting.javascript.wrapper;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.javascript.SlingWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * Resource in JavaScript has following signature: [Object] getData(); [Object]
 * data [Item] getItem(); [Item] item [String] getResourceType(); [String] type
 * [String] getPath(); [String] path
 */
public class ScriptableResource extends ScriptableObject implements SlingWrapper {

    public static final String CLASSNAME = "Resource";
    public static final Class<?> [] WRAPPED_CLASSES = { Resource.class };

    private Resource resource;

    public ScriptableResource() {
    }

    public ScriptableResource(Resource resource) {
        this.resource = resource;
    }

    public void jsConstructor(Object res) {
        this.resource = (Resource) res;
    }

    public Class<?> [] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    public Object jsFunction_getObject() {
        return toJS(resource.adaptTo(Object.class));
    }

    public String jsFunction_getResourceType() {
        return resource.getResourceType();
    }

    public String jsGet_type() {
        return this.jsFunction_getResourceType();
    }

    public String jsFunction_getPath() {
        return resource.getPath();
    }

    public String jsGet_path() {
        return this.jsFunction_getPath();
    }

    public Object jsFunction_getMetadata() {
        return toJS(resource.getResourceMetadata());
    }

    public Object jsGet_meta() {
        return jsFunction_getMetadata();
    }

    public Object jsFunction_getResourceResolver() {
        return toJS(resource.getResourceResolver());
    }

    public Object jsGet_resourceResolver() {
        return jsFunction_getResourceResolver();
    }

    public static Object jsFunction_adaptTo(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {

        // get and unwrap the argument
        Object arg = (args.length > 0) ? args[0] : null;
        while (arg instanceof Wrapper) {
            arg = ((Wrapper) arg).unwrap();
        }

        // try to get the Class object for the argument
        Class<?> adapter = null;
        if (arg instanceof Class) {

            adapter = (Class<?>) arg;

        } else if (arg != null && arg != Undefined.instance) {

            // try loading the class from the String
            String className = ScriptRuntime.toString(arg);
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = thisObj.getClass().getClassLoader();
                }
                adapter = Class.forName(className, true, loader);
            } catch (Exception e) {
                // TODO: log exception
            }

        }

        if (adapter != null) {
            ScriptableResource sr = (ScriptableResource) thisObj;
            return sr.toJS(sr.resource.adaptTo(adapter));
        }

        return Undefined.instance;
    }

    public Class<?> jsGet_javascriptWrapperClass() {
        return getClass();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object getDefaultValue(Class typeHint) {
        return resource.getPath();
    }

    public void setResource(Resource entry) {
        this.resource = entry;
    }

    // ---------- Wrapper interface --------------------------------------------

    // returns the wrapped resource
    public Object unwrap() {
        return resource;
    }

    //---------- Internal helper ----------------------------------------------

    private Object toJS(Object javaObject) {
        if (javaObject == null) {
            return Undefined.instance;
        }

        return ScriptRuntime.toObject(this, javaObject);
    }
}
