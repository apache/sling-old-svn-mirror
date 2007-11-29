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
package org.apache.sling.scripting.javascript;

import org.apache.sling.api.resource.NodeProvider;
import org.apache.sling.api.resource.ObjectProvider;
import org.apache.sling.api.resource.Resource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * Resource in JavaScript has following signature: [Object] getData(); [Object]
 * data [Item] getItem(); [Item] item [String] getResourceType(); [String] type
 * [String] getURI(); [String] uri
 */
public class ScriptableResource extends ScriptableObject {
    private Resource resource;

    public ScriptableResource() {
    }

    public void jsConstructor(Object res) {
        this.resource = (Resource) res;
    }

    @Override
    public String getClassName() {
        return "Resource";
    }

    public Object jsFunction_getObject() {
        if (resource instanceof ObjectProvider) {
            return ((ObjectProvider) resource).getObject();
        }
        return Undefined.instance;
    }

    public Object jsFunction_getNode() {
        if (resource instanceof NodeProvider) {
            return new ScriptableNode(((NodeProvider) resource).getNode());
        }
        return Undefined.instance;
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

    public Object jsFunction_getURI() {
        return Context.javaToJS(resource.getURI(), this);
    }

    public Object jsGet_uri() {
        return this.jsFunction_getURI();
    }

    public Object jsFunction_getMetadata() {
        return resource.getResourceMetadata();
    }

    public Object jsGet_meta() {
        return resource.getResourceMetadata();
    }

    @Override
    public Object getDefaultValue(Class typeHint) {
        return resource.getURI();
    }

    public void setResource(Resource entry) {
        this.resource = entry;
    }

}
