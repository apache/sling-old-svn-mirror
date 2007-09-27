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
package org.apache.sling.event.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.sling.event.EventUtil;
import org.apache.sling.scheduler.Job;
import org.apache.sling.scheduler.JobContext;
import org.apache.sling.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * An event handler for timed events.
 *
 * @scr.component inherit="true"
 * @scr.property name="event.topics" value="org/apache/sling/event/timed"
 * @scr.property name="repository.path" value="/sling/timed-events"
 */
public class TimedEventHandler
    extends AbstractRepositoryEventHandler
    implements Job {

    /** @scr.reference */
    protected Scheduler scheduler;

    /** @scr.reference */
    protected EventAdmin eventAdmin;

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#activate(org.osgi.service.component.ComponentContext)
     */
    protected void activate(ComponentContext context)
    throws RepositoryException {
        super.activate(context);
        // load timed events from repository
        //this.loadEvents();
    }

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startSession() throws RepositoryException {
        super.startSession();
        this.session.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.PROPERTY_CHANGED, this.repositoryPath, true, null, null, true);
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#cleanUpRepository()
     */
    protected void cleanUpRepository() {
        // nothing to do right now
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#runInBackground()
     */
    protected void runInBackground() {
        while ( this.running ) {
            // so let's wait/get the next info from the queue
            EventInfo info = null;
            try {
                info = this.queue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( info != null && this.running ) {
                try {
                    this.session.refresh(true);
                    final Node eventNode = (Node) this.session.getItem(info.nodePath);
                    if ( !eventNode.isLocked() ) {
                        // lock node
                        Lock lock = null;
                        try {
                            lock = eventNode.lock(false, true);
                        } catch (RepositoryException re) {
                            // lock failed which means that the node is locked by someone else, so we don't have to requeue
                        }
                        if ( lock != null ) {
                            this.processEvent(info.event);
                        }
                    }
                } catch (RepositoryException e) {
                    // ignore
                    this.ignoreException(e);
                }
            }
        }
    }

    protected boolean processEvent(Event event) {
        if ( this.scheduler != null ) {
            final Map<String, Serializable> config = new HashMap<String, Serializable>();
            try {
                final Hashtable properties = new Hashtable();
                config.put("topic", (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
                final String[] names = event.getPropertyNames();
                if ( names != null ) {
                    for(int i=0; i<names.length; i++) {
                        properties.put(names[i], event.getProperty(names[i]));
                    }
                }
                config.put("config", properties);
                // if the event contains a job id we'll use that as the name
                String jobName = null;
                if ( event.getProperty(EventUtil.PROPERTY_JOB_ID) != null ) {
                    jobName = "Timed job " + event.getProperty(EventUtil.PROPERTY_JOB_ID);
                }
                // first, check for expression
                final String expression = (String) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_SCHEDULE);
                if ( expression != null ) {
                    this.scheduler.addJob(jobName, this, config, expression, false);
                } else {
                    // check for period next
                    final Long period = (Long) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_PERIOD);
                    if ( period != null ) {
                        this.scheduler.addPeriodicJob(jobName, this, config, period, false);
                    } else {
                        // then we check for a fixed date
                        final Date date = (Date) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_DATE);
                        if ( date != null ) {
                            this.scheduler.fireJobAt(jobName, this, config, date);
                        } else {
                            // no information, so fire the job once now
                            this.scheduler.fireJob(this, config);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                this.ignoreException(e);
            }
        } else {
            this.logger.error("No scheduler available to start timed event " + event);
        }
        return false;
    }

    public void onEvent(EventIterator events) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        this.processEvent(event);

    }

    /**
     * @see org.apache.sling.scheduler.Job#execute(org.apache.sling.scheduler.JobContext)
     */
    public void execute(JobContext context) {
        final String topic = (String) context.getConfiguration().get("topic");
        final Dictionary properties = (Dictionary) context.getConfiguration().get("config");
        if ( this.eventAdmin != null ) {
            this.eventAdmin.postEvent(new Event(topic, properties));
        } else {
            this.logger.warn("Unable to send timed event as no event admin service is available.");
        }
    }

    /**
     * Load all active timed events from the repository.
     * @throws RepositoryException
     */
    protected void loadEvents() throws RepositoryException {
        final QueryManager qManager = this.session.getWorkspace().getQueryManager();
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(this.getEventNodeType());
        buffer.append(")");
        final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
        final NodeIterator result = q.execute().getNodes();
        while ( result.hasNext() ) {
            final Node eventNode = result.nextNode();
            if ( !eventNode.isLocked() ) {
                final Event event = this.readEvent(eventNode);
                final EventInfo info = new EventInfo();
                info.event = event;
                info.nodePath = eventNode.getPath();
                try {
                    this.queue.put(info);
                } catch (InterruptedException e) {
                    // we ignore this exception as this should never occur
                    this.ignoreException(e);
                }
            }
        }
    }

}
