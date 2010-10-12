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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.DefaultJobManager;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.Statistics;

/**
 * This is a webconsole plugin displaying the active queues, some statistics
 * and the configurations.
 * @since 3.0
 */
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="felix.webconsole.label", value="slingevent", propertyPrivate=true),
    @Property(name="felix.webconsole.title", value="Sling Eventing", propertyPrivate=true),
    @Property(name="felix.webconsole.configprinter.modes", value={"zip", "txt"}, propertyPrivate=true)
})
public class WebConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = -6983227434841706385L;

    @Reference
    private JobManager jobManager;

    @Reference
    private QueueConfigurationManager queueConfigManager;

    /** Escape the output for html. */
    private String escape(final String text) {
        if ( text == null ) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Queue getQueue(final HttpServletRequest req) throws ServletException {
        final String name = req.getParameter("queue");
        if ( name != null ) {
            for(final Queue q : this.jobManager.getQueues()) {
                if ( name.equals(q.getName()) ) {
                    return q;
                }
            }
        }
        throw new ServletException("Wrong parameters");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
    throws ServletException, IOException {
        final String cmd = req.getParameter("action");
        if ( "suspend".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            q.suspend();
        } else if ( "resume".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            q.resume();
        } else if ( "clear".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            q.clear();
        } else if ( "dropall".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            q.removeAll();
        } else {
            throw new ServletException("Unknown command");
        }
        resp.sendRedirect(req.getContextPath() + req.getServletPath() + req.getPathInfo());
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
     throws ServletException, IOException {
        final PrintWriter pw = res.getWriter();

        pw.println("<p class='statline ui-state-highlight'>Apache Sling Eventing</p>");

        pw.println("<table class='nicetable'><tbody>");
        Statistics s = this.jobManager.getStatistics();
        pw.println("<tr><th colspan='2'>Overall Statistics</th></tr>");
        pw.printf("<tr><td>Start Time</td><td>%s</td></tr>", formatDate(s.getStartTime()));
        pw.printf("<tr><td>Last Activated</td><td>%s</td></tr>", formatDate(s.getLastActivatedJobTime()));
        pw.printf("<tr><td>Last Finished</td><td>%s</td></tr>", formatDate(s.getLastFinishedJobTime()));
        pw.printf("<tr><td>Queued Jobs</td><td>%s</td></tr>", s.getNumberOfQueuedJobs());
        pw.printf("<tr><td>Active Jobs</td><td>%s</td></tr>", s.getNumberOfActiveJobs());
        pw.printf("<tr><td>Jobs</td><td>%s</td></tr>", s.getNumberOfJobs());
        pw.printf("<tr><td>Finished Jobs</td><td>%s</td></tr>", s.getNumberOfFinishedJobs());
        pw.printf("<tr><td>Failed Jobs</td><td>%s</td></tr>", s.getNumberOfFailedJobs());
        pw.printf("<tr><td>Cancelled Jobs</td><td>%s</td></tr>", s.getNumberOfCancelledJobs());
        pw.printf("<tr><td>Processed Jobs</td><td>%s</td></tr>", s.getNumberOfProcessedJobs());
        pw.printf("<tr><td>Average Processing Time</td><td>%s</td></tr>", formatTime(s.getAverageProcessingTime()));
        pw.printf("<tr><td>Average Waiting Time</td><td>%s</td></tr>", formatTime(s.getAverageWaitingTime()));
        pw.println("</tbody></table>");
        pw.println("<br/>");

        boolean isEmpty = true;
        for(final Queue q : this.jobManager.getQueues()) {
            isEmpty = false;
            pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
            pw.printf("<span style='float: left; margin-left: 1em'>Active JobQueue: %s %s</span>", escape(q.getName()),
                    q.isSuspended() ? "(SUSPENDED)" : "");
            if ( q.isSuspended() ) {
                this.printForm(pw, q, "Resume", "resume");
            } else {
                this.printForm(pw, q, "Suspend", "suspend");
            }
            this.printForm(pw, q, "Clear", "clear");
            this.printForm(pw, q, "Drop All", "dropall");
            pw.println("</div>");
            pw.println("<table class='nicetable'><tbody>");

            s = q.getStatistics();
            final QueueConfiguration c = q.getConfiguration();
            pw.println("<tr><th colspan='2'>Statistics</th><th colspan='2'>Configuration</th></tr>");
            pw.printf("<tr><td>Start Time</td><td>%s</td><td>Type</td><td>%s</td></tr>", formatDate(s.getStartTime()), c.getType());
            pw.printf("<tr><td>Last Activated</td><td>%s</td><td>Topics</td><td>%s</td></tr>", formatDate(s.getLastActivatedJobTime()), formatArray(c.getTopics()));
            pw.printf("<tr><td>Last Finished</td><td>%s</td><td>Max Parallel</td><td>%s</td></tr>", formatDate(s.getLastFinishedJobTime()), c.getMaxParallel());
            pw.printf("<tr><td>Queued Jobs</td><td>%s</td><td>Max Retries</td><td>%s</td></tr>", s.getNumberOfQueuedJobs(), c.getMaxRetries());
            pw.printf("<tr><td>Active Jobs</td><td>%s</td><td>Retry Delay</td><td>%s ms</td></tr>", s.getNumberOfActiveJobs(), c.getRetryDelayInMs());
            pw.printf("<tr><td>Jobs</td><td>%s</td><td>Priority</td><td>%s</td></tr>", s.getNumberOfJobs(), c.getPriority());
            pw.printf("<tr><td>Finished Jobs</td><td>%s</td><td>Run Local</td><td>%s</td></tr>", s.getNumberOfFinishedJobs(), c.isLocalQueue());
            pw.printf("<tr><td>Failed Jobs</td><td>%s</td><td>App Ids</td><td>%s</td></tr>", s.getNumberOfFailedJobs(), formatArray(c.getApplicationIds()));
            pw.printf("<tr><td>Cancelled Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfCancelledJobs());
            pw.printf("<tr><td>Processed Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfProcessedJobs());
            pw.printf("<tr><td>Average Processing Time</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", formatTime(s.getAverageProcessingTime()));
            pw.printf("<tr><td>Average Waiting Time</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", formatTime(s.getAverageWaitingTime()));
            pw.printf("<tr><td>Status Info</td><td>%s</td></tr>", escape(q.getStatusInfo()));
            pw.println("</tbody></table>");
            pw.println("<br/>");
        }
        if ( isEmpty ) {
            pw.println("<p>No active queues.</p>");
            pw.println("<br/>");
        }

        pw.println("<p class='statline'>Apache Sling Eventing - Job Queue Configurations</p>");
        this.printQueueConfiguration(req, pw, ((DefaultJobManager)this.jobManager).getMainQueueConfiguration());
        final InternalQueueConfiguration[] configs = this.queueConfigManager.getConfigurations();
        for(final InternalQueueConfiguration c : configs ) {
            this.printQueueConfiguration(req, pw, c);
        }
    }

    private void printQueueConfiguration(final HttpServletRequest req, final PrintWriter pw, final InternalQueueConfiguration c) {
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
        pw.printf("<span style='float: left; margin-left: 1em'>Job Queue Configuration: %s</span>%n",
                escape(c.getName()));
        pw.printf("<button id='edit' class='ui-state-default ui-corner-all' onclick='javascript:window.location=\"%s%s/configMgr/%s\";'>Edit</button>",
                req.getContextPath(), req.getServletPath(), c.getPid());
        pw.println("</div>");
        pw.println("<table class='nicetable'><tbody>");
        pw.println("<tr><th colspan='2'>Configuration</th></tr>");
        pw.printf("<tr><td>Valid</td><td>%s</td></tr>", c.isValid());
        pw.printf("<tr><td>Type</td><td>%s</td></tr>", c.getType());
        pw.printf("<tr><td>Topics</td><td>%s</td></tr>", formatArray(c.getTopics()));
        pw.printf("<tr><td>Max Parallel</td><td>%s</td></tr>", c.getMaxParallel());
        pw.printf("<tr><td>Max Retries</td><td>%s</td></tr>", c.getMaxRetries());
        pw.printf("<tr><td>Retry Delay</td><td>%s ms</td></tr>", c.getRetryDelayInMs());
        pw.printf("<tr><td>Priority</td><td>%s ms</td></tr>", c.getPriority());
        pw.printf("<tr><td>Run Local</td><td>%s ms</td></tr>", c.isLocalQueue());
        pw.printf("<tr><td>App Ids</td><td>%s ms</td></tr>", formatArray(c.getApplicationIds()));
        pw.printf("<tr><td>Ranking</td><td>%s ms</td></tr>", c.getRanking());

        pw.println("</tbody></table>");
        pw.println("<br/>");
    }

    /**
     * Format an array for html rendering.
     */
    private String formatArray(final String[] array) {
        if ( array == null || array.length == 0 ) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(final String s : array ) {
            if ( !first ) {
                sb.append('\n');
            }
            first = false;
            sb.append(s);
        }
        return escape(sb.toString());
    }

    /**
     * Format an array.
     */
    private String formatArrayAsText(final String[] array) {
        if ( array == null || array.length == 0 ) {
            return "";
        }
        return Arrays.toString(array);
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

    private void printForm(final PrintWriter pw,
            final Queue q,
            final String buttonLabel,
            final String hiddenValue) {
        pw.printf("<form method='POST' name='%s'><input type='hidden' name='action' value='%s'/>"+
                "<input type='hidden' name='queue' value='%s'/>" +
                "<button class='ui-state-default ui-corner-all' onclick='javascript:document.forms[\"%s\"].submit();'>" +
                "%s</button></form>", hiddenValue, hiddenValue, q.getName(), hiddenValue, buttonLabel);
    }

    /** Configuration printer for the web console. */
    public void printConfiguration(final PrintWriter pw, final String mode) {
        if ( !"zip".equals(mode) && !"txt".equals(mode) ) {
            return;
        }
        pw.println("Apache Sling Eventing");
        pw.println("---------------------");

        Statistics s = this.jobManager.getStatistics();
        pw.println("Overall Statistics");
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
            pw.printf("Status Info : %s%n", q.getStatusInfo());
            pw.println("Configuration");
            pw.printf("Type : %s%n", c.getType());
            pw.printf("Topics : %s%n", formatArrayAsText(c.getTopics()));
            pw.printf("Max Parallel : %s%n", c.getMaxParallel());
            pw.printf("Max Retries : %s%n", c.getMaxRetries());
            pw.printf("Retry Delay : %s ms%n", c.getRetryDelayInMs());
            pw.printf("Priority : %s%n", c.getPriority());
            pw.printf("Run Local : %s%n", c.isLocalQueue());
            pw.printf("App Ids : %s%n", formatArrayAsText(c.getApplicationIds()));
            pw.println();
        }
        if ( isEmpty ) {
            pw.println("No active queues.");
            pw.println();
        }

        pw.println("Apache Sling Eventing - Job Queue Configurations");
        pw.println("------------------------------------------------");
        this.printQueueConfiguration(pw, ((DefaultJobManager)this.jobManager).getMainQueueConfiguration());
        final InternalQueueConfiguration[] configs = this.queueConfigManager.getConfigurations();
        for(final InternalQueueConfiguration c : configs ) {
            this.printQueueConfiguration(pw, c);
        }

    }

    private void printQueueConfiguration(final PrintWriter pw, final InternalQueueConfiguration c) {
        pw.printf("Job Queue Configuration: %s%n",
                c.getName());
        pw.printf("Valid : %s%n", c.isValid());
        pw.printf("Type : %s%n", c.getType());
        pw.printf("Topics : %s%n", formatArrayAsText(c.getTopics()));
        pw.printf("Max Parallel : %s%n", c.getMaxParallel());
        pw.printf("Max Retries : %s%n", c.getMaxRetries());
        pw.printf("Retry Delay : %s ms%n", c.getRetryDelayInMs());
        pw.printf("Priority : %s ms%n", c.getPriority());
        pw.printf("Run Local : %s ms%n", c.isLocalQueue());
        pw.printf("App Ids : %s ms%n", formatArrayAsText(c.getApplicationIds()));
        pw.printf("Ranking : %s ms%n", c.getRanking());

        pw.println();
    }
}
