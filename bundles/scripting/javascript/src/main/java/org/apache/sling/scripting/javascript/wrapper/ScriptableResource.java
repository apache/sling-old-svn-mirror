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
public class ScriptableResource extends ScriptableObject implements
        SlingWrapper {

    public static final String CLASSNAME = "Resource";

    public static final Class<?>[] WRAPPED_CLASSES = { Resource.class };

    private Resource resource;

    public ScriptableResource() {
    }

    public ScriptableResource(Resource resource) {
        this.resource = resource;
    }

    public void jsConstructor(Object res) {
        this.resource = (Resource) res;
    }

    /**
     * Mapps getPath() method as path property.
     */
    public String jsGet_path() {
        return this.jsFunction_getPath();
    }

    /**
     * Mapps getPath() method as getPath() method.
     */
    public String jsFunction_getPath() {
        return resource.getPath();
    }

    /**
     * Maps getResourceType() to type property. This property is deprecated
     * since it does not correctly map the getResourceType() method name to a
     * property.
     *
     * @deprecated since 2.1.0 because it maps the method name incorrectly.
     */
    @Deprecated
    public String jsGet_type() {
        return this.jsFunction_getResourceType();
    }

    /**
     * Maps getResourceType() to resourceType property.
     */
    public String jsGet_resourceType() {
        return this.jsFunction_getResourceType();
    }

    /**
     * Maps getResourceType() to the getResourceType() method.
     */
    public String jsFunction_getResourceType() {
        return resource.getResourceType();
    }

    /**
     * Maps getResourceSuperType() to resourceSuperType property.
     */
    public String jsGet_resourceSuperType() {
        return this.jsFunction_getResourceSuperType();
    }

    /**
     * Maps getResourceSuperType() to the getResourceSuperType() method.
     */
    public String jsFunction_getResourceSuperType() {
        return resource.getResourceSuperType();
    }

    /**
     * Maps getResourceMetadata() to meta property. This property is deprecated
     * since it does not correctly map the getResourceType() method name to a
     * property.
     *
     * @deprecated since 2.1.0 because it maps the method name incorrectly.
     */
    @Deprecated
    public Object jsGet_meta() {
        return jsFunction_getResourceMetadata();
    }

    /**
     * Maps getResourceMetadata() to resourceMetadata property.
     */
    public Object jsGet_resourceMetadata() {
        return jsFunction_getResourceMetadata();
    }

    /**
     * Maps getResourceMetadata() to getMetadata() method. This method is
     * deprecated since it has the wrong name to support the
     * getResourceMetadata() method.
     *
     * @deprecated since 2.1.0 because the method is named incorrectly.
     */
    @Deprecated
    public Object jsFunction_getMetadata() {
        return jsFunction_getResourceMetadata();
    }

    /**
     * Maps getResourceMetadata() to getResourceMetadata method.
     */
    public Object jsFunction_getResourceMetadata() {
        return toJS(resource.getResourceMetadata());
    }

    /**
     * Maps getResourceResolver() to resourceResolver property.
     */
    public Object jsFunction_getResourceResolver() {
        return toJS(resource.getResourceResolver());
    }

    /**
     * Maps getResourceResolver() to getResourceResolver method.
     */
    public Object jsGet_resourceResolver() {
        return jsFunction_getResourceResolver();
    }

    /**
     * Helper method to easily retrieve the default adapted object of the
     * resource. In case of Object Content Mapping support, this method will
     * return the correctly mapped content object for this resource.
     * <p>
     * Calling this method is equivalent to calling the adaptTo method with the
     * argument "java.lang.Object".
     */
    public Object jsFunction_getObject() {
        return toJS(resource.adaptTo(Object.class));
    }

    /**
     * Implements the adaptTo() method for JavaScript scripts. This method takes
     * either a java.lang.Class object or a String containing the fully
     * qualified name of the class to adapt to.
     * <p>
     * Supporting String as an argument to this method allows for much easier
     * use in JavaScript since instead of for example writing
     * <i>"javax.jcr.Node"</i> instead of the much clumsier
     * <i>Packages.javax.jcr.Node</i>.
     *
     * @param cx The current Rhino context
     * @param thisObj The ScriptableResource object in which the method is
     *            called.
     * @param args The argument vector. Only the first argument is used which is
     *            expected to be a Class object or a String. If no argument is
     *            supplied or it has the wrong type, this method just returns
     *            <code>null</code>.
     * @param funObj The object representing the JavaScript adaptTo function.
     * @return The object to which the resource adapts or <code>null</code> if
     *         the resource does not adapt to the required type or if the
     *         argument is of the wrong type or missing.
     */
    public static Object jsFunction_adaptTo(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {

        // get and unwrap the argument
        Object arg = (args.length > 0) ? args[0] : null;
        while (arg instanceof Wrapper) {
            arg = ((Wrapper) arg).unwrap();
        }

        // try to get the Class object for the argument
        Class<?> adapter = null;
        if (arg instanceof Class<?>) {

            adapter = (Class<?>) arg;

        } else if (arg != null && arg != Undefined.instance) {

            // try loading the class from the String
            String className = ScriptRuntime.toString(arg);
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = thisObj.getClass().getClassLoader();
                }
                adapter = loader.loadClass(className);
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

    // --------- ScriptableObject API

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getDefaultValue(Class typeHint) {
        if (resource != null) {
            return resource.getPath();
        }

        return String.valueOf((Object) null);
    }

    public void setResource(Resource entry) {
        this.resource = entry;
    }

    // ---------- SlingWrapper API

    public Class<?>[] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }

    // ---------- Wrapper interface --------------------------------------------

    // returns the wrapped resource
    public Object unwrap() {
        return resource;
    }

    // ---------- Internal helper ----------------------------------------------

    private Object toJS(Object javaObject) {
        if (javaObject == null) {
            return Undefined.instance;
        }

        return ScriptRuntime.toObject(this, javaObject);
    }
}
