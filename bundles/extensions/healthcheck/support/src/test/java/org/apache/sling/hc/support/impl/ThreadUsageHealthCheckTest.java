/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.support.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.osgi.service.component.ComponentContext;

import org.apache.sling.hc.support.impl.ThreadUsageHealthCheck.ThreadTimeInfo;

public class ThreadUsageHealthCheckTest {

    @Spy
    ThreadUsageHealthCheck threadsHealthCheck;

    @Mock
    ComponentContext componentContext;

    @Mock
    Dictionary<String, Object> componentConfig;

    @Before
    public void setup() {
        initMocks(this);

        doReturn(componentConfig).when(componentContext).getProperties();
        doNothing().when(threadsHealthCheck).checkForDeadlock(any(FormattingResultLog.class), any(ThreadMXBean.class));

    }

    @Test
    public void testThreadActivityWithinThreshold() {

        long samplePeriod = 200L;
        int processorsAvailable = 4;
        int busyThreads = 3;

        List<ThreadTimeInfo> resultListOverloaded = getFullLoadThreadInfos(samplePeriod, busyThreads);

        setupExpectations(samplePeriod, processorsAvailable, resultListOverloaded);

        Result result = threadsHealthCheck.execute();

        assertEquals(Result.Status.OK, result.getStatus());

    }

    @Test
    public void testThreadActivityOverloaded() {

        long samplePeriod = 200L;
        int processorsAvailable = 4;
        int busyThreads = 5;

        List<ThreadTimeInfo> resultListOverloaded = getFullLoadThreadInfos(samplePeriod, busyThreads);

        setupExpectations(samplePeriod, processorsAvailable, resultListOverloaded);

        Result result = threadsHealthCheck.execute();

        assertEquals(Result.Status.WARN, result.getStatus());

    }

    private List<ThreadTimeInfo> getFullLoadThreadInfos(long samplePeriod, int count) {
        List<ThreadTimeInfo> resultList = new ArrayList<ThreadTimeInfo>();
        for (int i = 0; i < count; i++) {
            resultList.add(getThreadTimeInfo((long) (samplePeriod * .95)));
        }
        return resultList;
    }

    private void setupExpectations(long samplePeriod, int processorsAvailable, List<ThreadTimeInfo> resultListOk) {
        doReturn(processorsAvailable * 90).when(componentConfig).get(ThreadUsageHealthCheck.PROP_CPU_TIME_THRESHOLD_WARN);
        doReturn(samplePeriod).when(componentConfig).get(ThreadUsageHealthCheck.PROP_SAMPLE_PERIOD_IN_MS);
        threadsHealthCheck.activate(componentContext);
        doReturn(resultListOk).when(threadsHealthCheck).collectThreadTimeInfos(any(FormattingResultLog.class), any(ThreadMXBean.class));
    }

    private ThreadUsageHealthCheck.ThreadTimeInfo getThreadTimeInfo(long diffInMs) {
        ThreadUsageHealthCheck.ThreadTimeInfo threadTimeInfo = new ThreadUsageHealthCheck.ThreadTimeInfo();
        threadTimeInfo.start = new Date().getTime();
        threadTimeInfo.stop = threadTimeInfo.start + (diffInMs * 1000000);
        threadTimeInfo.name = "Unit Test Thread";
        return threadTimeInfo;
    }

}
