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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
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
public class WebConsolePrinter {

    public static ServiceRegistration initPlugin(final BundleContext bundleContext,
                                                 final QuartzScheduler qs) {
        final WebConsolePrinter propertiesPrinter = new WebConsolePrinter(qs);
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Scheduler Configuration Printer");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put("felix.webconsole.label", "slingscheduler");
        props.put("felix.webconsole.title", "Sling Scheduler");
        props.put("felix.webconsole.configprinter.modes", "always");

        return bundleContext.registerService(WebConsolePrinter.class.getName(),
                                               propertiesPrinter, props);
    }

    public static void destroyPlugin(final ServiceRegistration plugin) {
        if ( plugin != null) {
            plugin.unregister();
        }
    }

    private static String HEADLINE = "Apache Sling Scheduler";

    private final QuartzScheduler scheduler;

    public WebConsolePrinter(final QuartzScheduler qs) {
        this.scheduler = qs;
    }

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
