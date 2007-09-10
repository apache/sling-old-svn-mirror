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
package org.apache.sling.event;

import java.util.Collection;

import org.osgi.service.event.Event;

/**
 * This service provides the current job processing status.
 */
public interface JobStatusProvider {

    /**
     * Return a list of currently schedulded jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     */
    Collection<Event> scheduledJobs(String topic);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     */
    Collection<Event> getCurrentJobs(String topic);
}
