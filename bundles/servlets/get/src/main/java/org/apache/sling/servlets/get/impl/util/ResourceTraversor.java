/*
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
 */
package org.apache.sling.servlets.get.impl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.request.RecursionTooDeepException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;

public class ResourceTraversor
{
    public static final class Entry {
        public final Resource resource;
        public final JsonObjectBuilder json;

        public Entry(final Resource r, final JsonObjectBuilder o) {
            this.resource = r;
            this.json = o;
        }
    }
    
    Map<JsonObjectBuilder, List<Entry>> tree = new HashMap<>();

    private long count;

    private long maxResources;

    private final int maxRecursionLevels;

    private final JsonObjectBuilder startObject;

    private LinkedList<Entry> currentQueue;

    private LinkedList<Entry> nextQueue;

    private final Resource startResource;

    /** Create a ResourceTraversor, optionally limiting recursion and total number of resources
     * @param levels recursion levels limit, -1 means no limit
     * @param maxResources maximum number of resources to collect, ignored if levels == 1
     * @param resource the root resource to traverse
     * @param tidy not used
     * @throws JSONException
     */
    public ResourceTraversor(final int levels, final long maxResources, final Resource resource) {
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
    public int collectResources() throws RecursionTooDeepException {
        return collectChildren(startResource, this.startObject, 0);
    }

    /**
     * @param resource
     * @param currentLevel
     * @throws JSONException
     */
    private int collectChildren(final Resource resource,
            final JsonObjectBuilder jsonObj,
            int currentLevel) {

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
                Entry child = new Entry(res, adapt(res));
                nextQueue.addLast(child);
                List<Entry> childTree = tree.get(jsonObj);
                if (childTree == null)
                {
                    childTree = new ArrayList<>();
                    tree.put(jsonObj, childTree);
                }
                childTree.add(child);
            }
        }

        // do processing only at first level to avoid unnecessary recursion
        if (currentLevel > 0) {
            return -1;
        }

        List<Entry> entries = new ArrayList<>();
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
     * Adapt a Resource to a JSON Object.
     *
     * @param resource The resource to adapt.
     * @return The JSON representation of the Resource
     * @throws JSONException
     */
    private JsonObjectBuilder adapt(final Resource resource) {
        return JsonObjectCreator.create(resource, 0);
    }

    /**
     * @return The number of resources this visitor found.
     */
    public long getCount() {
        return count;
    }

    public JsonObject getJSONObject() {
        
        return addChildren(startObject).build();
    }
    
    private JsonObjectBuilder addChildren(JsonObjectBuilder builder) {
        List<Entry> children = tree.get(builder);
        
        if (children != null)
        {
            for (Entry child : children) {
                addChildren(child.json);

                builder.add(ResourceUtil.getName(child.resource), child.json);
            }
        }
        
        return builder;
    }
}
