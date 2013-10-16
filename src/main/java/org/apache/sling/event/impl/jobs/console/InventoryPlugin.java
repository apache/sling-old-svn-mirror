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
package org.apache.sling.event.impl.jobs.console;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.jobs.TopologyCapabilities;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;

/**
 * This is a inventory plugin displaying the active queues, some statistics
 * and the configurations.
 * @since 3.2
 */
@Component
@Service(value={InventoryPrinter.class})
@Properties({
    @Property(name=InventoryPrinter.NAME, value="slingjobs"),
    @Property(name=InventoryPrinter.TITLE, value="Sling Jobs"),
    @Property(name=InventoryPrinter.FORMAT, value={"TEXT", "JSON"}),
    @Property(name=InventoryPrinter.WEBCONSOLE, boolValue=false)
})
public class InventoryPlugin implements InventoryPrinter {

    @Reference
    private JobManager jobManager;

    @Reference
    private QueueConfigurationManager queueConfigManager;

    @Reference
    private JobConsumerManager jobConsumerManager;

    /**
     * Format an array.
     */
    private String formatArrayAsText(final String[] array) {
        if ( array == null || array.length == 0 ) {
            return "";
        }
        return Arrays.toString(array);
    }

    private String formatType(final QueueConfiguration.Type type) {
        switch ( type ) {
            case ORDERED : return "Ordered";
            case TOPIC_ROUND_ROBIN : return "Topic Round Robin";
            case UNORDERED : return "Parallel";
            case IGNORE : return "Ignore";
            case DROP : return "Drop";
        }
        return type.toString();
    }

    /** Default date format used. */
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS yyyy-MMM-dd");

    /**
     * Format a date
     */
    private synchronized String formatDate(final long time) {
        if ( time == -1 ) {
            return "-";
        }
        final Date d = new Date(time);
        return dateFormat.format(d);
    }

