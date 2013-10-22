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

package org.apache.sling.resource.inventory.impl;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class ResourceTraversor {

    private final JSONObject startObject;

    private final Resource startResource;

    public ResourceTraversor(final Resource resource)
    throws JSONException {
        this.startResource = resource;
        this.startObject = this.adapt(resource);
    }

    /**
     * Recursive descent from startResource, collecting JSONObjects into
     * startObject.
     * @throws JSONException
     */
    public void collectResources() throws JSONException {
        collectChildren(startResource, this.startObject);
    }

    /**
     * @param resource
     * @param currentLevel
     * @throws JSONException
     */
    private void collectChildren(final Resource resource,
            final JSONObject jsonObj)
    throws JSONException {

        final Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            final Resource res = children.next();
            final JSONObject json = collectResource(res, jsonObj);
            collectChildren(res, json);
        }
    }

    /**
     * Adds a resource in the JSON tree.
     *
     * @param resource The resource to add
     * @param level The level where this resource is located.
     * @throws JSONException
     */
    private JSONObject collectResource(final Resource resource, final JSONObject parent)
    throws JSONException {
        final JSONObject o = adapt(resource);
        parent.put(resource.getName(), o);
        return o;
    }

    /**
     * Adapt a Resource to a JSON Object.
     *
     * @param resource The resource to adapt.
     * @return The JSON representation of the Resource
     * @throws JSONException
     */
    private JSONObject adapt(final Resource resource) throws JSONException {
        return JsonObjectCreator.create(resource);
    }

    public JSONObject getJSONObject() {
        return startObject;
    }
}
