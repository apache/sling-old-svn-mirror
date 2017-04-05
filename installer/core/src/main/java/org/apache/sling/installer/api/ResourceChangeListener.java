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
package org.apache.sling.installer.api;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;


/**
 * OSGi Service listening for changes of resources.
 * These resources are not changed through the installer but through
 * any other means like a bundle being installed directly through
 * the framework or a configuration directly changed through the
 * configuration admin.
 *
 * @since 3.1
 */
@ProviderType
public interface ResourceChangeListener {

    /**
     * This attribute defines if a change of the resource should be persisted by the
     * installer. This property is a boolean value defaulting to true.
     *
     * The property should be used, if a resource should not be updated/deleted if
     * the resource is modified/deleted outside of the installer, e.g. if a configuration
     * is changed or deleted through configuration admin.
     * @since 3.2.0
     */
    String RESOURCE_PERSIST = "org.apache.sling.installer.api.persist";

    /**
     * Inform the installer about an added or updated
     * resource
     * @param resourceType The resource type
     * @param entityId     The entity id (symbolic name etc.)
     * @param is           Input stream or
     * @param dict         Dictionary
     * @param attributes   Map of attributes.
     */
    void resourceAddedOrUpdated(String resourceType,
            String entityId,
            InputStream is,
            Dictionary<String, Object> dict,
            Map<String, Object> attributes);

    /**
     * Inform the installer about a removed resource
     * @param resourceType The resource type
     * @param entityId     The entity id (symbolic name etc.)
     */
    void resourceRemoved(String resourceType,
            String entityId);
}