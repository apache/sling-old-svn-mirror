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

/**
 * This is a group of resources all pointing to the same artifact,
 * but maybe in different versions or locations.
 */
public interface TaskResourceGroup {


    /**
     * Return the first resource if it either needs to be installed or uninstalled.
     */
    TaskResource getActiveResource();

    /**
     * If there is more than the active resource in the group, return the second
     * resource from the group.
     * @since 1.1
     */
    TaskResource getNextActiveResource();

    /**
     * Set the finish state for the active resource.
     * If this resource has been uninstalled, check the next in the list if it needs to
     * be reactivated.
     */
    void setFinishState(ResourceState state);

    /**
     * Set the finish state for the active resource and register an alias.
     * This method does the same as {@link #setFinishState(ResourceState)}
     * but in addition registers an alias id for the resource.
     *
     * @see #setFinishState(ResourceState)
     * @since 1.1
     */
    void setFinishState(ResourceState state, String alias);

    /**
     * Get the current alias for this group.
     * @return The alias or <code>null</code>
     * @since 1.1
     */
    String getAlias();
}
