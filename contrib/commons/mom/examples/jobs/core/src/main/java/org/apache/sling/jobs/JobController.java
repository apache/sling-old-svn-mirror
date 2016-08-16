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

package org.apache.sling.jobs;

/**
 * Created by ieb on 30/03/2016.
 * A job controller provides a mechanism by which a running job can be sent control messages.
 * Jobs when they start should register a JobController implementation with the JobManager so that
 * it can act on control messages sent. Alternatively a TopicListener can be used to route
 * message to the Job, bypassing the manager, although this TopicListener will need to filter
 * for all messages associated with the Job.
 */
public interface JobController {


    /**
     * Stop the job as soon as appropriate.
     */
    void stop();

    /**
     * Abort the job immediately.
     */
    void abort();


}
