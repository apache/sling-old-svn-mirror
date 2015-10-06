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
package org.apache.sling.resourcemerger.spi;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ConsumerType;

/**
 * Service interface which can be implemented to define an algorithm used to pick
 * resources to be merged. For each picker service, a separate {@link ResourceProviderFactory}
 * will be exposed at the MERGE_ROOT of the picker.
 */
@ConsumerType
public interface MergedResourcePicker {

    /**
     * Service property name identifying the root path for the merged resources.
     * By convention, starts, with /mnt, although this is by no means required.
     * The value of this service property must be of type String and must not end
     * in a slash.
     */
    String MERGE_ROOT = "merge.root";

    /**
     * Service property name specifying whether the resources are read-only
     * or support CRUD operations. If not specified this property defaults
     * to <code>true</code>. The value of this property must be of type
     * Boolean.
     */
    String READ_ONLY = "merge.readOnly";

    /**
     * Service property name specifying whether the parent hierarchy is
     * traversed to check for hiding properties. If not specified this
     * property defaults to <code>false</code>. The value of this
     * property must be of type Boolean.
     */
    String TRAVERSE_PARENT = "merge.traverseParent";

    /**
     * @see #pickResources(ResourceResolver, String, Resource)
     * @deprecated
     */
    List<Resource> pickResources(ResourceResolver resolver, String relativePath);

    /**
     * Method invoked by the MergingResourceProvider to identify the resources to be merged for a given
     * relative path. The resources returned may be either resources returned from the ResourceResolver
     * directory or an instance of NonExistingResource.
     *
     * @param resolver the ResourceResolver
     * @param relativePath the path relative to the merge root
     * @param relatedResource an optional resource which is related to the given path (parent or child)
     * @return a List of Resource objects
     */
    List<Resource> pickResources(ResourceResolver resolver, String relativePath, Resource relatedResource);

}
