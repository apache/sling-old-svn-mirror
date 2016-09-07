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
package org.apache.sling.jcr.slingoakrestrictions.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.jackrabbit.oak.util.TreeUtil;
import org.apache.sling.api.SlingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of the {@link RestrictionPattern} interface that returns {@code true} if the resource type of the target tree (or the
 * parent of a target property) is contained in the configured resource type. */
public class ResourceTypePattern implements RestrictionPattern {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceTypePattern.class);

    static final String DEFAULT_PATH = ".";
    static final String PATH_MARKER = "@";
    
    static final String SLING_RESOURCE_TYPE = SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE;


    private final String limitedToPath;
    private final boolean matchChildren;
    
    private final Map<String,Set<String>> resourceTypesByPath;
    
  
    ResourceTypePattern(@Nonnull Iterable<String> resourceTypesRaw, String limitedToPath, boolean matchChildren) {
        
        this.limitedToPath = limitedToPath;
        this.matchChildren = matchChildren;
        
        Map<String,Set<String>> resourceTypesByPath = new LinkedHashMap<String,Set<String>>();
        for (String resourceTypeRaw : resourceTypesRaw) {
            String path;
            String resourceType;
            if(resourceTypeRaw.contains(PATH_MARKER)) {
                String[] bits = resourceTypeRaw.trim().split(PATH_MARKER, 2);
                path = bits[1];
                resourceType = bits[0];
            } else {
                path = DEFAULT_PATH;
                resourceType = resourceTypeRaw;
            }
                
            Set<String> resourceTypesForPath = resourceTypesByPath.get(path);
            if(resourceTypesForPath==null) {
                resourceTypesForPath = new HashSet<String>();
                resourceTypesByPath.put(path, resourceTypesForPath);
            }
            resourceTypesForPath.add(resourceType);
        }
        
        this.resourceTypesByPath = Collections.unmodifiableMap(resourceTypesByPath);
        LOG.trace("pattern setup with resourceTypesByPath={}",  this.resourceTypesByPath);
    }
    
    String getLimitedToPath() {
        return limitedToPath;
    }

    boolean isMatchChildren() {
        return matchChildren;
    }

    @Override
    public boolean matches(@Nonnull Tree tree, @Nullable PropertyState property) {
        boolean isMatch = matchesAtTree(tree);
        if(!isMatch && matchChildren) { // try parent hierarchy
            Tree treeCursor = tree;
            while(!isMatch && !treeCursor.isRoot()) {
                treeCursor = treeCursor.getParent();
                if(!treeCursor.getPath().startsWith(limitedToPath)) {
                    if(LOG.isTraceEnabled()) {
                        LOG.trace("Breaking parent traversal loop: tree={}, limitedToPath={}", treeCursor.getPath(), limitedToPath);
                    }
                    break; 
                }
                isMatch = matchesAtTree(treeCursor);
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Match for "+tree.getPath()+": "+ (isMatch ? "YES":"NO") + " ("+this+")");
        }
        return isMatch;
    }

    private boolean matchesAtTree(Tree tree) {
        boolean isResourceTypeMatch = false;
        for (String path : resourceTypesByPath.keySet()) {

                Tree treeToCheck = tree; // the default if e.g. just the resource type without @path is given
                if(!DEFAULT_PATH.equals(path)) {
                    try {
                        String[] segments = path.split("/");
                        for (String string : segments) {
                            treeToCheck = treeToCheck.getChild(string);
                        }
                    } catch (IllegalArgumentException e) {
                        continue; // continue and ignore if path is not found
                    } 
                }
                
                Set<String> resourceTypesForPath = resourceTypesByPath.get(path);
                String actualResourceType = TreeUtil.getString(treeToCheck, SLING_RESOURCE_TYPE);
                isResourceTypeMatch = resourceTypesForPath.contains(actualResourceType);
                
                if(LOG.isTraceEnabled()) {
                    LOG.trace("isResourceTypeMatch={} (checked at path {} at sub path {})", new Object[]{isResourceTypeMatch, tree.getPath(), path});
                }
                if(isResourceTypeMatch) {
                    break; // return as quickly as possible
                }

        }
        return isResourceTypeMatch;
    }


    @Override
    public boolean matches(@Nonnull String path) {
        return false;
    }

    @Override
    public boolean matches() {
        // node type pattern never matches for repository level permissions
        return false;
    }

    // -------------------------------------------------------------< Object >---

    @Override
    public String toString() {
        return "ResourceTypePattern [limitedToPath=" + limitedToPath + ", matchChildren=" + matchChildren + ", resourceTypesByPath="
                + resourceTypesByPath + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((limitedToPath == null) ? 0 : limitedToPath.hashCode());
        result = prime * result + (matchChildren ? 1231 : 1237);
        result = prime * result + ((resourceTypesByPath == null) ? 0 : resourceTypesByPath.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        ResourceTypePattern other = (ResourceTypePattern) obj;
        if(limitedToPath == null) {
            if(other.limitedToPath != null)
                return false;
        } else if(!limitedToPath.equals(other.limitedToPath))
            return false;
        if(matchChildren != other.matchChildren)
            return false;
        if(resourceTypesByPath == null) {
            if(other.resourceTypesByPath != null)
                return false;
        } else if(!resourceTypesByPath.equals(other.resourceTypesByPath))
            return false;
        return true;
    }
    
}