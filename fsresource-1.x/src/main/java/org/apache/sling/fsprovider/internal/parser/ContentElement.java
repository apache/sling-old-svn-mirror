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
package org.apache.sling.fsprovider.internal.parser;

import java.util.Map;

/**
 * Represents a resource or node in the content hierarchy.
 */
public interface ContentElement {

    /**
     * @return Resource name. The root resource has no name (null).
     */
    String getName();
    
    /**
     * Properties of this resource.
     * @return Properties (keys, values)
     */
    Map<String, Object> getProperties();
    
    /**
     * Get children of current resource. The Map preserves the ordering of children.
     * @return Children (child names, child objects)
     */
    Map<String, ContentElement> getChildren();
    
    /**
     * Get child or descendant
     * @param path Relative path to address child or one of it's descendants (use "/" as hierarchy separator).
     * @return Child or null if no child found with this path
     */
    ContentElement getChild(String path);
    
}
