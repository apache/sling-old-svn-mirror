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
                    final List<String> groups = s.getJobGroupNames();
                    for(final String group : groups) {
                        final Set<JobKey> keys = s.getJobKeys(GroupMatcher.jobGroupEquals(group));
                        for(final JobKey key : keys) {
                            final JobDetail detail = s.getJobDetail(key);
                            final QuartzJobExecutor.JobDesc desc = new QuartzJobExecutor.JobDesc(detail.getJobDataMap());
                            // only print jobs started through the sling scheduler
                            if ( desc.isKnownJob() ) {
                                pw.print("Job : ");
                                pw.print(desc.name);
                                if ( detail.getDescription() != null && detail.getDescription().length() > 0 ) {
                                    pw.print(" (");
                                    pw.print(detail.getDescription());
                                    pw.print(")");
                                }
                                pw.print(", class: ");
                                pw.print(desc.job.getClass().getName());
                                pw.print(", concurrent: ");
                                pw.print(!detail.isConcurrentExectionDisallowed());
                                if ( desc.runOn != null ) {
                                    pw.print(", runOn: ");
                                    pw.print(Arrays.toString(desc.runOn));
                                    // check run on information
                                    if ( desc.isRunOnLeader() || desc.isRunOnSingle() ) {
                                        if ( QuartzJobExecutor.DISCOVERY_AVAILABLE.get() ) {
                                            if ( QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get() ) {
                                                if ( desc.isRunOnLeader() ) {
                                                    if ( !QuartzJobExecutor.IS_LEADER.get() ) {
                                                        pw.print(" (inactive: not leader)");
                                                    }
                                                } else {
                                                    final String id = desc.shouldRunAsSingleOn();
                                                    if ( id != null ) {
                                                        pw.print(" (inactive: single distributed elsewhere ");
                                                        pw.print(id);
                                                        pw.print(")");
                                                    }
                                                }
                                            } else {
                                                pw.print(" (inactive: no discovery info)");
                                            }
                                        } else {
                                            pw.print(" (inactive: no discovery)");
                                        }
                                    } else { // sling IDs
                                        final String myId = QuartzJobExecutor.SLING_ID;
                                        if ( myId == null ) {
                                            pw.print(" (inactive: no Sling settings)");
                                        } else {
                                            boolean schedule = false;
                                            for(final String id : desc.runOn ) {
                                                if ( myId.equals(id) ) {
                                                    schedule = true;
                                                    break;
                                                }
                                            }
                                            if ( !schedule ) {
                                                pw.print(" (inactive: Sling ID)");
                                            }
                                        }
                                    }                            }
                                final Long bundleId = (Long)detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_BUNDLE_ID);
                                if ( bundleId != null ) {
                                    pw.print(", bundleId: ");
                                    pw.print(String.valueOf(bundleId));
                                }
                                final Long serviceId = (Long)detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_SERVICE_ID);
                                if ( serviceId != null ) {
                                    pw.print(", serviceId: ");
                                    pw.print(String.valueOf(serviceId));
                                }
                                pw.println();
                                for(final Trigger trigger : s.getTriggersOfJob(key)) {
                                    pw.print("Trigger : ");
                                    pw.print(trigger);
                                    pw.println();
                                }
                                pw.println();
                            }
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
}
