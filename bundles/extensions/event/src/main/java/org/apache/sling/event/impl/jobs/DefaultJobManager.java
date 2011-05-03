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
package org.apache.sling.event.impl.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.queues.AbstractJobQueue;
import org.apache.sling.event.impl.jobs.queues.OrderedJobQueue;
import org.apache.sling.event.impl.jobs.queues.ParallelJobQueue;
import org.apache.sling.event.impl.jobs.queues.TopicRoundRobinJobQueue;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.JobsIterator;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An event handler for special job events.
 *
 * We schedule this event handler to run in the background
 * and clean up obsolete queues.
 *
 */
@Component(label="%job.events.name",
        description="%job.events.description",
        metatype=true,immediate=true)
@Services({
    @Service(value=Runnable.class),
    @Service(value=JobManager.class),
    @Service(value=EventHandler.class)
})
@Properties({
    @Property(name="scheduler.period", longValue=60, propertyPrivate=true),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true),
    @Property(name=ConfigurationConstants.PROP_PRIORITY,
            value=ConfigurationConstants.DEFAULT_PRIORITY,
            options={@PropertyOption(name="NORM",value="Norm"),
            @PropertyOption(name="MIN",value="Min"),
            @PropertyOption(name="MAX",value="Max")}),
    @Property(name=ConfigurationConstants.PROP_RETRIES,
            intValue=ConfigurationConstants.DEFAULT_RETRIES),
    @Property(name=ConfigurationConstants.PROP_RETRY_DELAY,
            longValue=ConfigurationConstants.DEFAULT_RETRY_DELAY),
    @Property(name=ConfigurationConstants.PROP_MAX_PARALLEL,
            intValue=ConfigurationConstants.DEFAULT_MAX_PARALLEL),
    @Property(name="event.topics",propertyPrivate=true,
            value={"org/apache/sling/event/notification/job/*"})
})
public class DefaultJobManager
    extends StatisticsImpl
    implements Runnable, JobManager, EventHandler {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The environment component. */
    @Reference
    private EnvironmentComponent environment;

    /** The configuration manager. */
    @Reference
    private QueueConfigurationManager configManager;

    /** The scheduler service. */
    @Reference
    private Scheduler scheduler;

    /** Lock object for the queues map - we don't want to sync directly on the concurrent map. */
    private final Object queuesLock = new Object();

    /** All active queues. */
    private final Map<String, AbstractJobQueue> queues = new ConcurrentHashMap<String, AbstractJobQueue>();

    /** Main configuration. */
    private InternalQueueConfiguration mainConfiguration;

    /** Current statistics. */
    private final StatisticsImpl baseStatistics = new StatisticsImpl();

    /** Last update for current statistics. */
    private long lastUpdatedStatistics;

    /** All existing events. */
    private final Map<String, JobEvent> allEvents = new HashMap<String, JobEvent>();

    /** All existing events by topic. */
    private final Map<String, List<JobEvent>> allEventsByTopic = new HashMap<String, List<JobEvent>>();

    /** Statistics per topic. */
    private final ConcurrentMap<String, TopicStatistics> topicStatistics = new ConcurrentHashMap<String, TopicStatistics>();

    private static final boolean DEFAULT_ENABLED = true;

    @Property(boolValue=DEFAULT_ENABLED)
    private static final String PROP_ENABLED = "jobmanager.enabled";

    private boolean enabled = DEFAULT_ENABLED;

    /** We count the scheduler runs. */
    private long schedulerRuns;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        this.update(props);
        logger.info("Apache Sling Job Event Handler started on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * Configure this component.
     * @param props Configuration properties
     */
    @Modified
    protected void update(final Map<String, Object> props) {
        // create a new dictionary with the missing info and do some sanety puts
        final Map<String, Object> queueProps = new HashMap<String, Object>(props);
        queueProps.remove(ConfigurationConstants.PROP_APP_IDS);
        queueProps.put(ConfigurationConstants.PROP_TOPICS, "*");
        queueProps.put(ConfigurationConstants.PROP_NAME, "<main queue>");
        queueProps.put(ConfigurationConstants.PROP_RUN_LOCAL, false);
        queueProps.put(ConfigurationConstants.PROP_TYPE, InternalQueueConfiguration.Type.UNORDERED);

        // check max parallel - this should never be lower than 2!
        final int maxParallel = OsgiUtil.toInteger(queueProps.get(ConfigurationConstants.PROP_MAX_PARALLEL),
                ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        if ( maxParallel < 2 ) {
            this.logger.debug("Ignoring invalid setting of {} for {}. Setting to minimum value: 2",
                    maxParallel, ConfigurationConstants.PROP_MAX_PARALLEL);
            queueProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, 2);
        }
        this.mainConfiguration = InternalQueueConfiguration.fromConfiguration(queueProps);

        final boolean oldEnabled = this.enabled;
        this.enabled = OsgiUtil.toBoolean(props.get(PROP_ENABLED), DEFAULT_ENABLED);

        // if we have been disabled before and now get enabled, restart to get processing going
        if ( this.enabled != oldEnabled && this.enabled ) {
            this.restart();
        }
    }

    /**
     * Dectivate this component.
     */
    @Deactivate
    protected void deactivate() {
        final Iterator<AbstractJobQueue> i = this.queues.values().iterator();
        while ( i.hasNext() ) {
            final AbstractJobQueue jbq = i.next();
            jbq.close();
        }
        this.queues.clear();
        logger.info("Apache Sling Job Event Handler stopped on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * It searches for idle queues and stops them after a timeout. If a queue
     * is idle for two consecutive clean up calls, it is removed.
     * @see java.lang.Runnable#run()
     */
    private void cleanup() {
        // check for unprocessed jobs first
        for(final AbstractJobQueue jbq : this.queues.values() ) {
            jbq.checkForUnprocessedJobs();
        }

        // we only do a full clean up on every fifth run
        this.schedulerRuns++;
        final boolean doFullCleanUp = (schedulerRuns % 5 == 0);

        if ( doFullCleanUp ) {
            // check for idle queue
           // we synchronize to avoid creating a queue which is about to be removed during cleanup
            synchronized ( queuesLock ) {
                final Iterator<Map.Entry<String, AbstractJobQueue>> i = this.queues.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, AbstractJobQueue> current = i.next();
                    final AbstractJobQueue jbq = current.getValue();
                    if ( jbq.isMarkedForRemoval() ) {
                        // close
                        jbq.close();
                        // copy statistics
                        this.baseStatistics.add(jbq);
                        // remove
                        i.remove();
                    } else {
                        // mark to be removed during next cycle
                        jbq.markForRemoval();
                    }
                }
            }
        }
    }

    /**
     * Process a new job event.
     * This method first searches the corresponding queue - if such a queue
     * does not exist yet, it is created and started.
     * @param event The job event
     */
    public void process(final JobEvent event) {
        // are we disabled?
        if ( !this.enabled ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Job manager is disabled. Ignoring job {}", EventUtil.toString(event.event));
            }
            return;
        }
        // get the queue configuration
        InternalQueueConfiguration config = configManager.getQueueConfiguration(event);

        // if no queue config is found, we either create a new queue or use the main queue
        if ( config == null ) {
            final String customQueueName = (String)event.event.getProperty(JobUtil.PROPERTY_JOB_QUEUE_NAME);
            if ( customQueueName != null ) {
                synchronized ( queuesLock ) {
                    final AbstractJobQueue queue = this.queues.get(customQueueName);
                    if ( queue != null ) {
                        config = queue.getConfiguration();
                    } else {
                        config = new InternalQueueConfiguration(event.event);
                    }
                    event.queueName = customQueueName;
                }
            } else {
                config = this.mainConfiguration;
                event.queueName = this.mainConfiguration.getName();
            }
        }

        // get the queue name
        final String queueName = event.queueName;

        if ( config.isSkipped(event) ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Ignoring job due to configuration of queue {} : {}", queueName, EventUtil.toString(event.event));
            }
            return;
        }

        // drop?
        if ( config.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", queueName, EventUtil.toString(event.event));
            }
            Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_CANCELLED, event.event, null);
            event.finished();
            return;
        }

        // get or create queue
        AbstractJobQueue queue = null;
        // we synchronize to avoid creating a queue which is about to be removed during cleanup
        synchronized ( queuesLock ) {
            queue = this.queues.get(queueName);
            // check for reconfiguration, we really do an identity check here(!)
            if ( queue != null && queue.getConfiguration() != config ) {
                this.outdateQueue(queue);
                // we use a new queue with the configuration
                queue = null;
            }
            if ( queue == null ) {
                if ( config.getType() == QueueConfiguration.Type.ORDERED ) {
                    queue = new OrderedJobQueue(queueName, config, this.environment);
                } else if ( config.getType() == QueueConfiguration.Type.UNORDERED ) {
                    queue = new ParallelJobQueue(queueName, config, this.environment, this.scheduler);
                } else if ( config.getType() == QueueConfiguration.Type.TOPIC_ROUND_ROBIN ) {
                    queue = new TopicRoundRobinJobQueue(queueName, config, this.environment, this.scheduler);
                }
                if ( queue == null ) {
                    // this is just a sanety check, actually we can never get here
                    logger.warn("Ignoring event due to unknown queue type of queue {} : {}", queueName, EventUtil.toString(event.event));
                    return;
                }
                queues.put(queueName, queue);
                queue.start();
            }
        }

        // and put event
        queue.process(event);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        this.cleanup();
    }

    /**
     * Return our internal statistics object.
     * We recalculate this every 1.5sec (if requested)
     *
     * @see org.apache.sling.event.jobs.JobManager#getStatistics()
     */
    public synchronized Statistics getStatistics() {
        final long now = System.currentTimeMillis();
        if ( this.lastUpdatedStatistics + 1500 < now ) {
            this.copyFrom(this.baseStatistics);
            for(final AbstractJobQueue jq : this.queues.values() ) {
                this.add(jq);
            }
        }
        return this;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueue(java.lang.String)
     */
    public Queue getQueue(final String name) {
        return this.queues.get(name);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueues()
     */
    public Iterable<Queue> getQueues() {
        final Iterator<AbstractJobQueue> jqI = this.queues.values().iterator();
        return new Iterable<Queue>() {

            public Iterator<Queue> iterator() {
                return new Iterator<Queue>() {

                    public boolean hasNext() {
                        return jqI.hasNext();
                    }

                    public Queue next() {
                        return jqI.next();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public InternalQueueConfiguration getMainQueueConfiguration() {
        return this.mainConfiguration;
    }

    /**
     * Add a job to all jobs.
     */
    public void notifyAddJob(final JobEvent job) {
        final String key = job.uniqueId;
        final String topic = (String)job.event.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
        final JobEvent oldJob;
        synchronized ( this.allEvents ) {
            oldJob = this.allEvents.put(key, job);
        }
        List<JobEvent> l;
        synchronized ( this.allEventsByTopic ) {
            l = this.allEventsByTopic.get(topic);
            if ( l == null ) {
                l = new ArrayList<JobEvent>();
                this.allEventsByTopic.put(topic, l);
            }
        }
        synchronized ( l ) {
            if ( oldJob != null ) {
                l.remove(oldJob);
            }
            l.add(job);
        }
    }

    /**
     * Remove a job from all jobs.
     */
    public void notifyRemoveJob(final String key) {
        final JobEvent oldJob;
        synchronized ( this.allEvents ) {
            oldJob = this.allEvents.remove(key);
        }
        if ( oldJob != null ) {
            final String topic = (String)oldJob.event.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
            final List<JobEvent> l;
            synchronized ( this.allEventsByTopic ) {
                l = this.allEventsByTopic.get(topic);
            }
            if ( l != null ) {
                synchronized ( l ) {
                    l.remove(oldJob);
                }
            }
        }
    }

    /**
     * Job started
     */
    public void notifyActiveJob(final String key) {
        final JobEvent job;
        synchronized ( this.allEvents ) {
            job = this.allEvents.get(key);
        }
        if ( job != null ) {
            job.started = 1;
        }
    }

    /**
     * Job started
     */
    public void notifyRescheduleJob(final String key) {
        final JobEvent job;
        synchronized ( this.allEvents ) {
            job = this.allEvents.get(key);
        }
        if ( job != null ) {
            job.started = -1;
        }
    }

    /**
     * Check the requested job type
     */
    private boolean checkType(final QueryType type, final JobEvent event) {
        if ( type == QueryType.ALL ) {
            return true;
        }
        if ( type == QueryType.ACTIVE && event.started == 1 ) {
            return true;
        }
        if ( type == QueryType.QUEUED && event.started == -1 ) {
            return true;
        }
        return false;
    }

    private enum Operation {
        LESS,
        LESS_OR_EQUALS,
        EQUALS,
        GREATER_OR_EQUALS,
        GREATER
    }
    /**
     * Check if the job matches the template
     */
    private boolean match(final JobEvent job, final Map<String, Object> template) {
        if ( template != null ) {
            for(final Map.Entry<String, Object> current : template.entrySet()) {
                final String key = current.getKey();
                final char firstChar = key.length() > 0 ? key.charAt(0) : 0;
                final String propName;
                final Operation op;
                if ( firstChar == '=' ) {
                    propName = key.substring(1);
                    op  = Operation.EQUALS;
                } else if ( firstChar == '<' ) {
                    final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                    if ( secondChar == '=' ) {
                        op = Operation.LESS_OR_EQUALS;
                        propName = key.substring(2);
                    } else {
                        op = Operation.LESS;
                        propName = key.substring(1);
                    }
                } else if ( firstChar == '>' ) {
                    final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                    if ( secondChar == '=' ) {
                        op = Operation.GREATER_OR_EQUALS;
                        propName = key.substring(2);
                    } else {
                        op = Operation.GREATER;
                        propName = key.substring(1);
                    }
                } else {
                    propName = key;
                    op  = Operation.EQUALS;
                }
                final Object value = current.getValue();

                if ( op == Operation.EQUALS ) {
                    if ( !value.equals(job.event.getProperty(propName)) ) {
                        return false;
                    }
                } else {
                    if ( value instanceof Comparable ) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        final int result = ((Comparable)value).compareTo(job.event.getProperty(propName));
                        if ( op == Operation.LESS && result != -1 ) {
                            return false;
                        } else if ( op == Operation.LESS_OR_EQUALS && result == 1 ) {
                            return false;
                        } else if ( op == Operation.GREATER_OR_EQUALS && result == -1 ) {
                            return false;
                        } else if ( op == Operation.GREATER && result != 1 ) {
                            return false;
                        }
                    } else {
                        // if the value is not comparable we simply don't match
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean queryCollection(
            final List<Event> result,
            final QueryType type,
            final Collection<JobEvent> collection,
            final long limit,
            final Map<String, Object>... filterProps) {
        synchronized ( collection ) {
            final Iterator<JobEvent> iter = collection.iterator();
            while ( iter.hasNext() ) {
                final JobEvent job = iter.next();
                boolean add = checkType(type, job);
                if ( add && filterProps != null && filterProps.length != 0 ) {
                    add = false;
                    for (Map<String,Object> template : filterProps) {
                        add = this.match(job, template);
                        if ( add ) {
                            break;
                        }
                    }
                }
                if ( add ) {
                    result.add(job.event);
                    if ( limit > 0 && result.size() == limit ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#queryJobs(QueryType, java.lang.String, java.util.Map...)
     */
    public JobsIterator queryJobs(final QueryType type,
            final String topic,
            final Map<String, Object>... filterProps) {
        return this.queryJobs(type, topic, -1, filterProps);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#queryJobs(QueryType, java.lang.String, long, java.util.Map...)
     */
    public JobsIterator queryJobs(final QueryType type,
            final String topic,
            final long limit,
            final Map<String, Object>... filterProps) {
        final List<Event> result = new ArrayList<Event>();
        if ( topic != null ) {
            final List<JobEvent> l;
            synchronized ( this.allEventsByTopic ) {
                l = this.allEventsByTopic.get(topic);
            }
            if ( l != null ) {
                queryCollection(result, type, l, limit, filterProps);
            }
        } else {
            final Set<Collection<JobEvent>> topics;
            synchronized ( this.allEventsByTopic ) {
                topics = new HashSet<Collection<JobEvent>>(this.allEventsByTopic.values());
            }
            boolean done = false;
            final Iterator<Collection<JobEvent>> i = topics.iterator();
            while ( !done && i.hasNext() ) {
                final Collection<JobEvent> l = i.next();
                done = queryCollection(result, type, l, limit, filterProps);
            }
        }
        return new JobsIteratorImpl(result);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#findJob(java.lang.String, java.util.Map)
     */
    public Event findJob(final String topic, final Map<String, Object> template) {
        Event result = null;
        if ( topic != null ) {
            final List<JobEvent> l;
            synchronized ( this.allEventsByTopic ) {
                l = this.allEventsByTopic.get(topic);
            }
            if ( l != null ) {
                synchronized ( l ) {
                    final Iterator<JobEvent> iter = l.iterator();
                    while ( result == null && iter.hasNext() ) {
                        final JobEvent job = iter.next();
                        if ( match(job, template) ) {
                            result = job.event;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#removeJob(java.lang.String)
     */
    public boolean removeJob(final String jobId) {
        final JobEvent job;
        synchronized ( this.allEvents ) {
            job = this.allEvents.get(jobId);
        }
        boolean result = true;
        if ( job != null ) {
            if ( job.started != 1 ) {
                result = job.remove();
            } else {
                result = false;
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#forceRemoveJob(java.lang.String)
     */
    public void forceRemoveJob(final String jobId) {
        while ( !this.removeJob(jobId) ) {
            // instead of using complicated syncs, waits and notifies we simply poll
            try {
                Thread.sleep(80);
            } catch (final InterruptedException ignore) {
                this.ignoreException(ignore);
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

    /**
     * @see org.apache.sling.event.impl.jobs.StatisticsImpl#reset()
     * Reset this statistics and all queues.
     */
    public synchronized void reset() {
        this.baseStatistics.reset();
        for(final AbstractJobQueue jq : this.queues.values() ) {
            jq.reset();
        }
        this.topicStatistics.clear();
        this.lastUpdatedStatistics = 0;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getTopicStatistics()
     */
    public Iterable<TopicStatistics> getTopicStatistics() {
        return topicStatistics.values();
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        final Event job = (Event)event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
        if ( job != null ) {
            final String topic = (String)job.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
            if ( topic != null ) { // this is just a sanity check
                TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                if ( ts == null ) {
                    this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
                    ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                }
                if ( event.getTopic().equals(JobUtil.TOPIC_JOB_CANCELLED) ) {
                    ts.addCancelled();
                } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_FAILED) ) {
                    ts.addFailed();
                } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_FINISHED) ) {
                    final Long time = (Long)event.getProperty(Utility.PROPERTY_TIME);
                    ts.addFinished(time == null ? -1 : time);
                } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_STARTED) ) {
                    final Long time = (Long)event.getProperty(Utility.PROPERTY_TIME);
                    ts.addActivated(time == null ? -1 : time);
                }
            }
        }
    }

    private void outdateQueue(final AbstractJobQueue queue) {
        // remove the queue with the old name
        this.queues.remove(queue.getName());
        // check if we can close or have to rename
        queue.markForRemoval();
        if ( queue.isMarkedForRemoval() ) {
            // close
            queue.close();
            // copy statistics
            this.baseStatistics.add(queue);
        } else {
            // notify queue
            queue.rename(queue.getName() + "<outdated>(" + queue.hashCode() + ")");
            // readd with new name
            this.queues.put(queue.getName(), queue);
        }
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#restart()
     */
    public void restart() {
        // let's rename/close all queues first
        synchronized ( queuesLock ) {
            final List<AbstractJobQueue> queues = new ArrayList<AbstractJobQueue>(this.queues.values());
            for(final AbstractJobQueue queue : queues ) {
                this.outdateQueue(queue);
            }
        }
        // reset statistics
        this.reset();
        // restart all jobs - we first copy all of them
        final List<JobEvent> jobs;
        synchronized ( this.allEvents ) {
            jobs = new ArrayList<JobEvent>(this.allEvents.values());
            this.allEvents.clear();
        }
        synchronized ( this.allEventsByTopic ) {
            this.allEventsByTopic.clear();
        }
        for(final JobEvent job : jobs) {
            job.restart();
        }
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#isJobProcessingEnabled()
     */
    public boolean isJobProcessingEnabled() {
        return this.enabled;
    }
}
