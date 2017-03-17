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
package org.apache.sling.testing.mock.sling.loader;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.contentparser.ContentHandler;

final class LoaderContentHandler implements ContentHandler {

    private static final String JCR_DATA_PLACEHOLDER = ":jcr:data";
    
    private final String rootPath;
    private final ResourceResolver resourceResolver;
    
    public LoaderContentHandler(String rootPath, ResourceResolver resourceResolver) {
        this.rootPath = rootPath;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void resource(String path, Map<String, Object> properties) {
        String fullPath = rootPath;
        if (!StringUtils.equals(path, "/")) {
            fullPath += path;
        }
        String parentPath = ResourceUtil.getParent(fullPath);
        String name = ResourceUtil.getName(fullPath);
        
        Resource parentResource = resourceResolver.getResource(parentPath);
        if (parentResource == null) {
            throw new RuntimeException("Parent resource '" + parentPath + "' not found.");
        }
        try {
            createResource(parentResource, name, properties);
        } 
        catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at '" + fullPath + "'.", ex);
        }
    }

    private Resource createResource(Resource parentResource, String childName, Map<String,Object> content) throws PersistenceException {
        
        // collect all properties first
        boolean hasJcrData = false;
        Map<String, Object> props = new HashMap<String, Object>();
        for (Map.Entry<String,Object> entry : content.entrySet()) {
            final String name = entry.getKey();
            if (StringUtils.equals(name, JCR_DATA_PLACEHOLDER)) {
                hasJcrData = true;
            }
            else {
                props.put(name, entry.getValue());
            }
        }
        
        // create resource
        Resource resource = resourceResolver.create(parentResource, childName, props);
        
        if (hasJcrData) {
            ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
            // we cannot import binary data here - but to avoid complaints by JCR we create it with empty binary data
            valueMap.put(JcrConstants.JCR_DATA, new ByteArrayInputStream(new byte[0]));
        }

        return resource;
    }

}
