/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.Test;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class InternalScheduleOptionsTest {

    private InternalScheduleOptions test;

    @Test
    public void testReferences() {
        TriggerBuilder<? extends Trigger> trigger = TriggerBuilder.newTrigger();
        test = new InternalScheduleOptions(trigger);

        assertTrue(trigger == test.trigger);
        assertNull("If trigger was set, argumentException must be null", test.argumentException);

        String[] onLeader = new String[]{Scheduler.VALUE_RUN_ON_LEADER};
        test.onLeaderOnly(true);
        assertArrayEquals(onLeader, test.runOn);
        test.onLeaderOnly(false);
        assertNull(test.runOn);

        String[] onSingle = new String[]{Scheduler.VALUE_RUN_ON_SINGLE};
        test.onSingleInstanceOnly(true);
        assertArrayEquals(onSingle, test.runOn);
        test.onSingleInstanceOnly(false);
        assertNull(test.runOn);

        Map<String, Serializable> config = new HashMap<String, Serializable>();
        test.config(config);
        assertEquals(config, test.configuration);

        String testName = "testName";
        test.name(testName);
        assertEquals(testName, test.name);

        test.canRunConcurrently(true);
        assertTrue(test.canRunConcurrently);

        String[] testStringArray = new String[]{"test1", "test2", "test3"};
        test.onInstancesOnly(testStringArray);
        assertArrayEquals(testStringArray, test.runOn);
    }

    @Test
    public void testIllegalStateConstructor() {
        IllegalArgumentException e = new IllegalArgumentException("Test message");
        test = new InternalScheduleOptions(e);
        assertEquals(e, test.argumentException);
        assertNull("If exception was set, trigger must be null", test.trigger);
    }
}
