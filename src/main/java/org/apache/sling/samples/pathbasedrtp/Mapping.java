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

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Map a path prefix to a (1-based) index in the path components */
class Mapping {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    String path;
    final String fixedNodeType;
    final Pattern nodeTypeExpr;
    int resourceTypeIndex;
    public static final String DEFAULT_NODE_TYPE = "nt:unstructured";
    
    Mapping(String definition) {
        final String [] parts = definition.trim().split(":");
        if(parts.length < 2) {
            throw new IllegalArgumentException("Illegal definition'" + definition + "'");
        }
        
        path = parts[0];
        try {
            resourceTypeIndex = Integer.parseInt(parts[1]);
        } catch(Exception e) {
            log.warn("Invalid path index in Mapping {}", definition);
        }

        // Parse the definition: two parts means default nodetype,
        // more than two means fixed nodetype or regexp.
        // Nodetypes can contain : which is our separator - not fun.
        if(parts.length > 2) {
            final StringBuilder sb = new StringBuilder();
            for(int i=2; i < parts.length; i++) {
                sb.append(parts[i]);
                if(i < parts.length - 1) {
                    sb.append(":");
                }
            }
            
            String expr = sb.toString();
            if(expr.startsWith("(") && expr.endsWith(")")) {
                // Definition in brackets -> regexp
                fixedNodeType = null;
                expr = expr.substring(1);
                expr = expr.substring(0, expr.length() - 1);
                nodeTypeExpr = Pattern.compile(expr);
            } else {
                // No brackets -> fixed node type
                fixedNodeType = expr;
                nodeTypeExpr = null;
            }
        } else {
            fixedNodeType = DEFAULT_NODE_TYPE;
            nodeTypeExpr = null;
        }
        
        log.debug(toString());
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Mapping: path=");
        sb.append(path);
        sb.append(", resource type index=");
        sb.append(resourceTypeIndex);
        if(fixedNodeType != null) {
            sb.append(", node type=");
            sb.append(fixedNodeType);
        } else {
            sb.append(", node type regexp=");
            sb.append(nodeTypeExpr);
        }
        return sb.toString();
    }
    
    String getResourceType(String nodePath, String nodeType) {
        String result = null;
        if(path!=null && nodePath.startsWith(path) && nodeTypeMatches(nodeType)) {
            final String [] paths = nodePath.split("/");
            if(paths.length >= resourceTypeIndex+1) {
                result = paths[resourceTypeIndex];
            }
        }
        return result;
    }
    
    private boolean nodeTypeMatches(String nodeType) {
        if(fixedNodeType != null) {
            return fixedNodeType.equals(nodeType);
        } else if(nodeTypeExpr == null) {
            throw new IllegalStateException("Neither fixedNodeType nor nodeTypeExpr supplied in " + this);
        }
        return nodeTypeExpr.matcher(nodeType).matches();
        
    }
}