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
package org.apache.sling.installer.api.info;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.osgi.framework.Version;


/**
 * A resource provides all relevant information about a registered
 * resource.
 *
 * @since 1.0
 */
public interface Resource extends RegisteredResource {

    /**
     * Get the current state of the resource.
     */
    ResourceState getState();

    /**
     * Return the version of the artifact.
     * @return The version of the artifact or <code>null</code>
     */
    Version getVersion();

    /**
     * When did the last change happen?
     * @return -1 if no change , 0 if unknown, > 0 otherwise
     */
    long getLastChange();

    /**
     * Get the value of an attribute.
     * Attributes are specific to the resource and are either set
     * by a {@link ResourceTransformer} or a {@link InstallTask} for
     * processing.
     * @param key The name of the attribute
     * @return The value of the attribute or <code>null</code>
     */
    Object getAttribute(String key);
}
