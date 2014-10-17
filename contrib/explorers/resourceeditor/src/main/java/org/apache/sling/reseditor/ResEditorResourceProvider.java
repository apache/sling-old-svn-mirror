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
package org.apache.sling.reseditor;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

//@Component
//@Service
//@Properties({
//    @Property(name=ResourceProvider.ROOTS, value=ResEditorResourceProvider.ROOT)
//})
public class ResEditorResourceProvider implements ResourceProvider{
    public static final String ROOT = "reseditor";
    public static final String ABS_ROOT = "/" + ROOT;

    
    /** ResourceProvider interface */
    public Resource getResource(ResourceResolver resolver, HttpServletRequest req, String path) {
        // Synthetic resource for the root, so that /reseditor works
        if((ABS_ROOT).equals(path)) {
            return new SyntheticResource(resolver, path, "reseditor");
        }
        Resource originalResource = resolver.resolve(req, path.substring(ROOT.length()));
        return originalResource;
    }

    /** ResourceProvider interface */
    public Resource getResource(ResourceResolver resolver, String path) {
        // Synthetic resource for the root, so that /reseditor works
        if((ABS_ROOT).equals(path)) {
            return new SyntheticResource(resolver, path, "reseditor");
        }
        Resource originalResource = resolver.resolve(path.substring(ROOT.length()+1));
        return originalResource;
    }

    /** ResourceProvider interface */
    public Iterator<Resource> listChildren(Resource parent) {
    	ResourceResolver resourceResolver = parent.getResourceResolver();
    	Resource resource = resourceResolver.resolve("/");
    	return resource.listChildren();
    }
    
}
