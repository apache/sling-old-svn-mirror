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
package org.apache.sling.resourcebuilder.api;

import java.io.InputStream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/** Builds Sling Resources using a simple fluent API */
@ProviderType
public interface ResourceBuilder {
    
    /** Default primary type for resources created by this builder */
    public static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured";
    
    /** Start a ResourceBuilder using the supplied parent resource 
     *  @return the new builder
     * */
    ResourceBuilder forParent(Resource parent);
    
    /** Start a ResourceBuilder using the supplied ResourceResolver,
     *  starting with the root resource as the builder's parent resource. 
     *  @return the new builder
     * */
    ResourceBuilder forResolver(ResourceResolver r);
    
    /** Create a Resource, which optionally becomes the current 
     *  parent Resource. 
     * @param path The path of the Resource to create.
     *    If it's a relative path this builder's current resource is used as parent.
     *    Otherwise the resource is created ad the given absoulte path.
     * @param properties optional name-value pairs 
     * @return this builder
     */
    ResourceBuilder resource(String path, Object ... properties);

    /** Create a file under the current parent resource
     * @param filename The name of the created file
     * @param data The file data
     * @param mimeType If null, use the Sling MimeTypeService to set the mime type
     * @param lastModified if < 0, current time is used
     * @return this builder
     */
    ResourceBuilder file(String filename, InputStream data, String mimeType, long lastModified);
    
    /** Create a file under the current parent resource. Mime type is set using the 
     *  Sling MimeTypeService, and last modified is set to current time.
     * @param filename The name of the created file
     * @param data The file data
     * @return this builder
     */
    ResourceBuilder file(String filename, InputStream data);
    
    /** Commit created resources */
    ResourceBuilder commit();
    
    /** Set the primary type for intermediate resources created
     *  when the parent of resource being created does not exist.
     * @param primaryType If null the DEFAULT_PRIMARY_TYPE is used.
     * @return this builder
     */
    ResourceBuilder withIntermediatePrimaryType(String primaryType);
    
    /** Set siblings mode (as opposed to hierarchy mode) where creating a resource 
     *  doesn't change the current parent. Used to create flat structures.
     *  This is off by default.
     * @return this builder
     */
    ResourceBuilder siblingsMode();
    
    /** Set hierarchy mode (as opposed to siblings mode) where creating a resource 
     *  sets it as the current parent. Used to create tree structures.
     *  This is on by default.
     * @return this builder
     */
    ResourceBuilder hierarchyMode();
    
    /** Return the current parent resource */
    Resource getCurrentParent();
    
    /** Reset the current parent Resource to the original one.
     *  Also activates hierarchyMode which is the default mode. */ 
    ResourceBuilder atParent();
}