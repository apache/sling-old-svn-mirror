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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.InternalJobState;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;

/**
 * The statistics manager keeps track of all statistics related tasks.
 */
@Component
@Service(value=StatisticsManager.class)
public class StatisticsManager {

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

    public void jobEnded(final String queueName,
            final String topic,
            final InternalJobState state,
            final long processingTime) {
        final StatisticsImpl queueStats = getStatisticsForQueue(queueName);

        TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
        if ( ts == null ) {
            this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
            ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
        }

        if ( state == InternalJobState.CANCELLED ) {
            ts.addCancelled();
            this.globalStatistics.cancelledJob();
            if ( queueStats != null ) {
                queueStats.cancelledJob();
            }

        } else if ( state == InternalJobState.FAILED ) {
            ts.addFailed();
            this.globalStatistics.failedJob();
            if ( queueStats != null ) {
                queueStats.failedJob();
            }

        } else if ( state == InternalJobState.SUCCEEDED ) {
            ts.addFinished(processingTime);
            this.globalStatistics.finishedJob(processingTime);
            if ( queueStats != null ) {
                queueStats.finishedJob(processingTime);
            }

        }
    }

    public void jobStarted(final String queueName,
            final String topic,
            final long queueTime) {
        final StatisticsImpl queueStats = getStatisticsForQueue(queueName);

        TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
        if ( ts == null ) {
            this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
            ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
        }

        ts.addActivated(queueTime);
        this.globalStatistics.addActive(queueTime);
        if ( queueStats != null ) {
            queueStats.addActive(queueTime);
        }
    }

    public void jobQueued(final String queueName,
            final String topic) {
        final StatisticsImpl queueStats = getStatisticsForQueue(queueName);

        this.globalStatistics.incQueued();
        if ( queueStats != null ) {
            queueStats.incQueued();
        }
    }

    public void jobDequeued(final String queueName, final String topic) {
        final StatisticsImpl queueStats = getStatisticsForQueue(queueName);

        this.globalStatistics.decQueued();
        if ( queueStats != null ) {
            queueStats.decQueued();
        }
    }
}
