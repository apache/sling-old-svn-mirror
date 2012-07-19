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

import java.util.List;

import org.apache.sling.installer.api.tasks.RegisteredResource;

/**
 * The state of the OSGi installer at a given time.
 *
 * @since 1.0
 */
public interface InstallationState {

    /**
     * Active resource groups
     * These resource groups are currently in processing. The first resource
     * of each group is being processing.
     * @return All active resource groups
     */
    List<ResourceGroup> getActiveResources();

    /**
     * Installed resource groups
     * These resources groups are processed.
     * @return All installed resource groups
     */
    List<ResourceGroup> getInstalledResources();

    /**
     * Return all untransformed resources
     * @return All untransformed resources.
     */
    List<RegisteredResource> getUntransformedResources();
}
