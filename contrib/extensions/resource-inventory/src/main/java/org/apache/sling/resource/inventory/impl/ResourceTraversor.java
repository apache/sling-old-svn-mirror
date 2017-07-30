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

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;

public class ResourceTraversor {

    private final JsonObjectBuilder startObject;

    private final Resource startResource;

    public ResourceTraversor(final Resource resource) {
        this.startResource = resource;
        this.startObject = this.adapt(resource);
    }

    /**
     * Recursive descent from startResource, collecting JSONObjects into
     * startObject.
     * @throws JsonException
     */
    public void collectResources() throws JsonException {
        collectChildren(startResource, this.startObject);
    }

    /**
     * @param resource
     * @param currentLevel
     * @throws JsonException
     */
    private void collectChildren(final Resource resource,
            final JsonObjectBuilder jsonObj)
    throws JsonException {

        final Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            final Resource res = children.next();
            final JsonObjectBuilder json = collectResource(res, jsonObj);
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
    private JsonObjectBuilder collectResource(final Resource resource, final JsonObjectBuilder parent)
    throws JsonException {
        final JsonObjectBuilder o = adapt(resource);
        parent.add(resource.getName(), o);
        return o;
    }

    /**
     * Adapt a Resource to a JSON Object.
     *
     * @param resource The resource to adapt.
     * @return The JSON representation of the Resource
     * @throws JSONException
     */
    private JsonObjectBuilder adapt(final Resource resource) throws JsonException {
        return JsonObjectCreator.create(resource);
    }

    public JsonObject getJsonObject() {
        return startObject.build();
    }
}
