/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Scheduler",
    description = "The scheduler is able to run services and jobs at specific times or periodically based on cron expressions."
)
@interface QuartzSchedulerConfiguration {

    @AttributeDefinition(
        name = "Thread Pool Name",
        description = "The name of a configured thread pool - if no name is configured the default pool is used."
    )
    String poolName() default ThreadPoolManager.DEFAULT_THREADPOOL_NAME;

    @AttributeDefinition(
        name = "Allowed Thread Pools",
        description="The names of thread pools that are allowed to be used by jobs. If a job is using a pool not in this list, the default pool is used."
    )
    String[] allowedPoolNames();

}
