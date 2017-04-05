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
package org.apache.sling.fsprovider.internal;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Maps files to resources.
 */
public interface FsResourceMapper {

    /**
     * Get single resource.
     * @param resolver Resource resolver
     * @param resourcePath Resource path
     * @return Resource or null if not exists
     */
    Resource getResource(ResourceResolver resolver, String resourcePath);
    
    /**
     * Get children of resource.
     * @param resolver Resource resolver.
     * @param parent Parent resource.
     * @return Child resources or null if no children exist
     */
    Iterator<Resource> getChildren(ResourceResolver resolver, Resource parent);
    
}
