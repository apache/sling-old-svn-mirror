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
package org.apache.sling.event.jobs;

import java.util.Calendar;
import java.util.Set;

/**
 * A job
 *
 *
 * Property Types
 *
 * In general all scalar types and all serializable classes are supported as
 * property types. However, in order for deseralizing classes these must be
 * exported. Serializable classes are not searchable in the query either.
 * Due to the above to potential problems, it is advisable to not use
 * custom classes as job properties, but rather use out of the box supported
 * types in combination with collections.
 *
 * A resource provider might convert numbers to a different type, JCR is well-known
 * for this behavior as it only supports long but neither integer nor short.
 * Therefore if you are dealing with numbers, use the {@link #getProperty(String, Class)}
 * method to get the correct type instead of directly casting it.
 *
 * @since 1.2
 */
public interface Job {

    /**
     * The name of the job queue processing this job.
     * This property is set by the job handling when the job is processed.
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_QUEUE_NAME = "event.job.queuename";

    /**
     * This property is set by the job handling to define the priority of this job
     * execution.
     * The property is evaluated by the job handling before starting the
     * {@link JobConsumer} and sets the priority of the thread accordingly.
     * For possible values see {@link JobUtil.JobPriority}.
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_PRIORITY = "event.job.priority";

    /**
     * The property to track the retry count for jobs. Value is of type Integer.
     * On first execution the value of this property is zero.
     * This property is managed by the job handling.
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /**
     * The property to track the retry maximum retry count for jobs. Value is of type Integer.
     * This property is managed by the job handling.
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_RETRIES = "event.job.retries";

    /**
     * This property is set by the job handling and contains a calendar object
     * specifying the date and time when this job has been created.
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_CREATED = "slingevent:created";

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job has been created.
     */
    String PROPERTY_JOB_CREATED_INSTANCE = "slingevent:application";

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job should be processed.
     */
    String PROPERTY_JOB_TARGET_INSTANCE = "event.job.application";

    /**
     * This property is set by the job handling and contains a calendar object
     * specifying the date and time when this job has been started.
     * This property is only set if the job is currently in processing
     * If this property is set by the client creating the job it's value is ignored
     */
    String PROPERTY_JOB_STARTED_TIME = "event.job.started.time";

    /**
     * The job topic.
     * @return The job topic
     */
    String getTopic();

    /**
     * Optional job name
     * @return The job name or <code>null</code>
     */
    String getName();

    /**
     * Unique job ID.
     * @return The unique job ID.
     */
    String getId();

    /**
     * Get the value of a property.
     * @param name The property name
     * @return The value of the property or <code>null</code>
     */
    Object getProperty(String name);

    /**
     * Get all property names.
     * @return A set of property names.
     */
    Set<String> getPropertyNames();

    /**
     * Get a named property and convert it into the given type.
     * This method does not support conversion into a primitive type or an
     * array of a primitive type. It should return <code>null</code> in this
     * case.
     *
     * @param name The name of the property
     * @param type The class of the type
     * @return Return named value converted to type T or <code>null</code> if
     *         non existing or can't be converted.
     */
    <T> T getProperty(String name, Class<T> type);

    /**
     * Get a named property and convert it into the given type.
     * This method does not support conversion into a primitive type or an
     * array of a primitive type. It should return the default value in this
     * case.
     *
     * @param name The name of the property
     * @param defaultValue The default value to use if the named property does
     *            not exist or cannot be converted to the requested type. The
     *            default value is also used to define the type to convert the
     *            value to. If this is <code>null</code> any existing property is
     *            not converted.
     * @return Return named value converted to type T or the default value if
     *         non existing or can't be converted.
     */
    <T> T getProperty(String name, T defaultValue);

    /**
     * This property is set by the job handling to define the priority of this job
     * execution.
     * The property is evaluated by the job handling before starting the
     * {@link JobConsumer} and sets the priority of the thread accordingly.
     * For possible values see {@link JobUtil.JobPriority}.
     */
    JobUtil.JobPriority getJobPriority();

    /**
     * On first execution the value of this property is zero.
     * This property is managed by the job handling.
     */
    int getRetryCount();

    /**
     * The property to track the retry maximum retry count for jobs.
     * This property is managed by the job handling.
     */
    int getNumberOfRetries();

    /**
     * The name of the job queue processing this job.
     * This property is set by the job handling when the job is processed.
     * @return The queue name or <code>null</code>
     */
    String getQueueName();

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job should be processed.
     * @return The sling ID or <code>null</code>
     */
    String getTargetInstance();

    /**
     * This property is set by the job handling and contains a calendar object
     * specifying the date and time when this job has been started.
     * This property is only set if the job is currently in processing
     */
    Calendar getProcessingStarted();

    /**
     * This property is set by the job handling and contains a calendar object
     * specifying the date and time when this job has been created.
     */
    Calendar getCreated();

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job has been created.
     */
    String getCreatedInstance();
}
