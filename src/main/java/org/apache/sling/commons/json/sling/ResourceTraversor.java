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

package org.apache.sling.commons.json.sling;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.sling.api.request.RecursionTooDeepException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class ResourceTraversor {

    public static final class Entry {
        public final Resource resource;
        public final JSONObject json;

        public Entry(final Resource r, final JSONObject o) {
            this.resource = r;
            this.json = o;
        }
    }

    private long count;

    private long maxResources;

    private final int maxRecursionLevels;

    private final JSONObject startObject;

    private LinkedList<Entry> currentQueue;

    private LinkedList<Entry> nextQueue;

    private final Resource startResource;

    public ResourceTraversor(final int levels, final long maxResources, final Resource resource, final boolean tidy)
    throws JSONException {
        this.maxResources = maxResources;
        this.maxRecursionLevels = levels;
        this.startResource = resource;
        currentQueue = new LinkedList<Entry>();
        nextQueue = new LinkedList<Entry>();
        this.startObject = this.adapt(resource);
    }

    /**
     * Recursive descent from startResource, collecting JSONObjects into
     * startObject. Throws a RecursionTooDeepException if the maximum number of
     * nodes is reached on a "deep" traversal (where "deep" === level greater
     * than 1).
     *
     * @return -1 if everything went fine, a positive valuew when the resource
     *            has more child nodes then allowed.
     * @throws JSONException
     */
    public int collectResources() throws RecursionTooDeepException, JSONException {
        return collectChildren(startResource, this.startObject, 0);
    }

    /**
     * @param resource
     * @param currentLevel
     * @throws JSONException
     */
    private int collectChildren(final Resource resource,
            final JSONObject jsonObj,
            int currentLevel)
    throws JSONException {

        if (maxRecursionLevels == -1 || currentLevel < maxRecursionLevels) {
            final Iterator<Resource> children = ResourceUtil.listChildren(resource);
            while (children.hasNext()) {
                count++;
                final Resource res = children.next();
                // SLING-2320: always allow enumeration of one's children;
                // DOS-limitation is for deeper traversals.
                if (count > maxResources && maxRecursionLevels != 1) {
                    return currentLevel;
                }
                final JSONObject json = collectResource(res, jsonObj);
                nextQueue.addLast(new Entry(res, json));
            }
        }

        while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
            if (currentQueue.isEmpty()) {
                currentLevel++;
                currentQueue = nextQueue;
                nextQueue = new LinkedList<Entry>();
            }
            final Entry nextResource = currentQueue.removeFirst();
            final int maxLevel = collectChildren(nextResource.resource, nextResource.json, currentLevel);
            if ( maxLevel != -1 ) {
                return maxLevel;
            }
        }
        return -1;
    }

    /**
     * Adds a resource in the JSON tree.
     *
     * @param resource The resource to add
     * @param level The level where this resource is located.
     * @throws JSONException
     */
    private JSONObject collectResource(Resource resource, final JSONObject parent)
    throws JSONException {
        final JSONObject o = adapt(resource);
        parent.put(ResourceUtil.getName(resource), o);
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
        return JsonObjectCreator.create(resource, 0);
    }

    /**
     * @return The number of resources this visitor found.
     */
    public long getCount() {
        return count;
    }

    public JSONObject getJSONObject() {
        return startObject;
    }
}