    /**
     * Format time (= duration)
     */
    private String formatTime(final long time) {
        if ( time == 0 ) {
            return "-";
        }
        if ( time < 1000 ) {
            return time + " ms";
        } else if ( time < 1000 * 60 ) {
            return time / 1000 + " secs";
        }
        final long min = time / 1000 / 60;
        final long secs = (time - min * 1000 * 60);
        return min + " min " + secs / 1000 + " secs";
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    @Override
    public void print(final PrintWriter pw, final Format format, final boolean isZip) {
        if ( format.equals(Format.TEXT) ) {
            printText(pw);
        } else if ( format.equals(Format.JSON) ) {
            printJson(pw);
        }
    }

    private void printText(final PrintWriter pw) {
        pw.println("Apache Sling Job Handling");
        pw.println("-------------------------");

        String topics = this.jobConsumerManager.getTopics();
        if ( topics == null ) {
            topics = "";
        }

        Statistics s = this.jobManager.getStatistics();
        pw.println("Overall Statistics");
        pw.printf("Start Time : %s%n", formatDate(s.getStartTime()));
        pw.printf("Local topic consumers: %s%n", topics);
        pw.printf("Last Activated : %s%n", formatDate(s.getLastActivatedJobTime()));
        pw.printf("Last Finished : %s%n", formatDate(s.getLastFinishedJobTime()));
        pw.printf("Queued Jobs : %s%n", s.getNumberOfQueuedJobs());
        pw.printf("Active Jobs : %s%n", s.getNumberOfActiveJobs());
        pw.printf("Jobs : %s%n", s.getNumberOfJobs());
        pw.printf("Finished Jobs : %s%n", s.getNumberOfFinishedJobs());
        pw.printf("Failed Jobs : %s%n", s.getNumberOfFailedJobs());
        pw.printf("Cancelled Jobs : %s%n", s.getNumberOfCancelledJobs());
        pw.printf("Processed Jobs : %s%n", s.getNumberOfProcessedJobs());
        pw.printf("Average Processing Time : %s%n", formatTime(s.getAverageProcessingTime()));
        pw.printf("Average Waiting Time : %s%n", formatTime(s.getAverageWaitingTime()));
        pw.println();

        pw.println("Topology Capabilities");
        final TopologyCapabilities cap = ((JobManagerImpl)this.jobManager).getTopologyCapabilities();
        if ( cap == null ) {
            pw.print("No topology information available !");
        } else {
            final Map<String, List<InstanceDescription>> instanceCaps = cap.getInstanceCapabilities();
            for(final Map.Entry<String, List<InstanceDescription>> entry : instanceCaps.entrySet()) {
                final StringBuilder sb = new StringBuilder();
                for(final InstanceDescription id : entry.getValue()) {
                    if ( sb.length() > 0 ) {
                        sb.append(", ");
                    }
                    if ( id.isLocal() ) {
                        sb.append("local");
                    } else {
                        sb.append(id.getSlingId());
                    }
                }
                pw.printf("%s : %s%n", entry.getKey(), sb.toString());
            }
        }
        pw.println();

        pw.println("Scheduled Jobs");
        pw.println("<table class='nicetable'><tbody>");
        final Collection<ScheduledJobInfo> infos = this.jobManager.getScheduledJobs();
        if ( infos.size() == 0 ) {
            pw.print("No jobs currently scheduled");
        } else {
            for(final ScheduledJobInfo info : infos) {
                pw.println("Schedule");
                pw.printf("Job Topic< : %s%n", info.getJobTopic());
                pw.print("Schedules : ");
                boolean first = true;
                for(final ScheduleInfo si : info.getSchedules() ) {
                    if ( !first ) {
                        pw.print(", ");
                    }
                    first = false;
                    switch ( si.getType() ) {
                    case YEARLY : pw.printf("YEARLY %s %s : %s:%s", si.getMonthOfYear(), si.getDayOfMonth(), si.getHourOfDay(), si.getMinuteOfHour());
                                  break;
                    case MONTHLY : pw.printf("MONTHLY %s : %s:%s", si.getDayOfMonth(), si.getHourOfDay(), si.getMinuteOfHour());
                                  break;
                    case WEEKLY : pw.printf("WEEKLY %s : %s:%s", si.getDayOfWeek(), si.getHourOfDay(), si.getMinuteOfHour());
                                  break;
                    case DAILY : pw.printf("DAILY %s:%s", si.getHourOfDay(), si.getMinuteOfHour());
                                 break;
                    case HOURLY : pw.printf("HOURLY %s", si.getMinuteOfHour());
                                 break;
                    case CRON : pw.printf("CRON %s", si.getExpression());
                                 break;
                    default : pw.printf("AT %s", si.getAt());
                    }
                }
                pw.println();
                pw.println();
            }
        }
        pw.println();

        boolean isEmpty = true;
        for(final Queue q : this.jobManager.getQueues()) {
            isEmpty = false;
            pw.printf("Active JobQueue: %s %s%n", q.getName(),
                    q.isSuspended() ? "(SUSPENDED)" : "");

            s = q.getStatistics();
            final QueueConfiguration c = q.getConfiguration();
            pw.println("Statistics");
            pw.printf("Start Time : %s%n", formatDate(s.getStartTime()));
            pw.printf("Last Activated : %s%n", formatDate(s.getLastActivatedJobTime()));
            pw.printf("Last Finished : %s%n", formatDate(s.getLastFinishedJobTime()));
            pw.printf("Queued Jobs : %s%n", s.getNumberOfQueuedJobs());
            pw.printf("Active Jobs : %s%n", s.getNumberOfActiveJobs());
            pw.printf("Jobs : %s%n", s.getNumberOfJobs());
            pw.printf("Finished Jobs : %s%n", s.getNumberOfFinishedJobs());
            pw.printf("Failed Jobs : %s%n", s.getNumberOfFailedJobs());
            pw.printf("Cancelled Jobs : %s%n", s.getNumberOfCancelledJobs());
            pw.printf("Processed Jobs : %s%n", s.getNumberOfProcessedJobs());
            pw.printf("Average Processing Time : %s%n", formatTime(s.getAverageProcessingTime()));
            pw.printf("Average Waiting Time : %s%n", formatTime(s.getAverageWaitingTime()));
            pw.printf("Status Info : %s%n", q.getStateInfo());
            pw.println("Configuration");
            pw.printf("Type : %s%n", formatType(c.getType()));
            pw.printf("Topics : %s%n", formatArrayAsText(c.getTopics()));
            pw.printf("Max Parallel : %s%n", c.getMaxParallel());
            pw.printf("Max Retries : %s%n", c.getMaxRetries());
            pw.printf("Retry Delay : %s ms%n", c.getRetryDelayInMs());
            pw.printf("Priority : %s%n", c.getThreadPriority());
            pw.println();
        }
        if ( isEmpty ) {
            pw.println("No active queues.");
            pw.println();
        }

        for(final TopicStatistics ts : this.jobManager.getTopicStatistics()) {
            pw.printf("Topic Statistics - %s%n", ts.getTopic());
            pw.printf("Last Activated : %s%n", formatDate(ts.getLastActivatedJobTime()));
            pw.printf("Last Finished : %s%n", formatDate(ts.getLastFinishedJobTime()));
            pw.printf("Finished Jobs : %s%n", ts.getNumberOfFinishedJobs());
            pw.printf("Failed Jobs : %s%n", ts.getNumberOfFailedJobs());
            pw.printf("Cancelled Jobs : %s%n", ts.getNumberOfCancelledJobs());
            pw.printf("Processed Jobs : %s%n", ts.getNumberOfProcessedJobs());
            pw.printf("Average Processing Time : %s%n", formatTime(ts.getAverageProcessingTime()));
            pw.printf("Average Waiting Time : %s%n", formatTime(ts.getAverageWaitingTime()));
            pw.println();
        }

        pw.println("Apache Sling Job Handling - Job Queue Configurations");
        pw.println("----------------------------------------------------");
        this.printQueueConfiguration(pw, this.queueConfigManager.getMainQueueConfiguration());
        final InternalQueueConfiguration[] configs = this.queueConfigManager.getConfigurations();
        for(final InternalQueueConfiguration c : configs ) {
            this.printQueueConfiguration(pw, c);
        }
    }

    private void printQueueConfiguration(final PrintWriter pw, final InternalQueueConfiguration c) {
        pw.printf("Job Queue Configuration: %s%n",
                c.getName());
        pw.printf("Valid : %s%n", c.isValid());
        pw.printf("Type : %s%n", formatType(c.getType()));
        pw.printf("Topics : %s%n", formatArrayAsText(c.getTopics()));
        pw.printf("Max Parallel : %s%n", c.getMaxParallel());
        pw.printf("Max Retries : %s%n", c.getMaxRetries());
        pw.printf("Retry Delay : %s ms%n", c.getRetryDelayInMs());
        pw.printf("Priority : %s%n", c.getPriority());
        pw.printf("Ranking : %s%n", c.getRanking());

        pw.println();
    }

    private void printJson(final PrintWriter pw) {
        pw.println("{");
        Statistics s = this.jobManager.getStatistics();
        pw.println("  \"statistics\" : {");
        pw.printf("    \"startTime\" : %s,%n", s.getStartTime());
        pw.printf("    \"startTimeText\" : \"%s\",%n", formatDate(s.getStartTime()));
        pw.printf("    \"lastActivatedJobTime\" : %s,%n", s.getLastActivatedJobTime());
        pw.printf("    \"lastActivatedJobTimeText\" : \"%s\",%n", formatDate(s.getLastActivatedJobTime()));
        pw.printf("    \"lastFinishedJobTime\" : %s,%n", s.getLastFinishedJobTime());
        pw.printf("    \"lastFinishedJobTimeText\" : \"%s\",%n", formatDate(s.getLastFinishedJobTime()));
        pw.printf("    \"numberOfQueuedJobs\" : %s,%n", s.getNumberOfQueuedJobs());
        pw.printf("    \"numberOfActiveJobs\" : %s,%n", s.getNumberOfActiveJobs());
        pw.printf("    \"numberOfJobs\" : %s,%n", s.getNumberOfJobs());
        pw.printf("    \"numberOfFinishedJobs\" : %s,%n", s.getNumberOfFinishedJobs());
        pw.printf("    \"numberOfFailedJobs\" : %s,%n", s.getNumberOfFailedJobs());
        pw.printf("    \"numberOfCancelledJobs\" : %s,%n", s.getNumberOfCancelledJobs());
        pw.printf("    \"numberOfProcessedJobs\" : %s,%n", s.getNumberOfProcessedJobs());
        pw.printf("    \"averageProcessingTime\" : %s,%n", s.getAverageProcessingTime());
        pw.printf("    \"averageProcessingTimeText\" : \"%s\",%n", formatTime(s.getAverageProcessingTime()));
        pw.printf("    \"averageWaitingTime\" : %s,%n", s.getAverageWaitingTime());
        pw.printf("    \"averageWaitingTimeText\" : \"%s\"%n", formatTime(s.getAverageWaitingTime()));
        pw.print("  }");

        final TopologyCapabilities cap = ((JobManagerImpl)this.jobManager).getTopologyCapabilities();
        if ( cap != null ) {
            pw.println(",");
            pw.println("  \"capabilities\" : [");
            final Map<String, List<InstanceDescription>> instanceCaps = cap.getInstanceCapabilities();
            final Iterator<Map.Entry<String, List<InstanceDescription>>> iter = instanceCaps.entrySet().iterator();
            while ( iter.hasNext() ) {
                final Map.Entry<String, List<InstanceDescription>> entry = iter.next();
                final List<String> instances = new ArrayList<String>();
                for(final InstanceDescription id : entry.getValue()) {
                    if ( id.isLocal() ) {
                        instances.add("local");
                    } else {
                        instances.add(id.getSlingId());
                    }
                }
                pw.println("    {");
                pw.printf("       \"topic\" : \"%s\",%n", entry.getKey());
                pw.printf("       \"instances\" : %s%n", formatArrayAsJson(instances.toArray(new String[instances.size()])));
                if ( iter.hasNext() ) {
                    pw.println("    },");
                } else {
                    pw.println("    }");
                }
            }
            pw.print("  ]");
        }

        boolean first = true;
        for(final Queue q : this.jobManager.getQueues()) {
            pw.println(",");
            if ( first ) {
                pw.println("  \"queues\" : [");
                first = false;
            }
            pw.println("    {");
            pw.printf("      \"name\" : \"%s\",%n", q.getName());
            pw.printf("      \"suspended\" : %s,%n", q.isSuspended());

            s = q.getStatistics();
            pw.println("      \"statistics\" : {");
            pw.printf("        \"startTime\" : %s,%n", s.getStartTime());
            pw.printf("        \"startTimeText\" : \"%s\",%n", formatDate(s.getStartTime()));
            pw.printf("        \"lastActivatedJobTime\" : %s,%n", s.getLastActivatedJobTime());
            pw.printf("        \"lastActivatedJobTimeText\" : \"%s\",%n", formatDate(s.getLastActivatedJobTime()));
            pw.printf("        \"lastFinishedJobTime\" : %s,%n", s.getLastFinishedJobTime());
            pw.printf("        \"lastFinishedJobTimeText\" : \"%s\",%n", formatDate(s.getLastFinishedJobTime()));
            pw.printf("        \"numberOfQueuedJobs\" : %s,%n", s.getNumberOfQueuedJobs());
            pw.printf("        \"numberOfActiveJobs\" : %s,%n", s.getNumberOfActiveJobs());
            pw.printf("        \"numberOfJobs\" : %s,%n", s.getNumberOfJobs());
            pw.printf("        \"numberOfFinishedJobs\" : %s,%n", s.getNumberOfFinishedJobs());
            pw.printf("        \"numberOfFailedJobs\" : %s,%n", s.getNumberOfFailedJobs());
            pw.printf("        \"numberOfCancelledJobs\" : %s,%n", s.getNumberOfCancelledJobs());
            pw.printf("        \"numberOfProcessedJobs\" : %s,%n", s.getNumberOfProcessedJobs());
            pw.printf("        \"averageProcessingTime\" : %s,%n", s.getAverageProcessingTime());
            pw.printf("        \"averageProcessingTimeText\" : \"%s\",%n", formatTime(s.getAverageProcessingTime()));
            pw.printf("        \"averageWaitingTime\" : %s,%n", s.getAverageWaitingTime());
            pw.printf("        \"averageWaitingTimeText\" : \"%s\"%n", formatTime(s.getAverageWaitingTime()));
            pw.print("      },");

            final QueueConfiguration c = q.getConfiguration();
            pw.printf("      \"stateInfo\" : \"%s\",%n", q.getStateInfo());
            pw.println("      \"configuration\" : {");
            pw.printf("        \"type\" : \"%s\",%n", c.getType());
            pw.printf("        \"topics\" : \"%s\",%n", formatArrayAsJson(c.getTopics()));
            pw.printf("        \"maxParallel\" : %s,%n", c.getMaxParallel());
            pw.printf("        \"maxRetries\" : %s,%n", c.getMaxRetries());
            pw.printf("        \"retryDelayInMs\" : %s,%n", c.getRetryDelayInMs());
            pw.printf("        \"priority\" : \"%s\"%n", c.getThreadPriority());
            pw.println("      }");
            pw.print("    }");
        }
        if ( !first ) {
            pw.print("  ]");
        }

        first = true;
        for(final TopicStatistics ts : this.jobManager.getTopicStatistics()) {
            pw.println(",");
            if ( first ) {
                pw.println("  \"topicStatistics\" : [");
                first = false;
            }
            pw.println("    {");
            pw.printf("      \"topic\" : \"%s\",%n", ts.getTopic());
            pw.printf("      \"lastActivatedJobTime\" : %s,%n", ts.getLastActivatedJobTime());
            pw.printf("      \"lastActivatedJobTimeText\" : \"%s\",%n", formatDate(ts.getLastActivatedJobTime()));
            pw.printf("      \"lastFinishedJobTime\" : %s,%n", ts.getLastFinishedJobTime());
            pw.printf("      \"lastFinishedJobTimeText\" : \"%s\",%n", formatDate(ts.getLastFinishedJobTime()));
            pw.printf("      \"numberOfFinishedJobs\" : %s,%n", ts.getNumberOfFinishedJobs());
            pw.printf("      \"numberOfFailedJobs\" : %s,%n", ts.getNumberOfFailedJobs());
            pw.printf("      \"numberOfCancelledJobs\" : %s,%n", ts.getNumberOfCancelledJobs());
            pw.printf("      \"numberOfProcessedJobs\" : %s,%n", ts.getNumberOfProcessedJobs());
            pw.printf("      \"averageProcessingTime\" : %s,%n", ts.getAverageProcessingTime());
            pw.printf("      \"averageProcessingTimeText\" : \"%s\",%n", formatTime(ts.getAverageProcessingTime()));
            pw.printf("      \"averageWaitingTime\" : %s,%n", ts.getAverageWaitingTime());
            pw.printf("      \"averageWaitingTimeText\" : \"%s\"%n", formatTime(ts.getAverageWaitingTime()));
            pw.print("    }");
        }
        if ( !first ) {
            pw.print("  ]");
        }

        pw.println(",");
        pw.println("  \"configurations\" : [");
        this.printQueueConfigurationJson(pw, this.queueConfigManager.getMainQueueConfiguration());
        final InternalQueueConfiguration[] configs = this.queueConfigManager.getConfigurations();
        for(final InternalQueueConfiguration c : configs ) {
            pw.println(",");
            this.printQueueConfigurationJson(pw, c);
        }
        pw.println();
        pw.println("  ]");
        pw.println("}");
    }

    private void printQueueConfigurationJson(final PrintWriter pw, final InternalQueueConfiguration c) {
        pw.println("    {");
        pw.printf("      \"name\" : \"%s\",%n", c.getName());
        pw.printf("      \"valid\" : %s,%n", c.isValid());
        pw.printf("      \"type\" : \"%s\",%n", c.getType());
        pw.printf("      \"topics\" : %s,%n", formatArrayAsJson(c.getTopics()));
        pw.printf("      \"maxParallel\" : %s,%n", c.getMaxParallel());
        pw.printf("      \"maxRetries\" : %s,%n", c.getMaxRetries());
        pw.printf("      \"retryDelayInMs\" : %s,%n", c.getRetryDelayInMs());
        pw.printf("      \"priority\" : \"%s\",%n", c.getPriority());
        pw.printf("      \"ranking\" : %s%n", c.getRanking());
        pw.print("    }");
    }

    /**
     * Format an array.
     */
    private String formatArrayAsJson(final String[] array) {
        if ( array == null || array.length == 0 ) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for(final String s : array) {
            if ( !first ) {
                sb.append(", ");
            }
            first = false;
            sb.append("\"");
            sb.append(s);
            sb.append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
