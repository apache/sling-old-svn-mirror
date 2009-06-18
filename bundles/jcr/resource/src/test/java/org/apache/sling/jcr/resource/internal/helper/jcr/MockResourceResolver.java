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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class MockResourceResolver implements ResourceResolver {

    private final Session session;
    
    MockResourceResolver(Session session) {
        this.session = session;
    }
    
    public Iterator<Resource> findResources(String query, String language) {
        return null;
    }

    public Resource getResource(String path) {
        // assume path is absolute for testing purposes
        try {
            Item item = session.getItem(path);
            
            if (item.isNode()) {
                return new JcrNodeResource(this, (Node) item, null);
            }
            
            return new JcrPropertyResource(this, path, (Property) item, null);
        } catch (Exception e) {
            // don't care
        }
        
        return null;
    }

    public Resource getResource(Resource base, String path) {
        return getResource(base.getPath() + "/" + path);
    }

    public String[] getSearchPath() {
        return new String[0];
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

    public String map(String resourcePath) {
        return null;
    }
    
    public String map(HttpServletRequest request, String resourcePath) {
        return null;
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        return null;
    }

    public Resource resolve(HttpServletRequest request, String absPath) {
        return null;
    }
    
    public Resource resolve(HttpServletRequest request) {
        return null;
    }

    public Resource resolve(String absPath) {
        // TODO Auto-generated method stub
        return null;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

}
