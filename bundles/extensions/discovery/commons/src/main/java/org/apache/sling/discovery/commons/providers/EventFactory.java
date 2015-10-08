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
package org.apache.sling.discovery.commons.providers;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;

/** Factory for creating TopologyEvents with BaseTopologyView **/
public class EventFactory {

    /** Simple factory method for creating a TOPOLOGY_INIT event with the given newView **/
    public static TopologyEvent newInitEvent(final BaseTopologyView newView) {
        if (newView==null) {
            throw new IllegalStateException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            throw new IllegalStateException("newView must be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_INIT, null, newView);
    }

    /** Simple factory method for creating a TOPOLOGY_CHANGING event with the given oldView **/
    public static TopologyEvent newChangingEvent(final BaseTopologyView oldView) {
        if (oldView==null) {
            throw new IllegalStateException("oldView must not be null");
        }
        if (oldView.isCurrent()) {
            throw new IllegalStateException("oldView must not be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView, null);
    }

    /** Simple factory method for creating a TOPOLOGY_CHANGED event with the given old and new views **/
    public static TopologyEvent newChangedEvent(final BaseTopologyView oldView, final BaseTopologyView newView) {
        if (oldView==null) {
            throw new IllegalStateException("oldView must not be null");
        }
        if (oldView.isCurrent()) {
            throw new IllegalStateException("oldView must not be current");
        }
        if (newView==null) {
            throw new IllegalStateException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            throw new IllegalStateException("newView must be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_CHANGED, oldView, newView);
    }

    public static TopologyEvent newPropertiesChangedEvent(final BaseTopologyView oldView, final BaseTopologyView newView) {
        if (oldView==null) {
            throw new IllegalStateException("oldView must not be null");
        }
        if (oldView.isCurrent()) {
            throw new IllegalStateException("oldView must not be current");
        }
        if (newView==null) {
            throw new IllegalStateException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            throw new IllegalStateException("newView must be current");
        }
        return new TopologyEvent(Type.PROPERTIES_CHANGED, oldView, newView);
    }

}
