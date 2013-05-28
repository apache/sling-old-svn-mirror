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
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a web console plugin displaying the active queues, some statistics
 * and the configurations.
 * @since 3.0
 */
@Component
@Service(value={javax.servlet.Servlet.class, EventHandler.class})
@Properties({
    @Property(name="felix.webconsole.label", value="slingevent", propertyPrivate=true),
    @Property(name="felix.webconsole.title", value="Sling Jobs", propertyPrivate=true),
    @Property(name="felix.webconsole.configprinter.modes", value={"zip", "txt"}, propertyPrivate=true),
    @Property(name="event.topics",propertyPrivate=true,
              value={"sling/webconsole/test"})
})
public class WebConsolePlugin extends HttpServlet implements EventHandler {

    private static final String SLING_WEBCONSOLE_TEST_JOB_TOPIC = "sling/webconsole/test";

    private static final long serialVersionUID = -6983227434841706385L;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private JobManager jobManager;

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private QueueConfigurationManager queueConfigManager;

    @Reference
    private JobConsumerManager jobConsumerManager;

    /** Escape the output for HTML. */
    private String escape(final String text) {
        if ( text == null ) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final String PAR_QUEUE = "queue";

    private Queue getQueue(final HttpServletRequest req) {
        final String name = req.getParameter(PAR_QUEUE);
        if ( name != null ) {
            for(final Queue q : this.jobManager.getQueues()) {
                if ( name.equals(q.getName()) ) {
                    return q;
                }
            }
        }
        return null;
    }

    private String getQueueErrorMessage(final HttpServletRequest req, final String command) {
        final String name = req.getParameter(PAR_QUEUE);
        if ( name == null || name.length() == 0 ) {
            return "Queue parameter missing for opertation " + command;
        }
        return "Queue with name '" + name + "' not found for operation " + command;
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
    throws ServletException, IOException {
        String msg = null;
        final String cmd = req.getParameter("action");
        if ( "suspend".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            if ( q != null ) {
                q.suspend();
            } else {
                msg = this.getQueueErrorMessage(req, "suspend");
            }
        } else if ( "resume".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            if ( q != null ) {
                q.resume();
            } else {
                msg = this.getQueueErrorMessage(req, "resume");
            }
        } else if ( "clear".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            if ( q != null ) {
                q.clear();
            } else {
                msg = this.getQueueErrorMessage(req, "clear");
            }
        } else if ( "reset".equals(cmd) ) {
            if ( req.getParameter(PAR_QUEUE) == null || req.getParameter(PAR_QUEUE).length() == 0 ) {
                this.jobManager.getStatistics().reset();
            } else {
                final Queue q = this.getQueue(req);
                if ( q != null ) {
                    q.getStatistics().reset();
                } else {
                    msg = this.getQueueErrorMessage(req, "reset");
                }
            }
        } else if ( "test".equals(cmd) ) {
            logger.info("Posting test event.");
            eventAdmin.postEvent(getTestEvent(null));
        } else if ( "restart".equals(cmd) ) {
            this.jobManager.restart();
        } else if ( "dropall".equals(cmd) ) {
            final Queue q = this.getQueue(req);
            if ( q != null ) {
                q.removeAll();
            } else {
                msg = this.getQueueErrorMessage(req, "drop all");
            }
        } else {
            msg = "Unknown command";
        }
        final String path = req.getContextPath() + req.getServletPath() + req.getPathInfo();
        final String redirectTo;
        if ( msg == null ) {
            redirectTo = path;
        } else {
            redirectTo = path + "?message=" + msg;
        }
        resp.sendRedirect(redirectTo);
    }

    private Event getTestEvent(final String id) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobUtil.PROPERTY_JOB_TOPIC, SLING_WEBCONSOLE_TEST_JOB_TOPIC);
        if ( id != null ) {
            props.put(JobUtil.PROPERTY_JOB_NAME, id);
        }

        return new Event(JobUtil.TOPIC_JOB, props);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
     throws ServletException, IOException {
        final String msg = req.getParameter("message");
        final PrintWriter pw = res.getWriter();

        pw.println("<form method='POST' name='eventingcmd'>" +
        		     "<input type='hidden' name='action' value=''/>"+
                     "<input type='hidden' name='queue' value=''/>" +
                   "</form>");
        pw.println("<script type='text/javascript'>");
        pw.println("function eventingsubmit(action, queue) {" +
                   " if ( action == 'restart' ) {" +
                   "   if ( !confirm('Do you really want to restart the job handling?') ) { return; }" +
                   " }" +
                   " document.forms['eventingcmd'].action.value = action;" +
                   " document.forms['eventingcmd'].queue.value = queue;" +
                   " document.forms['eventingcmd'].submit();" +
                   "} </script>");

        pw.printf("<p class='statline ui-state-highlight'>Apache Sling Job Handling%s%n</p>",
                msg != null ? " : " + msg : "");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
        pw.println("<span style='float: left; margin-left: 1em'>Apache Sling Job Handling: Overall Statistics</span>");
        this.printForm(pw, null, "Restart!", "restart");
        this.printForm(pw, null, "Reset Stats", "reset");
        pw.println("</div>");

        pw.println("<table class='nicetable'><tbody>");
        String topics = this.jobConsumerManager.getTopics();
        if ( topics == null ) {
            topics = "";
        }
        Statistics s = this.jobManager.getStatistics();
        pw.printf("<tr><td>Start Time</td><td>%s</td></tr>", formatDate(s.getStartTime()));
        pw.printf("<tr><td>Local topic consumers: </td><td>%s</td></tr>", topics);
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
            String queueName = q.getName();
            pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
            pw.printf("<span style='float: left; margin-left: 1em'>Active JobQueue: %s %s</span>", escape(q.getName()),
                    q.isSuspended() ? "(SUSPENDED)" : "");
            this.printForm(pw, queueName, "Reset Stats", "reset");
            if ( q.isSuspended() ) {
                this.printForm(pw, queueName, "Resume", "resume");
            } else {
                this.printForm(pw, queueName, "Suspend", "suspend");
            }
            this.printForm(pw, queueName, "Test", "test");
            this.printForm(pw, queueName, "Clear Queue", "clear");
            this.printForm(pw, queueName, "Drop All", "dropall");
            pw.println("</div>");
            pw.println("<table class='nicetable'><tbody>");

            s = q.getStatistics();
            final QueueConfiguration c = q.getConfiguration();
            pw.println("<tr><th colspan='2'>Statistics</th><th colspan='2'>Configuration</th></tr>");
            pw.printf("<tr><td>Start Time</td><td>%s</td><td>Type</td><td>%s</td></tr>", formatDate(s.getStartTime()), formatType(c.getType()));
            pw.printf("<tr><td>Last Activated</td><td>%s</td><td>Topics</td><td>%s</td></tr>", formatDate(s.getLastActivatedJobTime()), formatArray(c.getTopics()));
            pw.printf("<tr><td>Last Finished</td><td>%s</td><td>Max Parallel</td><td>%s</td></tr>", formatDate(s.getLastFinishedJobTime()), c.getMaxParallel());
            pw.printf("<tr><td>Queued Jobs</td><td>%s</td><td>Max Retries</td><td>%s</td></tr>", s.getNumberOfQueuedJobs(), c.getMaxRetries());
            pw.printf("<tr><td>Active Jobs</td><td>%s</td><td>Retry Delay</td><td>%s ms</td></tr>", s.getNumberOfActiveJobs(), c.getRetryDelayInMs());
            pw.printf("<tr><td>Jobs</td><td>%s</td><td>Priority</td><td>%s</td></tr>", s.getNumberOfJobs(), c.getPriority());
            pw.printf("<tr><td>Finished Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfFinishedJobs());
            pw.printf("<tr><td>Failed Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfFailedJobs());
            pw.printf("<tr><td>Cancelled Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfCancelledJobs());
            pw.printf("<tr><td>Processed Jobs</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", s.getNumberOfProcessedJobs());
            pw.printf("<tr><td>Average Processing Time</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", formatTime(s.getAverageProcessingTime()));
            pw.printf("<tr><td>Average Waiting Time</td><td>%s</td><td colspan='2'>&nbsp</td></tr>", formatTime(s.getAverageWaitingTime()));
            pw.printf("<tr><td>Status Info</td><td colspan='3'>%s</td></tr>", escape(q.getStateInfo()));
            pw.println("</tbody></table>");
            pw.println("<br/>");
        }
        if ( isEmpty ) {
            pw.println("<p>No active queues.</p>");
            pw.println("<br/>");
        }

        for(final TopicStatistics ts : this.jobManager.getTopicStatistics()) {
            pw.println("<table class='nicetable'><tbody>");
            pw.printf("<tr><th colspan='2'>Topic Statistics: %s</th></tr>", escape(ts.getTopic()));

            pw.printf("<tr><td>Last Activated</td><td>%s</td></tr>", formatDate(ts.getLastActivatedJobTime()));
            pw.printf("<tr><td>Last Finished</td><td>%s</td></tr>", formatDate(ts.getLastFinishedJobTime()));
            pw.printf("<tr><td>Finished Jobs</td><td>%s</td></tr>", ts.getNumberOfFinishedJobs());
            pw.printf("<tr><td>Failed Jobs</td><td>%s</td></tr>", ts.getNumberOfFailedJobs());
            pw.printf("<tr><td>Cancelled Jobs</td><td>%s</td></tr>", ts.getNumberOfCancelledJobs());
            pw.printf("<tr><td>Processed Jobs</td><td>%s</td></tr>", ts.getNumberOfProcessedJobs());
            pw.printf("<tr><td>Average Processing Time</td><td>%s</td></tr>", formatTime(ts.getAverageProcessingTime()));
            pw.printf("<tr><td>Average Waiting Time</td><td>%s</td></tr>", formatTime(ts.getAverageWaitingTime()));
            pw.println("</tbody></table>");
            pw.println("<br/>");
        }

        pw.println("<p class='statline'>Apache Sling Job Handling - Job Queue Configurations</p>");
        this.printQueueConfiguration(req, pw, this.queueConfigManager.getMainQueueConfiguration());
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
        this.printForm(pw, c.getName(), "Test", "test");

        pw.println("</div>");
        pw.println("<table class='nicetable'><tbody>");
        pw.println("<tr><th colspan='2'>Configuration</th></tr>");
        pw.printf("<tr><td>Valid</td><td>%s</td></tr>", c.isValid());
        pw.printf("<tr><td>Type</td><td>%s</td></tr>", formatType(c.getType()));
        pw.printf("<tr><td>Topics</td><td>%s</td></tr>", formatArray(c.getTopics()));
        pw.printf("<tr><td>Max Parallel</td><td>%s</td></tr>", c.getMaxParallel());
        pw.printf("<tr><td>Max Retries</td><td>%s</td></tr>", c.getMaxRetries());
        pw.printf("<tr><td>Retry Delay</td><td>%s ms</td></tr>", c.getRetryDelayInMs());
        pw.printf("<tr><td>Priority</td><td>%s</td></tr>", c.getPriority());
        pw.printf("<tr><td>Ranking</td><td>%s</td></tr>", c.getRanking());

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

    private void printForm(final PrintWriter pw,
            final String qeueName,
            final String buttonLabel,
            final String cmd) {
        pw.printf("<button class='ui-state-default ui-corner-all' onclick='javascript:eventingsubmit(\"%s\", \"%s\");'>" +
                "%s</button>", cmd, (qeueName != null ? qeueName : ""), buttonLabel);
    }


    /** Configuration printer for the web console. */
    public void printConfiguration(final PrintWriter pw, final String mode) {
        if ( !"zip".equals(mode) && !"txt".equals(mode) ) {
            return;
        }
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
            pw.printf("Priority : %s%n", c.getPriority());
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

    @Override
    public void handleEvent(final Event event) {
        if ( SLING_WEBCONSOLE_TEST_JOB_TOPIC.equals(event.getTopic()) ) {
            logger.info("Received test event.");

            JobUtil.acknowledgeJob(event);
            JobUtil.finishedJob(event);
        }
    }
}
