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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.annotations.SlingHealthCheck;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses ThreadMXBean to show (and potentially WARN about) information about thread usage. Use deadlock detection provided by ThreadMXBean to send CRITICAL. 
 */
@SlingHealthCheck(name = ThreadUsageHealthCheck.HC_NAME, label = "Apache Sling "
        + ThreadUsageHealthCheck.HC_NAME, description = "Checks time threads are using ThreadMXBean", tags = { "resources",
                "threads" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ThreadUsageHealthCheck implements HealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadUsageHealthCheck.class);

    public static final String HC_NAME = "Thread Usage Health Check";

    private static final long DEFAULT_SAMPLE_PERIOD_IN_MS = 200;

    static final String PROP_SAMPLE_PERIOD_IN_MS = "samplePeriodInMs";
    @Property(name = PROP_SAMPLE_PERIOD_IN_MS, label = "Sample Period", description = "Period this HC uses to measure", longValue = DEFAULT_SAMPLE_PERIOD_IN_MS)
    private long samplePeriodInMs;

    static final String PROP_CPU_TIME_THRESHOLD_WARN = "cpuTimeThresholdWarn";
    @Property(name = PROP_CPU_TIME_THRESHOLD_WARN, label = "CPU Time Threshold for WARN in %", description = "Will WARN once this threshold is reached. Has to be configured according to cores of the system (e.g. use 400% for 4 cores)")
    private long cpuTimeThresholdWarn;
    
    private int availableProcessors;
    private boolean cpuTimeThresholdWarnIsConfigured;

    @Activate
    protected final void activate(final Map<String, Object> properties) {
        this.samplePeriodInMs = PropertiesUtil.toLong(properties.get(PROP_SAMPLE_PERIOD_IN_MS), DEFAULT_SAMPLE_PERIOD_IN_MS);

        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        long defaultThresholdWarn = availableProcessors * 90; // 100% is rarely reached, 90% means a very busy system
        this.cpuTimeThresholdWarn = PropertiesUtil.toLong(properties.get(PROP_CPU_TIME_THRESHOLD_WARN), defaultThresholdWarn);
        this.cpuTimeThresholdWarnIsConfigured = properties.get(PROP_CPU_TIME_THRESHOLD_WARN) !=null;

    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        log.debug("Checking threads for exceeding {}% CPU time within time period of {}ms", cpuTimeThresholdWarn, samplePeriodInMs);
        
        try {
            ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

            List<ThreadTimeInfo> threadTimeInfos = collectThreadTimeInfos(log, threadMxBean);

            Collections.sort(threadTimeInfos);

            float totalCpuTimePercentage = 0;
            for (int i = 0; i < threadTimeInfos.size(); i++) {

                ThreadTimeInfo threadInfo = threadTimeInfos.get(i);
                float cpuTimePercentage = ((float) threadInfo.getCpuTime() / ((float) samplePeriodInMs * 1000000)) * 100f;
                totalCpuTimePercentage +=cpuTimePercentage;

                String msg = String.format("%4.1f", cpuTimePercentage) + "% used by thread \"" + threadInfo.name + "\"";
                if (i<3  || (i<10 && cpuTimePercentage > 15)) {
                    log.info(msg);
                } else {
                    log.debug(msg);
                }
            }
            log.info(threadTimeInfos.size() + " threads using "+String.format("%4.1f", totalCpuTimePercentage)+"% CPU Time");
            boolean isHighCpuTime = totalCpuTimePercentage > cpuTimeThresholdWarn;
            if(isHighCpuTime) {
                log.warn("Threads are consuming significant CPU time "+  ( cpuTimeThresholdWarnIsConfigured ? 
                        "(more than configured " + cpuTimeThresholdWarn + "% within " + samplePeriodInMs + "ms)" 
                        : "(more than "+availableProcessors+" cores * 90% = "+cpuTimeThresholdWarn + "%)"));
            }

            checkForDeadlock(log, threadMxBean);

        } catch (Exception e) {
            LOG.error("Could not analyse thread usage "+e, e);
            log.healthCheckError("Could not analyse thread usage", e);
        }
      
        return new Result(log);

    }

    List<ThreadTimeInfo> collectThreadTimeInfos(FormattingResultLog log, ThreadMXBean threadMxBean) {
        
        
        Map<Long, ThreadTimeInfo> threadTimeInfos = new HashMap<Long, ThreadTimeInfo>();

        long[] allThreadIds = threadMxBean.getAllThreadIds();
        for (long threadId : allThreadIds) {
            ThreadTimeInfo threadTimeInfo = new ThreadTimeInfo();
            long threadCpuTimeStart = threadMxBean.getThreadCpuTime(threadId);
            threadTimeInfo.start = threadCpuTimeStart;
            ThreadInfo threadInfo = threadMxBean.getThreadInfo(threadId);
            threadTimeInfo.name = threadInfo!=null?threadInfo.getThreadName():"Thread id "+threadId+" (name not resolvable)";
            threadTimeInfos.put(threadId, threadTimeInfo);
        }

        try {
            Thread.sleep(samplePeriodInMs);
        } catch (InterruptedException e) {
            log.warn("Could not sleep configured samplePeriodInMs={} to gather thread load", samplePeriodInMs);
        }

        for (long threadId : allThreadIds) {
            ThreadTimeInfo threadTimeInfo = threadTimeInfos.get(threadId);
            if (threadTimeInfo == null) {
                continue;
            }
            long threadCpuTimeStop = threadMxBean.getThreadCpuTime(threadId);
            threadTimeInfo.stop = threadCpuTimeStop;
        }
        
        List<ThreadTimeInfo> threads = new ArrayList<ThreadTimeInfo>(threadTimeInfos.values());

        return threads;
    }
    

    void checkForDeadlock(FormattingResultLog log, ThreadMXBean threadMxBean) {
        long[] findDeadlockedThreads = threadMxBean.findDeadlockedThreads();
        if (findDeadlockedThreads != null) {
            for (long threadId : findDeadlockedThreads) {
                log.critical("Thread " + threadMxBean.getThreadInfo(threadId).getThreadName() + " is DEADLOCKED");
            }
        }
    }

    static class ThreadTimeInfo implements Comparable<ThreadTimeInfo> {
        long start;
        long stop;
        String name;

        long getCpuTime() {
            long cpuTime = stop - start;
            if(cpuTime < 0) {
                cpuTime = 0;
            }
            return cpuTime;
        }

        @Override
        public int compareTo(ThreadTimeInfo otherThreadTimeInfo) {
            if (otherThreadTimeInfo == null) {
                return -1;
            }
            return (int) (otherThreadTimeInfo.getCpuTime() - this.getCpuTime());
        }

    }

}