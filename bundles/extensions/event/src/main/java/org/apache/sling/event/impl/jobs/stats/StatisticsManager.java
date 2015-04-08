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
package org.apache.sling.event.impl.jobs.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.notifications.NotificationUtility;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * The statistics manager keeps track of all statistics related tasks.
 * The statistics are updated through OSGi event by using the special
 * job notification events.
 */
@Component(immediate=true)
@Service(value={EventHandler.class, StatisticsManager.class})
@Properties({
    @Property(name=EventConstants.EVENT_TOPIC,
          value={NotificationConstants.TOPIC_JOB_ADDED,
                 NotificationConstants.TOPIC_JOB_STARTED,
                 NotificationConstants.TOPIC_JOB_CANCELLED,
                 NotificationConstants.TOPIC_JOB_FAILED,
                 NotificationConstants.TOPIC_JOB_FINISHED,
                 NotificationConstants.TOPIC_JOB_REMOVED})
})
public class StatisticsManager implements EventHandler {

    /** The job manager configuration. */
    @Reference
    private JobManagerConfiguration configuration;

    /** Global statistics. */
    private final StatisticsImpl globalStatistics = new StatisticsImpl() {

        @Override
        public synchronized void reset() {
            super.reset();
            topicStatistics.clear();
            for(final Statistics s : queueStatistics.values()) {
                s.reset();
            }
        }

    };

    /** Statistics per topic. */
    private final ConcurrentMap<String, TopicStatistics> topicStatistics = new ConcurrentHashMap<String, TopicStatistics>();

    /** Statistics per queue. */
    private final ConcurrentMap<String, Statistics> queueStatistics = new ConcurrentHashMap<String, Statistics>();

    /**
     * Get the global statistics.
     * @return The global statistics.
     */
    public Statistics getGlobalStatistics() {
        return this.globalStatistics;
    }

    /**
     * Get all topic statistics.
     * @return The map of topic statistics by topic.
     */
    public Map<String, TopicStatistics> getTopicStatistics() {
        return topicStatistics;
    }

    /**
     * Get a single queue statistics.
     * @param queueName The queue name.
     * @return The statistics for that queue.
     */
    public Statistics getQueueStatistics(final String queueName) {
        Statistics queueStats = queueStatistics.get(queueName);
        if ( queueStats == null ) {
            queueStats = new StatisticsImpl();
        }
        return queueStats;
    }

    /**
     * Internal method to get the statistics of a queue.
     * @param queueName The queue name.
     * @return The statistics or {@code null} if queue name is {@code null}.
     */
    private StatisticsImpl getStatisticsForQueue(final String queueName) {
        if ( queueName == null ) {
            return null;
        }
        StatisticsImpl queueStats = (StatisticsImpl)queueStatistics.get(queueName);
        if ( queueStats == null ) {
            queueStatistics.putIfAbsent(queueName, new StatisticsImpl());
            queueStats = (StatisticsImpl)queueStatistics.get(queueName);
        }
        return queueStats;
    }

    /**
     * Handle all job notification events and update the statistics.
     */
    @Override
    public void handleEvent(final Event event) {
        final String topic = (String)event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
        if ( topic != null ) { // this is just a sanity check
            final String queueName = (String)event.getProperty(Job.PROPERTY_JOB_QUEUE_NAME);
            final StatisticsImpl queueStats = getStatisticsForQueue(queueName);

            TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
            if ( ts == null ) {
                this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
                ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
            }

            if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_ADDED) ) {
                this.globalStatistics.incQueued();
                if ( queueStats != null ) {
                    queueStats.incQueued();
                }

            } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_CANCELLED) ) {
                ts.addCancelled();
                this.globalStatistics.cancelledJob();
                if ( queueStats != null ) {
                    queueStats.cancelledJob();
                }

            } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_FAILED) ) {
                ts.addFailed();
                this.globalStatistics.failedJob();
                if ( queueStats != null ) {
                    queueStats.failedJob();
                }

            } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_FINISHED) ) {
                final Long time = (Long)event.getProperty(NotificationUtility.PROPERTY_TIME);
                ts.addFinished(time == null ? -1 : time);
                this.globalStatistics.finishedJob(time == null ? -1 : time);
                if ( queueStats != null ) {
                    queueStats.finishedJob(time == null ? -1 : time);
                }

            } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_STARTED) ) {
                final Long time = (Long)event.getProperty(NotificationUtility.PROPERTY_TIME);
                ts.addActivated(time == null ? -1 : time);
                this.globalStatistics.addActive(time == null ? -1 : time);
                if ( queueStats != null ) {
                    queueStats.addActive(time == null ? -1 : time);
                }

            } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_REMOVED) ) {
                this.globalStatistics.decQueued();
                this.globalStatistics.cancelledJob();
                if ( queueStats != null ) {
                    queueStats.decQueued();
                    queueStats.cancelledJob();
                }
            }
        }
    }
}
