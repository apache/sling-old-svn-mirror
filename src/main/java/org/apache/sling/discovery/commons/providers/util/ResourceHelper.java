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
package org.apache.sling.discovery.commons.providers.util;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * Some helper methods surrounding resources
 */
public class ResourceHelper {

    private static final String DEFAULT_RESOURCE_TYPE = "sling:Folder";

    public static Resource getOrCreateResource(
            final ResourceResolver resourceResolver, final String path)
            throws PersistenceException {
    	return ResourceUtil.getOrCreateResource(resourceResolver, path,
    	        DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, true);
    }

    public static boolean deleteResource(
            final ResourceResolver resourceResolver, final String path) throws PersistenceException {
        final Resource resource = resourceResolver.getResource(path);
        if (resource==null) {
            return false;
        }
        resourceResolver.delete(resource);
        return true;
    }

    /** Compile a string builder containing the properties of a resource - used for logging **/
    public static StringBuilder getPropertiesForLogging(final Resource resource) {
        ValueMap valueMap;
        try{
            valueMap = resource.adaptTo(ValueMap.class);
        } catch(RuntimeException re) {
            return new StringBuilder("non-existing resource: "+resource+" ("+re.getMessage()+")");
        }
        if (valueMap==null) {
            return new StringBuilder("non-existing resource: "+resource+" (no ValueMap)");
        }
        final Set<Entry<String, Object>> entrySet = valueMap.entrySet();
        final StringBuilder sb = new StringBuilder();
        for (Iterator<Entry<String, Object>> it = entrySet.iterator(); it
                .hasNext();) {
            Entry<String, Object> entry = it.next();
            sb.append(" ");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb;
    }

    /**
     * Move resource to given path. Try to do it optimized via JCR API.
     * If JCR is not available, fallback to Sling Resource API. 
     * @param res Source resource
     * @param path Target path
     * @throws PersistenceException
     */
    public static void moveResource(Resource res, String path) throws PersistenceException {
        Node node = res.adaptTo(Node.class);
        if (node != null) {
            try {
                Session session = node.getSession();
                session.move(res.getPath(), path);
            }
            catch (RepositoryException re) {
                throw new PersistenceException("Move from " + res.getPath() + " to " + path + " failed.", re);
            }
        }
        else {
            moveResourceWithResourceAPI(res, path);
        }
    }
    
    /**
     * Move resource to given path with Sling Resource API.
     * @param res Source resource
     * @param path target path
     * @throws PersistenceException
     */
    private static void moveResourceWithResourceAPI(Resource res, String path) throws PersistenceException {
        String parentPath = ResourceUtil.getParent(path);
        Resource parent = res.getResourceResolver().getResource(parentPath);
        if (parent == null) {
            throw new PersistenceException("Parent resource does not exist: " + parentPath);
        }

        // make move with copy + delete
        copyResourceWithResourceAPI(res, parent, ResourceUtil.getName(path));
        res.getResourceResolver().delete(res);
    }

    /**
     * Copy resource to given target with Sling Resource API.
     * @param source Source resource
     * @param destParent Destination parent
     * @param name Destination resource name
     * @throws PersistenceException
     */
    private static void copyResourceWithResourceAPI(Resource source, Resource destParent, String name) throws PersistenceException {
        Resource copy = source.getResourceResolver().create(destParent, name, ResourceUtil.getValueMap(source));
        Iterator<Resource> children = source.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            copyResourceWithResourceAPI(child, copy, child.getName());
        }
    }

}
