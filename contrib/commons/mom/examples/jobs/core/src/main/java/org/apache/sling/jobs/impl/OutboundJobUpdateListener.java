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

package org.apache.sling.jobs.impl;

import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.JobUpdateListener;
import org.apache.sling.jobs.Types;
import org.apache.sling.mom.QueueManager;
import org.apache.sling.mom.TopicManager;

import javax.annotation.Nonnull;

/**
 * Created by ieb on 30/03/2016.
 * Sends messages out to JMS Queues or topics. Normally called by the local JobManager Implementation.
 * Uses a TopicManager or QueueManager to perform the send operation.
 */
public class OutboundJobUpdateListener implements JobUpdateListener {


    private boolean active;
    private final TopicManager topicManager;
    private final QueueManager queueManager;

    public OutboundJobUpdateListener(TopicManager topicManager, QueueManager queueManager ) {
        this.topicManager = topicManager;
        this.queueManager = queueManager;
        active = true;
    }
    
    public void dispose() {
        active = false;
    }


    @Override
    public void update(@Nonnull JobUpdate update) {
        if ( active ) {
            switch(update.getCommand()) {
                case START_JOB:
                    queueManager.add(update.getQueue().asQueueName(), Utils.toMapValue(update));
                    break;
                default:
                    topicManager.publish(update.getQueue().asTopicName(), update.getCommand().asCommandName(), Utils.toMapValue(update));
                    break;
            }
        }
    }
}
