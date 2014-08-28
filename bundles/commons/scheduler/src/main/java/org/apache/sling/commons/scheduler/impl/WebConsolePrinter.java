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
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;
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
@Component
@Service(value=WebConsolePrinter.class)
@Properties({
    @Property(name=Constants.SERVICE_DESCRIPTION,
              value="Apache Sling Scheduler Configuration Printer"),
    @Property(name="felix.webconsole.label", value="slingscheduler"),
    @Property(name="felix.webconsole.title", value="Sling Scheduler"),
    @Property(name="felix.webconsole.configprinter.modes", value="always")
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
        final Scheduler s = this.scheduler.getScheduler();
        if ( s != null ) {
            pw.println("Status : active");
            try {
                pw.print  ("Name   : ");
                pw.println(s.getSchedulerName());
                pw.print  ("Id     : ");
                pw.println(s.getSchedulerInstanceId());
                final List<String> groups = s.getJobGroupNames();
                for(final String group : groups) {
                    final Set<JobKey> keys = s.getJobKeys(GroupMatcher.jobGroupEquals(group));
                    for(final JobKey key : keys) {
                        final JobDetail detail = s.getJobDetail(key);
                        final String jobName = (String) detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_NAME);
                        final Object job = detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_OBJECT);
                        // only print jobs started through the sling scheduler
                        if ( jobName != null && job != null ) {
                            pw.print("Job : ");
                            pw.print(detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_NAME));
                            if ( detail.getDescription() != null && detail.getDescription().length() > 0 ) {
                                pw.print(" (");
                                pw.print(detail.getDescription());
                                pw.print(")");
                            }
                            pw.print(", class: ");
                            pw.print(job.getClass().getName());
                            pw.print(", concurrent: ");
                            pw.print(!detail.isConcurrentExectionDisallowed());
                            final String[] runOn = (String[])detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_RUN_ON);
                            if ( runOn != null ) {
                                pw.print(", runOn: ");
                                pw.print(Arrays.toString(runOn));
                            }
                            final Long bundleId = (Long)detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_BUNDLE_ID);
                            if ( bundleId != null ) {
                                pw.print(", bundleId: ");
                                pw.print(String.valueOf(bundleId));
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
        } else {
            pw.println("Status : not active");
        }
    }
}
