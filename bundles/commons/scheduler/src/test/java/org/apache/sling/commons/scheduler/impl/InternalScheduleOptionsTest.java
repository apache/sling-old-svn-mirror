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
        TriggerBuilder<? extends Trigger> trigger;

        trigger = TriggerBuilder.newTrigger();
        test = new InternalScheduleOptions(trigger);

        assertEquals(trigger, test.trigger);
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

        Map<String, Serializable> testMap = new HashMap<String, Serializable>();
        test.config(testMap);
        assertEquals(testMap, test.configuration);

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
