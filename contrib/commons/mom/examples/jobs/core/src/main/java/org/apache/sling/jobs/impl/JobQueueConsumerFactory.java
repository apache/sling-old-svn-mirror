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

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.jobs.*;
import org.apache.sling.mom.*;
import org.apache.sling.mom.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created by ieb on 12/04/2016.
 * This is a configuration factory that creates QueueReader instances on configuration. These connect to the JobManager
 * service and are registered using the OSGi Whiteboard pattern with the QueueManager. The JobManager service must implement JobConsumer.
 *
 */
@Component(configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        immediate = true)
@Properties({
    @Property(name= QueueReader.QUEUE_NAME_PROP)
})
@Service(value = QueueReader.class)
public class JobQueueConsumerFactory implements QueueReader, MessageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueConsumerFactory.class);
    private static final Set<JobUpdate.JobUpdateCommand> ALLOWED_COMMANDS = ImmutableSet.of(JobUpdate.JobUpdateCommand.UPDATE_JOB) ;

    @Reference
    private JobManager jobManager;

    @Reference
    private TopicManager topicManager;
    @Reference
    private QueueManager queueManager;

    @Activate
    public void activate(Map<String, Object> properties) {
        if ( !(jobManager instanceof JobConsumer) ) {
            LOGGER.error("JobManager must implement JobConsumer interface. {} does not. ", jobManager.getClass());
            throw new IllegalStateException("JobManager does not implement JobConsumer");
        }
    }

    @Deactivate
    public void deactivate(@SuppressWarnings("UnusedParameters") Map<String, Object> properties) {
    }




    @Override
    public void onMessage(Types.QueueName queueName, Map<String, Object> message) throws RequeueMessageException {

        final Job job = new JobImpl(new JobUpdateImpl(message));


        ((JobConsumer)jobManager).execute(job, new JobUpdateListener() {
            @Override
            public void update(@Nonnull JobUpdate update) {
                if (update.getId() != job.getId() || !ALLOWED_COMMANDS.contains(update.getCommand())) {

                    throw new IllegalArgumentException("Not allowed to update other jobs or issue reserved commands when updating the state of a running job.");
                }
                topicManager.publish(update.getQueue().asTopicName(), update.getCommand().asCommandName(), Utils.toMapValue(update));
            }
        }, new JobCallback() {
            @Override
            public void callback(Job finalJobState) {
                if (finalJobState.getId() != job.getId()) {
                    throw new IllegalArgumentException("Final Job state ID must match initial JobState ID");
                }
                JobUpdate finalJobUpdate = finalJobState.newJobUpdateBuilder()
                        .command(JobUpdate.JobUpdateCommand.UPDATE_JOB)
                        .putAll(finalJobState.getProperties())
                        .build();
                topicManager.publish(finalJobUpdate.getQueue().asTopicName(), finalJobUpdate.getCommand().asCommandName(), Utils.toMapValue(finalJobUpdate));
            }
        });


    }

    @Override
    public boolean accept(Types.Name name, Map<String, Object> mapMessage) {
        return !(jobManager instanceof MessageFilter) || ((MessageFilter) jobManager).accept(name, mapMessage);
    }


}
