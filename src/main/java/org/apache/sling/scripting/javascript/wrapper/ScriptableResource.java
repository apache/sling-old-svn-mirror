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

import javax.jcr.Node;

import org.apache.sling.api.resource.Resource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * Resource in JavaScript has following signature: [Object] getData(); [Object]
 * data [Item] getItem(); [Item] item [String] getResourceType(); [String] type
 * [String] getPath(); [String] path
 */
public class ScriptableResource extends ScriptableObject implements Wrapper {

    public static final String CLASSNAME = "Resource";

    private Resource resource;

    public ScriptableResource() {
    }

    public ScriptableResource(Resource resource) {
        this.resource = resource;
    }

    public void jsConstructor(Object res) {
        this.resource = (Resource) res;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    public Object jsFunction_getObject() {
        Object object = resource.adaptTo(Object.class);
        return (object != null) ? object : Undefined.instance;
    }

    public Object jsFunction_getNode() {
        Node node = resource.adaptTo(Node.class);
        return node != null ? node : Undefined.instance;
    }

    /** alias for getNode */
    public Object jsGet_node() {
        return jsFunction_getNode();
    }

    public String jsFunction_getResourceType() {
        return resource.getResourceType();
    }

    public String jsGet_type() {
        return this.jsFunction_getResourceType();
    }

    public Object jsFunction_getPath() {
        return Context.javaToJS(resource.getPath(), this);
    }

    public Object jsGet_path() {
        return this.jsFunction_getPath();
    }

    public Object jsFunction_getMetadata() {
        return resource.getResourceMetadata();
    }

    public Object jsGet_meta() {
        return resource.getResourceMetadata();
    }

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

}
