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
package org.apache.sling.api.resource.observation;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;

/**
 * A resource change event is immutable.
 */
@ConsumerType
public abstract class ResourceChange {

    public enum ChangeType {
        ADDED,
        REMOVED,
        CHANGED,
        PROVIDER_ADDED,
        PROVIDER_REMOVED
    }

    private final String path;
    private final ChangeType changeType;
    private final boolean isExternal;

    /**
     * Create a new change object
     * @param changeType The change type
     * @param path The resource path
     */
    public ResourceChange(final @Nonnull ChangeType changeType,
            final @Nonnull String path,
            final boolean isExternal) {
        this.path = path;
        this.changeType = changeType;
        this.isExternal = isExternal;
    }

    /**
     * Get the resource path.
     * @return The path to the resource.
     */
    public @Nonnull String getPath() {
        return this.path;
    }

    /**
     * Get the user id of the user initiating the change
     * @return The user id or {@code null} if it's not available.
     */
    public @CheckForNull String getUserId() {
        return null;
    }

    /**
     * Is this an external event?
     * @return {@code true} if the event is external.
     */
    public boolean isExternal() {
        return this.isExternal;
    }

   /**
     * Get the type of change
     * @return The type of change
     */
    public @Nonnull ChangeType getType() {
        return this.changeType;
    }

    /**
     * Optional information about changed properties.
     * TODO Clarify when these might be available.
     */
    public @CheckForNull String[] getChangedAttributeNames() {
        return null;
    }

    /**
     * Optional information about added properties.
     * TODO Clarify when these might be available.
     */
    public @CheckForNull String[] getAddedAttributeNames() {
        return null;
    }

    /**
     * Optional information about removed properties.
     * TODO Clarify when these might be available.
     */
    public @CheckForNull String[] getRemovedAttributeNames() {
        return null;
    }
}
