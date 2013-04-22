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

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;

public class AssertingTopologyEventListener implements TopologyEventListener {
    private final List<TopologyEventAsserter> expectedEvents = new LinkedList<TopologyEventAsserter>();

    public AssertingTopologyEventListener() {
    }

    private List<TopologyEvent> events_ = new LinkedList<TopologyEvent>();

    public void handleTopologyEvent(TopologyEvent event) {
        TopologyEventAsserter asserter = null;
        synchronized (expectedEvents) {
            if (expectedEvents.size() == 0) {
                throw new IllegalStateException(
                        "no expected events anymore. But got: " + event);
            }
            asserter = expectedEvents.remove(0);
        }
        if (asserter == null) {
            throw new IllegalStateException("this should not occur");
        }
        asserter.assertOk(event);
        events_.add(event);
    }

    public List<TopologyEvent> getEvents() {
        return events_;
    }

    public void addExpected(Type expectedType) {
        addExpected(new AcceptsParticularTopologyEvent(expectedType));
    }

    public void addExpected(TopologyEventAsserter topologyEventAsserter) {
        expectedEvents.add(topologyEventAsserter);
    }

    public int getRemainingExpectedCount() {
        return expectedEvents.size();
    }
}