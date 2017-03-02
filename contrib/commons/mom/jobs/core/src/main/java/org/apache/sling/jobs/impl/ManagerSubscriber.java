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

import java.util.Map;

import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.JobUpdateListener;
import org.apache.sling.mom.Subscriber;
import org.apache.sling.mom.Types;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Listens to a topic to retrieve control messages.
 */
@Component(service = Subscriber.class,
           property = {
                   Subscriber.TOPIC_NAMES_PROP + "=sling/jobupdates"
           })
public class ManagerSubscriber implements Subscriber {

    @Reference
    private JobManager jobManager;


    @Override
    public void onMessage(Types.TopicName topic, Map<String, Object> message) {
        if (jobManager instanceof JobUpdateListener) {
            ((JobUpdateListener) jobManager).update(new JobUpdateImpl(message));
        }
    }

}
