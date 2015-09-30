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

import java.util.List;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;

/**
 * Listener for resource change events.
 *
 * <p>
 * {@code ResourceChangeListener} objects are registered with the Framework service
 * registry and are notified with {@code ResourceChange} objects when a
 * change occurs.
 * <p>
 * {@code ResourceChangeListener} objects can inspect the received {@code ResourceChange} objects to
 * determine the type of change, location and other properties.
 *
 * <p>
 * {@code ResourceChangeListener} objects must be registered with a service property
 * {@link ResourceChangeListener#PATHS} whose value is the list of resource paths for which
 * the listener is interested in getting change events.
 *
 * <p>
 * By default a resource listener gets only local events which means events
 * caused by changes persisted on the same instance as this listener is registered.
 * If the resource listener is interested in external events, the implementation
 * should implement the {@link ExternalResourceListener} interface, but still register
 * the service as a {@code ResourceChangeListener} service.
 */
@ConsumerType
public interface ResourceChangeListener {

    /**
     * Array of paths - required.
     * A path is either absolute or relative. If it's a relative path, the relative path
     * will be appended to all search paths of the resource resolver. If the whole tree
     * of all search paths should be observed, the special value {@code .} should be used.
     * If one of the paths is a sub resource of another specified path,
     * the sub path is ignored.
     * If this property is missing or invalid, the listener is ignored. The type of the
     * property must either be String, or a String array.
     */
    String PATHS = "resource.paths";

    /**
     * Array of change types - optional.
     * If this property is missing, added, removed and changed events are reported.
     * If this property is invalid, the listener is ignored. The type of the property
     * must either be String, or a String array. Valid values are the constants from
     * {@link ResourceChange.ChangeType}.
     */
    String CHANGES = "resource.change.types";

    /**
     * Report a resource change based on the filter properties of this listener.
     * @param changes The changes.
     */
    void onChange(@Nonnull List<ResourceChange> changes);
}
