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
package org.apache.sling.discovery.commons.providers.impl;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;

/** SLING-4755 : encapsulates an event that yet has to be sent (asynchronously) for a particular listener **/
final class AsyncEvent {
    final TopologyEventListener listener;
    final TopologyEvent event;
    AsyncEvent(TopologyEventListener listener, TopologyEvent event) {
        if (listener==null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (event==null) {
            throw new IllegalArgumentException("event must not be null");
        }
        this.listener = listener;
        this.event = event;
    }
    @Override
    public String toString() {
        return "an AsyncEvent[event="+event+", listener="+listener+"]";
    }
}