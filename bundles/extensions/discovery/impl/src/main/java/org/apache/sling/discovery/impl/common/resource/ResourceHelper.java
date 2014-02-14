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
package org.apache.sling.discovery.impl.common.resource;

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

    public static Resource getOrCreateResource(
            final ResourceResolver resourceResolver, final String path)
            throws PersistenceException {
    	return ResourceUtil.getOrCreateResource(resourceResolver, path,
    			(String)null, null, true);
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

    /**
     * @deprecated use {@link #getOrCreateResource(ResourceResolver, String)} instead
     */
    @Deprecated
    public static Resource createResource(final ResourceResolver resourceResolver,
            final String path) throws PersistenceException {
    	return getOrCreateResource(resourceResolver, path);
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

    public static void moveResource(Resource res, String path) throws PersistenceException {
        try{
            Session session = res.adaptTo(Node.class).getSession();
            session.move(res.getPath(), path);
        } catch(RepositoryException re) {
            throw new PersistenceException(String.valueOf(re), re);
        }
    }

	/** SLING-2883 : properly test for valid property names **/
	public static boolean isValidPropertyName(String name) {
		if (name==null || name.length()==0) {
			return false;
		}
		return name.matches("[a-zA-Z0-9._-]+");
	}

}
