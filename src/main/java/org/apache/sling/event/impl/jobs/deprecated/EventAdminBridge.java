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
package org.apache.sling.event.impl.jobs.deprecated;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.EventPropertiesMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event admin bridge for compatibility.
 *
 * This event handler receives jobs from the event admin and adds them
 * to the job manager. It uses an async queue for adding.
 *
 * This handler is enabled by default, to disable it provide a configuration
 * which removes both topic properties (or sets them to null)
 *
 */
@Component(immediate=true)
@Service(value={EventHandler.class, JobConsumer.class})
@Properties({
    @Property(name=EventConstants.EVENT_TOPIC, value=JobUtil.TOPIC_JOB),
    @Property(name=JobConsumer.PROPERTY_TOPICS, value="/")
})
public class EventAdminBridge
    implements EventHandler, JobConsumer {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** A local queue for writing received events into the repository. */
    private final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /** Is the background task still running? */
    private volatile boolean running;

    @Reference
    private JobManager jobManager;

    /**
     * Activate this component and start background thread
     */
    @Activate
    protected void activate() {
        // start writer background thread
        this.running = true;
        final Thread writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                addJobs();
            }
        }, "Apache Sling Job Event Bridge");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        this.running = false;
        // stop write queue
        try {
            this.writeQueue.put(new Event("deactivate", (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
        }
        logger.info("Apache Sling Job Event Bridge stopped on instance {}", Environment.APPLICATION_ID);
    }


    /**
     * Background thread adding jobs received as events
     */
    private void addJobs() {
        logger.info("Apache Sling Job Event Bridge started on instance {}", Environment.APPLICATION_ID);
        try {
            this.processWriteQueue();
         } catch (final Throwable t) { //NOSONAR
             logger.error("Bridge thread stopped with exception: " + t.getMessage(), t);
             running = false;
         }
    }

    private static String[] IGNORED_CONFIG_PROPERTIES = new String[] {
        JobUtil.PROPERTY_JOB_PARALLEL,
        JobUtil.PROPERTY_JOB_RUN_LOCAL,
        JobUtil.PROPERTY_JOB_RETRIES,
        JobUtil.PROPERTY_JOB_QUEUE_NAME,
        JobUtil.PROPERTY_JOB_QUEUE_ORDERED,
        JobUtil.PROPERTY_JOB_PRIORITY
    };

    /**
     * The writer queue. One job is written on each run.
     */
    private void processWriteQueue() {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (final InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( event != null && this.running ) {
                final JobManager jm = this.jobManager;
                if ( jm == null ) {
                    try {
                        this.writeQueue.put(event);
                        Thread.sleep(500);
                    } catch (final InterruptedException ie) {
                        this.ignoreException(ie);
                    }
                } else {
                    final String jobTopic = (String)event.getProperty(ResourceHelper.PROPERTY_JOB_TOPIC);
                    final String jobName = (String)event.getProperty(ResourceHelper.PROPERTY_JOB_NAME);

                    final Map<String, Object> props =  new EventPropertiesMap(event);
                    props.put(JobImpl.PROPERTY_BRIDGED_EVENT, Boolean.TRUE);

                    // check for deprecated/unsupported properties
                    for(final String ignoredProp : IGNORED_CONFIG_PROPERTIES) {
                        if ( props.containsKey(ignoredProp)) {
                            logger.info("Job {} is using deprecated and ignored property {}", EventUtil.toString(event), ignoredProp);
                            props.remove(ignoredProp);
                        }
                    }
                    this.jobManager.addJob(jobTopic, jobName, props);
                }
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Receiving event {}", EventUtil.toString(event));
        }
        // we ignore remote job events
        if ( EventUtil.isLocal(event) ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Handling local job {}", EventUtil.toString(event));
            }
            // check job topic
            final String errorMessage = Utility.checkJobTopic(event.getProperty(ResourceHelper.PROPERTY_JOB_TOPIC));
            if ( errorMessage == null ) {
                try {
                    this.writeQueue.put(event);
                } catch (final InterruptedException e) {
                    // this should never happen
                    this.ignoreException(e);
                }
            } else {
                this.logger.warn(errorMessage + " : {}", EventUtil.toString(event));
            }
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    @Override
    public JobResult process(final Job job) {
        // this is never been called, but we return something anyway
        return JobResult.CANCEL;
    }
}
