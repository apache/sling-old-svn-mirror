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
package org.apache.sling.installer.api.tasks;

import javax.annotation.CheckForNull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is a group of resources all pointing to the same artifact,
 * but maybe in different versions or locations.
 */
@ProviderType
public interface TaskResourceGroup {


    /**
     * Return the first resource if it either needs to be installed or uninstalled.
     * @return The task resource.
     */
    TaskResource getActiveResource();

    /**
     * If there is more than the active resource in the group, return the second
     * resource from the group.
     * @return The next task resource or {@code null}.
     * @since 1.1
     */
    TaskResource getNextActiveResource();

    /**
     * Set the finish state for the active resource.
     * If this resource has been uninstalled, check the next in the list if it needs to
     * be reactivated.
     * @param state The finish state.
     */
    void setFinishState(ResourceState state);

    /**
     * Set the finish state for the active resource and register an alias.
     * This method does the same as {@link #setFinishState(ResourceState)}
     * but in addition registers an alias id for the resource.
     *
     * @param state The finish state.
     * @param alias The alias for this group (may be {@code null}).
     * @see #setFinishState(ResourceState)
     * @since 1.1
     */
    void setFinishState(ResourceState state, String alias);

    /**
     * Set the finish state for the active resource and register an alias.
     * In addition set an error text (may be null).
     * This method does the same as {@link #setFinishState(ResourceState)}
     * but in addition registers an alias id for the resource and an error text.
     *
     * @param state The finish state.
     * @param alias The alias for this group (may be {@code null}).
     * @param error The error text explaining why the finish state was set (may be {@code null}) .
     * @see #setFinishState(ResourceState)
     * @since 1.4
     */
    void setFinishState(ResourceState state, String alias, String error);

    /**
     * Get the current alias for this group.
     * @return The alias or {@code null}.
     * @since 1.1
     */
    @CheckForNull
    String getAlias();
}
