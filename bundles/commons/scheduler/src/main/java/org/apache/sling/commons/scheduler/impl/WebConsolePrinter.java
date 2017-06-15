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
package org.apache.sling.commons.scheduler.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * This is a configuration printer for the web console which
 * prints out the current configuration/status.
 *
 */
@Component(
        service = WebConsolePrinter.class,
        property = {
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
                Constants.SERVICE_DESCRIPTION + "=Apache Sling Scheduler Configuration Printer",
                "felix.webconsole.label=slingscheduler",
                "felix.webconsole.title=Sling Scheduler",
                "felix.webconsole.configprinter.modes=always"
        })
public class WebConsolePrinter {

    private static String HEADLINE = "Apache Sling Scheduler";

    @Reference
    private QuartzScheduler scheduler;

    public static final class JobInfo {
        public String name;
        public String className;
        public String description;
        public String reason;
        public boolean concurrent;
        public String runOn;
        public String[] triggers;
        public Long bundleId;
        public Long serviceId;
    }
    /**
     * Print out the configuration
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println(HEADLINE);
        pw.println();
        final Map<String, SchedulerProxy> proxies = this.scheduler.getSchedulers();
        if ( !proxies.isEmpty() ) {
            pw.println("Status : active");
            pw.println("Discovery : " + (QuartzJobExecutor.DISCOVERY_AVAILABLE.get() ? "available" : "not available"));
            for(final Map.Entry<String, SchedulerProxy> entry : proxies.entrySet()) {
                final Scheduler s = entry.getValue().getScheduler();
                try {
                    pw.print  ("Name      : ");
                    pw.println(s.getSchedulerName());
                    pw.print  ("ThreadPool: ");
                    pw.println(entry.getKey());
                    pw.print  ("Id        : ");
                    pw.println(s.getSchedulerInstanceId());
                    pw.println();
                    final List<JobInfo> activeJobs = new ArrayList<>();
                    final List<JobInfo> disabledJobs = new ArrayList<>();
                    for(final String group : s.getJobGroupNames()) {
                        final Set<JobKey> keys = s.getJobKeys(GroupMatcher.jobGroupEquals(group));
                        for(final JobKey key : keys) {
                            final JobDetail detail = s.getJobDetail(key);
                            final QuartzJobExecutor.JobDesc desc = new QuartzJobExecutor.JobDesc(detail.getJobDataMap());
                            // only print jobs started through the sling scheduler
                            if ( desc.isKnownJob() ) {
                                final JobInfo info = new JobInfo();
                                info.name = desc.name;
                                info.className = desc.job.getClass().getName();
                                info.concurrent = !detail.isConcurrentExectionDisallowed();
                                // check run on information
                                if ( desc.runOn != null ) {
                                    if ( desc.isRunOnLeader() ) {
                                        info.runOn = "LEADER";
                                    } else if ( desc.isRunOnSingle() ) {
                                        info.runOn = "SINGLE";
                                    } else {
                                        info.runOn = Arrays.toString(desc.runOn);
                                    }
                                    if ( desc.isRunOnLeader() || desc.isRunOnSingle() ) {
                                        if ( QuartzJobExecutor.DISCOVERY_AVAILABLE.get() ) {
                                            if ( QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get() ) {
                                                if ( desc.isRunOnLeader() || QuartzJobExecutor.FORCE_LEADER.get() ) {
                                                    if ( !QuartzJobExecutor.IS_LEADER.get() ) {
                                                        info.reason = "not leader";
                                                    }
                                                } else {
                                                    final String id = desc.shouldRunAsSingleOn();
                                                    if ( id != null ) {
                                                        info.reason = "single distributed elsewhere " + id;
                                                    }
                                                }
                                            } else {
                                                info.reason = "no discovery info";
                                            }
                                        } else {
                                            info.reason = "no discovery";
                                        }
                                    } else { // sling IDs
                                        final String myId = QuartzJobExecutor.SLING_ID;
                                        if ( myId == null ) {
                                            info.reason = "no Sling settings";
                                        } else {
                                            boolean schedule = false;
                                            for(final String id : desc.runOn ) {
                                                if ( myId.equals(id) ) {
                                                    schedule = true;
                                                    break;
                                                }
                                            }
                                            if ( !schedule ) {
                                                info.reason = "Sling ID";
                                            }
                                        }
                                    }
                                }
                                info.bundleId = (Long)detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_BUNDLE_ID);
                                info.serviceId = (Long)detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_SERVICE_ID);
                                int index = 0;
                                final List<? extends Trigger> triggers = s.getTriggersOfJob(key);
                                info.triggers = new String[triggers.size()];
                                for(final Trigger trigger : triggers) {
                                    info.triggers[index] = trigger.toString();
                                    index++;
                                }

                                if ( info.reason != null ) {
                                    disabledJobs.add(info);
                                } else {
                                    activeJobs.add(info);
                                }
                            }
                        }
                    }
                    if ( !activeJobs.isEmpty() ) {
                        pw.println();
                        pw.println("Active Jobs");
                        pw.println("-----------");
                        for(final JobInfo info : activeJobs) {
                            print(pw, info);
                        }
                    }
                    if ( !disabledJobs.isEmpty() ) {
                        pw.println();
                        pw.println("Inactive Jobs");
                        pw.println("-------------");
                        for(final JobInfo info : disabledJobs) {
                            print(pw, info);
                        }
                    }
                } catch ( final SchedulerException se ) {
                    pw.print  ("Unable to print complete configuration: ");
                    pw.println(se.getMessage());
                }
                pw.println();
            }
        } else {
            pw.println("Status : not active");
        }
        pw.println();
    }

    private void print(final PrintWriter pw, final JobInfo info) {
        pw.print("Job : ");
        pw.print(info.name);
        if ( info.description != null ) {
            pw.print(" (");
            pw.print(info.description);
            pw.print(")");
        }
        pw.print(", class: ");
        pw.print(info.className);
        pw.print(", concurrent: ");
        pw.print(info.concurrent);
        if ( info.runOn != null ) {
            pw.print(", runOn: ");
            pw.print(info.runOn);
        }
        if ( info.bundleId != null ) {
            pw.print(", bundleId: ");
            pw.print(String.valueOf(info.bundleId));
        }
        if ( info.serviceId != null ) {
            pw.print(", serviceId: ");
            pw.print(String.valueOf(info.serviceId));
        }
        pw.println();
        if ( info.reason != null ) {
            pw.print("Reason: ");
            pw.println(info.reason);
        }
        for(final String trigger : info.triggers) {
            pw.print("Trigger : ");
            pw.print(trigger);
            pw.println();
        }
        pw.println();
    }
}
