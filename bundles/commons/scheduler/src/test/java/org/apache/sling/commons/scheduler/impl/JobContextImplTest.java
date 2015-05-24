package org.apache.sling.commons.scheduler.impl;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class JobContextImplTest {

    @Test
    public void testReferencies() {
        String testName = "testName";
        Map<String, Serializable> testMap = new HashMap<String, Serializable>();
        QuartzJobExecutor.JobContextImpl underTest = new QuartzJobExecutor.JobContextImpl(testName, testMap);

        assertTrue(underTest.getConfiguration().equals(testMap));
        assertTrue(underTest.getName().equals(testName));
    }
}
