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
package org.apache.sling.samples.pathbasedrtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Map a path prefix to a (1-based) index in the path components */
class Mapping {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    String path;
    int resourceTypeIndex;
    
    Mapping(String definition) {
        final String [] parts = definition.split(":");
        if(parts.length != 2) {
            log.debug("Invalid Mapping definition ignored: {}", definition);
        } else {
            path = parts[0];
            try {
                resourceTypeIndex = Integer.parseInt(parts[1]);
            } catch(Exception e) {
                log.warn("Invalid path index in Mapping {}", definition);
            }
        }
    }
    
    @Override
    public String toString() {
       return "Mapping: path=" + path + ", resource type index=" + resourceTypeIndex; 
    }
    
    String getResourceType(String nodePath, String nodeType) {
        String result = null;
        if(path!=null && nodePath.startsWith(path) && "nt:unstructured".equals(nodeType)) {
            final String [] paths = nodePath.split("/");
            if(paths.length >= resourceTypeIndex+1) {
                result = paths[resourceTypeIndex];
            }
        }
        return result;
    }
}