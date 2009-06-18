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
package org.apache.sling.event;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;
import org.osgi.service.event.Event;

/**
 * Tests for the EventUtil utility methods.
 */
public class EventUtilTest {

    @Test public void testDistributeFlag() {
        final Event distributableEvent = EventUtil.createDistributableEvent("some/topic", null);
        assertTrue(EventUtil.shouldDistribute(distributableEvent));
        final Event nonDistributableEvent = new Event("another/topic", null);
        assertFalse(EventUtil.shouldDistribute(nonDistributableEvent));
    }

    @Test public void testLocalFlag() {
        final Event localEvent = new Event("local/event", null);
        assertTrue(EventUtil.isLocal(localEvent));
        final Properties props = new Properties();
        props.put(EventUtil.PROPERTY_APPLICATION, "application1");
        final Event remoteEvent = new Event("remote/event", props);
        assertFalse(EventUtil.isLocal(remoteEvent));
    }
}
