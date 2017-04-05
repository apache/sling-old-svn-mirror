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

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A resource change event is immutable.
 *
 * A change event can either be local or external. Local changes happened
 * on the same instance, while external changes happened on a different
 * instance.
 *
 * Resource listeners only receive external changes if they mark themselves
 * as a {@link ExternalResourceChangeListener}.
 *
 * For all events (local and external), the path and the type of change is
 * set.
 *
 * Resource provider events are always local events and only provide the path.
 *
 * Local events for resources provide the names of the properties that
 * have been added, removed or changed. This information might be missing
 * for external events.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ConsumerType
public class ResourceChange {

    /**
     * The type of the change
     */
    public enum ChangeType {
        ADDED,            // the resource has been added
        REMOVED,          // the resource has been removed
        CHANGED,          // the resource has been changed
        PROVIDER_ADDED,   // a provider has been added
        PROVIDER_REMOVED  // a provider has been removed
    }

    /** The resource path. */
    private final String path;

    /** The resource change. */
    private final ChangeType changeType;

    /** Flag whether the change is external. */
    private final boolean isExternal;

    /** Optional set of added property names. */
    private final Set<String> addedPropertyNames;

    /** Optional set of changed property names. */
    private final Set<String> changedPropertyNames;

    /** Optional set of removed property names. */
    private final Set<String> removedPropertyNames;

    /**
     * Create a new change object
     *
     * @param changeType The change type
     * @param path The resource path
     * @param isExternal {code true} if the change happened on another node
     * @since 1.2.0 (Sling API Bundle 2.15.0)
     */
    public ResourceChange(final @Nonnull ChangeType changeType,
            final @Nonnull String path,
            final boolean isExternal) {
        this.path = path;
        this.changeType = changeType;
        this.isExternal = isExternal;
        this.addedPropertyNames = null;
        this.changedPropertyNames = null;
        this.removedPropertyNames = null;
    }

    /**
     * Create a new change object
     *
     * @param changeType The change type
     * @param path The resource path
     * @param isExternal {code true} if the change happened on another node
     * @param addedPropertyNames set of added property names, if provided must be immutable
     * @param changedPropertyNames set of added property names, if provided must be immutable
     * @param removedPropertyNames set of added property names, if provided must be immutable
     * @deprecated The sets of property names are not supported anymore.
     */
    @Deprecated
    public ResourceChange(final @Nonnull ChangeType changeType,
            final @Nonnull String path,
            final boolean isExternal,
            final Set<String> addedPropertyNames,
            final Set<String> changedPropertyNames,
            final Set<String> removedPropertyNames) {
        this.path = path;
        this.changeType = changeType;
        this.isExternal = isExternal;
        this.addedPropertyNames = addedPropertyNames;
        this.changedPropertyNames = changedPropertyNames;
        this.removedPropertyNames = removedPropertyNames;
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
     * The application code can not rely on getting the correct set of changed
     * properties. A resource provider implementation is free to not support
     * this. Therefore if this method returns {@code null} it does not mean
     * that there are no changed properties. However if an empty set is
     * returned, it can safely be assumed that there are none. Therefore
     * returning {code null} is the equivalent of "don't know".
     * @return The set of changed property names. For external events or
     *         resource provider events {@code null} is returned.
     * @deprecated As there is no guarantee that this information is contained in the change
     *             event, this should not be used anymore.
     */
    @Deprecated
    public @CheckForNull Set<String> getChangedPropertyNames() {
        return this.changedPropertyNames;
    }

    /**
     * Optional information about added properties.
     * The application code can not rely on getting the correct set of added
     * properties. A resource provider implementation is free to not support
     * this. Therefore if this method returns {@code null} it does not mean
     * that there are no added properties. However if an empty set is
     * returned, it can safely be assumed that there are none. Therefore
     * returning {code null} is the equivalent of "don't know".
     * @return The set of changed property names. For external events or
     *         resource provider events {@code null} is returned.
     * @deprecated As there is no guarantee that this information is contained in the change
     *             event, this should not be used anymore.
     */
    @Deprecated
    public @CheckForNull Set<String> getAddedPropertyNames() {
        return this.addedPropertyNames;
    }

    /**
     * Optional information about removed properties.
     * The application code can not rely on getting the correct set of removed
     * properties. A resource provider implementation is free to not support
     * this. Therefore if this method returns {@code null} it does not mean
     * that there are no removed properties. However if an empty set is
     * returned, it can safely be assumed that there are none. Therefore
     * returning {code null} is the equivalent of "don't know".
     * @return The set of changed property names. For external events or
     *         resource provider events {@code null} is returned.
     * @deprecated As there is no guarantee that this information is contained in the change
     *             event, this should not be used anymore.
     */
    @Deprecated
    public @CheckForNull Set<String> getRemovedPropertyNames() {
        return this.removedPropertyNames;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("ResourceChange[type=")
          .append(this.getType())
          .append(", path=")
          .append(this.getPath())
          .append(", external=")
          .append(this.isExternal)
          .append("]");
        return b.toString();
    }
}
