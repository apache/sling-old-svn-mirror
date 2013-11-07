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
package org.apache.sling.servlets.resolver.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.engine.impl.request.SlingRequestPathInfo;

/** Used by the ServletResolverWebConsolePlugin to decompose URLs */
public class DecomposedURL {
    final RequestPathInfo requestPathInfo;

    DecomposedURL(String urlString) {
        
        if(urlString == null) {
            urlString = "";
        }
        
        // For the path, take everything up to the first dot
        String fullPath = urlString;
        if(urlString.contains("http")) {
            try {
                fullPath = new URL(urlString).getPath();
            } catch(MalformedURLException ignore) {
            }
        }
        final int firstDot = fullPath.indexOf(".");
        
        final ResourceMetadata metadata = new ResourceMetadata();
        final Resource r = new SyntheticResource(null, metadata, null);
        metadata.setResolutionPath(firstDot < 0 ? fullPath : fullPath.substring(0, firstDot));
        metadata.setResolutionPathInfo(firstDot < 0 ? null : fullPath.substring(firstDot));
        requestPathInfo = new SlingRequestPathInfo(r);
    }
    
    RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }
}
