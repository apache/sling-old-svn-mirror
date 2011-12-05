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

package org.apache.sling.servlets.get.impl.helpers;

import org.apache.sling.api.request.RecursionTooDeepException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.RepositoryException;

public class ResourceTraversor {

  private long count;
  private long maxResources;
  private int maxRecursionLevels;
  private JSONObject startObject;
  private String startingPath;
  private LinkedList<Resource> currentQueue;
  private LinkedList<Resource> nextQueue;
  private Resource startResource;
  private JsonResourceWriter jsResourceWriter;
  private boolean tidy;

  public ResourceTraversor(int levels, long maxNodes, Resource resource,
      boolean tidy) throws RepositoryException, JSONException {
    this.setMaxNodes(maxNodes);
    this.maxRecursionLevels = levels;
    this.startResource = resource;
    this.tidy = tidy;
    startingPath = resource.getPath();
    jsResourceWriter = new JsonResourceWriter(null);
    currentQueue = new LinkedList<Resource>();
    nextQueue = new LinkedList<Resource>();
    startObject = adapt(resource);
  }

  /**
   * Recursive descent from startResource, collecting JSONObjects into startObject.
   * Throws a RecursionTooDeepException if the maximum number of nodes is reached on a
   * "deep" traversal (where "deep" === level greateer than 1).
   *
   * @throws RepositoryException
   * @throws RecursionTooDeepException
   *           When the resource has more child nodes then allowed.
   * @throws JSONException
   */
  public void collectResources() throws RepositoryException, RecursionTooDeepException,
      JSONException {
    collectChildren(startResource, 0);
  }

  /**
   *
   * @param resource
   * @param currentLevel
   * @throws RecursionTooDeepException
   * @throws JSONException
   * @throws RepositoryException
   */
  private void collectChildren(Resource resource, int currentLevel)
      throws RecursionTooDeepException, JSONException, RepositoryException {

    if (maxRecursionLevels == -1 || currentLevel < maxRecursionLevels) {
      final Iterator<Resource> children = ResourceUtil.listChildren(resource);
      while (children.hasNext()) {
        count++;
        Resource res = children.next();
        // SLING-2320: always allow enumeration of one's children;
        // DOS-limitation is for deeper traversals.
        if (count > maxResources && maxRecursionLevels > 1) {
            throw new RecursionTooDeepException(String.valueOf(currentLevel));
        }
        collectResource(res, currentLevel);
        nextQueue.addLast(res);
      }
    }

    while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
      if (currentQueue.isEmpty()) {
        currentLevel++;
        currentQueue = nextQueue;
        nextQueue = new LinkedList<Resource>();
      }
      Resource nextResource = currentQueue.removeFirst();
      collectChildren(nextResource, currentLevel);
    }
  }

  /**
   * Adds a resource in the JSON tree.
   *
   * @param resource
   *          The resource to add
   * @param level
   *          The level where this resource is located.
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void collectResource(Resource resource, int level)
      throws RepositoryException, JSONException {

    if (!resource.getPath().equals(startingPath)) {
      JSONObject o = adapt(resource);
      getParentJSONObject(resource, level).put(ResourceUtil.getName(resource),
          o);
    }
  }

  /**
   * Adapt a Resource to a JSON Object.
   *
   * @param resource
   *          The resource to adapt.
   * @return The JSON representation of the Resource
   * @throws JSONException
   */
  private JSONObject adapt(Resource resource) throws JSONException {
    // TODO Find a better way to adapt a Resource to a JSONObject.
    StringWriter writer = new StringWriter();
    jsResourceWriter.dump(resource, writer, 0, tidy);
    return new JSONObject(writer.getBuffer().toString());
  }

  /**
   * Get the JSON Object where this resource should be added in.
   *
   * @param resource
   * @param level
   * @return
   * @throws RepositoryException
   * @throws JSONException
   */
  private JSONObject getParentJSONObject(Resource resource, int level)
      throws RepositoryException, JSONException {
    String path = resource.getPath();
    // The root node.
    if (path.equals(startingPath)) {
      return startObject;
    }

    // Some level deeper
    String pathDiff = path.substring(startingPath.length());
    String[] names = pathDiff.split("/");
    JSONObject o = startObject;
    for (String name : names) {
      try {
        o = o.getJSONObject(name);
      } catch (JSONException e) {
      }
    }
    return o;

  }

  /**
   * @return The number of nodes this visitor found.
   */
  public long getCount() {
    return count;
  }

  /**
   * @param maxNodes
   *          the maxNodes to set
   */
  public void setMaxNodes(long maxNodes) {
    this.maxResources = maxNodes;
  }

  /**
   * @return the maxNodes
   */
  public long getMaxNodes() {
    return maxResources;
  }

  public JSONObject getJSONObject() {
    return startObject;
  }

}
