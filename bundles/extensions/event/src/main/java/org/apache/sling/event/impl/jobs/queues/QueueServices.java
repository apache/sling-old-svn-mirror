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
package org.apache.sling.event.impl.jobs.queues;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.osgi.service.event.EventAdmin;

/**
 * The queue services class is a helper class containing all
 * services used by the queue implementations.
 * This avoids passing a set of separate objects.
 */
public class QueueServices {

    public JobManagerConfiguration configuration;

    public JobConsumerManager jobConsumerManager;

    public EventAdmin eventAdmin;

    public ThreadPoolManager threadPoolManager;

    public Scheduler scheduler;

    public StatisticsManager statisticsManager;

    public ThreadPool eventingThreadPool;
}
