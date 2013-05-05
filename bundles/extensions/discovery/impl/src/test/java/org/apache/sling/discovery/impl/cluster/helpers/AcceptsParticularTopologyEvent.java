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
package org.apache.sling.discovery.impl.cluster.helpers;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;

public class AcceptsParticularTopologyEvent implements TopologyEventAsserter {

    private final Type particularType;

    private int eventCnt = 0;

    /**
     * @param singleInstanceTest
     */
    public AcceptsParticularTopologyEvent(Type particularType) {
        this.particularType = particularType;
    }

    public void assertOk(TopologyEvent event) {
        if (event.getType() == particularType) {
            // fine
            eventCnt++;
        } else {
            throw new IllegalStateException("expected " + particularType
                    + ", got " + event.getType());
        }
    }

    public int getEventCnt() {
        return eventCnt;
    }
}