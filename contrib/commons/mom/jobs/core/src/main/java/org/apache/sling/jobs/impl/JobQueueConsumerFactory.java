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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobCallback;
import org.apache.sling.jobs.JobConsumer;
import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.JobUpdateListener;
import org.apache.sling.mom.MessageFilter;
import org.apache.sling.mom.QueueManager;
import org.apache.sling.mom.QueueReader;
import org.apache.sling.mom.RequeueMessageException;
import org.apache.sling.mom.TopicManager;
import org.apache.sling.mom.Types;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a configuration factory that creates QueueReader instances on configuration. These connect to the JobManager
 * service and are registered using the OSGi Whiteboard pattern with the QueueManager. The JobManager service must implement JobConsumer.
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = QueueReader.class)
@Designate(factory=true, ocd=JobQueueConsumerFactory.Config.class)
public class JobQueueConsumerFactory implements QueueReader, MessageFilter {

    @ObjectClassDefinition()
    public @interface Config {

        @AttributeDefinition(name = "queue-name")
        String queuename();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(JobQueueConsumerFactory.class);
    private static final Set<JobUpdate.JobUpdateCommand> ALLOWED_COMMANDS = Collections.unmodifiableSet(Collections.singleton(JobUpdate.JobUpdateCommand.UPDATE_JOB));

    @Reference
    private JobManager jobManager;

    @Reference
    private TopicManager topicManager;
    @Reference
    private QueueManager queueManager;

    @Activate
    public void activate() {
        if ( !(jobManager instanceof JobConsumer) ) {
            LOGGER.error("JobManager must implement JobConsumer interface. {} does not. ", jobManager.getClass());
            throw new IllegalStateException("JobManager does not implement JobConsumer");
        }
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
