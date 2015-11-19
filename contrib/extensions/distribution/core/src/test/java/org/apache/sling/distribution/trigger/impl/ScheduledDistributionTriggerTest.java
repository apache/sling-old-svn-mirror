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
package org.apache.sling.distribution.trigger.impl;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ScheduledDistributionTrigger}
 */
public class ScheduledDistributionTriggerTest {

    @Test
    public void testRegister() throws Exception {
        for (DistributionRequestType action : DistributionRequestType.values()) {
            String path = "/path/to/somewhere";
            int interval = 10;
            DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
            Scheduler scheduler = mock(Scheduler.class);
            ScheduleOptions options = mock(ScheduleOptions.class);
            when(scheduler.NOW(-1, interval)).thenReturn(options);
            when(options.name(handler.toString())).thenReturn(options);
            ScheduledDistributionTrigger scheduleddistributionTrigger = new ScheduledDistributionTrigger(action.name(), path, interval, null, scheduler, mock(ResourceResolverFactory.class));
            scheduleddistributionTrigger.register(handler);
        }
    }

    @Test
    public void testUnregister() throws Exception {
        for (DistributionRequestType action : DistributionRequestType.values()) {
            String path = "/path/to/somewhere";
            int interval = 10;
            Scheduler scheduler = mock(Scheduler.class);
            ScheduledDistributionTrigger scheduleddistributionTrigger = new ScheduledDistributionTrigger(action.name(), path, interval, null, scheduler, mock(ResourceResolverFactory.class));
            DistributionRequestHandler handlerId = mock(DistributionRequestHandler.class);
            scheduleddistributionTrigger.unregister(handlerId);
        }
    }
}