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
package org.apache.sling.event.impl.jobs;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.support.Environment;

/**
 * Configuration of the job handling
 *
 */
public class JobManagerConfiguration {

    /** Default repository path. */
    public static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/jobs";

    /** The path where all jobs are stored. */
    public static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    /** Default background load delay. */
    public static final long DEFAULT_BACKGROUND_LOAD_DELAY = 30;

    /** The background loader waits this time of seconds after startup before loading events from the repository. (in secs) */
    public static final String CONFIG_PROPERTY_BACKGROUND_LOAD_DELAY = "load.delay";

    /** Default for disabling the distribution. */
    public static final boolean DEFAULT_DISABLE_DISTRIBUTION = false;

    /** Configuration switch for distributing the jobs. */
    public static final String PROPERTY_DISABLE_DISTRIBUTION = "job.consumermanager.disableDistribution";

    /** The jobs base path with a slash. */
    private String jobsBasePathWithSlash;

    /** The base path for assigned jobs. */
    private String assignedJobsPath;

    /** The base path for unassigned jobs. */
    private String unassignedJobsPath;

    /** The base path for assigned jobs to the current instance. */
    private String localJobsPath;

    /** The base path for assigned jobs to the current instance - ending with a slash. */
    private String localJobsPathWithSlash;

    /** The base path for locks. */
    private String locksPath;

    private String previousVersionAnonPath;

    private String previousVersionIdentifiedPath;

    /** The base path for locks - ending with a slash. */
    private String locksPathWithSlash;

    private long backgroundLoadDelay;

    private boolean disabledDistribution;

    public JobManagerConfiguration(final Map<String, Object> props) {
        this.disabledDistribution = PropertiesUtil.toBoolean(props.get(PROPERTY_DISABLE_DISTRIBUTION), DEFAULT_DISABLE_DISTRIBUTION);
        this.jobsBasePathWithSlash = PropertiesUtil.toString(props.get(CONFIG_PROPERTY_REPOSITORY_PATH),
                            DEFAULT_REPOSITORY_PATH) + '/';

        // create initial resources
        this.assignedJobsPath = this.jobsBasePathWithSlash + "assigned";
        this.unassignedJobsPath = this.jobsBasePathWithSlash + "unassigned";
        this.locksPath = this.jobsBasePathWithSlash + "locks";
        this.locksPathWithSlash = this.locksPath.concat("/");

        this.localJobsPath = this.assignedJobsPath.concat("/").concat(Environment.APPLICATION_ID);
        this.localJobsPathWithSlash = this.localJobsPath.concat("/");

        this.previousVersionAnonPath = this.jobsBasePathWithSlash + "anon";
        this.previousVersionIdentifiedPath = this.jobsBasePathWithSlash + "identified";

        this.backgroundLoadDelay = PropertiesUtil.toLong(props.get(CONFIG_PROPERTY_BACKGROUND_LOAD_DELAY), DEFAULT_BACKGROUND_LOAD_DELAY);
    }

    /**
     * Get the resource path for all assigned jobs.
     * @return The path - does not end with a slash.
     */
    public String getAssginedJobsPath() {
        return this.assignedJobsPath;
    }

    /**
     * Get the resource path for all unassigned jobs.
     * @return The path - does not end with a slash.
     */
    public String getUnassignedJobsPath() {
        return this.unassignedJobsPath;
    }

    /**
     * Get the resource path for all jobs assigned to the current instance
     * @return The path - does not end with a slash
     */
    public String getLocalJobsPath() {
        return this.localJobsPath;
    }

    /**
     * Get the resource path for all locks
     * @return The path - does not end with a slash
     */
    public String getLocksPath() {
        return this.locksPath;
    }

    public long getBackgroundLoadDelay() {
        return backgroundLoadDelay;
    }

    /** Counter for jobs without an id. */
    private final AtomicLong jobCounter = new AtomicLong(0);

    /**
     * Create a unique job path (folder and name) for the job.
     */
    public String getUniquePath(final String targetId,
            final String topic,
            final String jobId,
            final Map<String, Object> jobProperties) {
        final boolean isBridged = (jobProperties != null ? jobProperties.containsKey(JobImpl.PROPERTY_BRIDGED_EVENT) : false);
        final String topicName = (isBridged ? JobImpl.PROPERTY_BRIDGED_EVENT : topic.replace('/', '.'));
        final StringBuilder sb = new StringBuilder();
        if ( targetId != null ) {
            sb.append(this.assignedJobsPath);
            sb.append('/');
            sb.append(targetId);
        } else {
            sb.append(this.unassignedJobsPath);
        }
        sb.append('/');
        sb.append(topicName);
        sb.append('/');
        sb.append(jobId);

        return sb.toString();
    }

    /**
     * Get the unique job id
     */
    public String getUniqueId(final String jobTopic) {
        final String convTopic = jobTopic.replace('/', '.');

        final Calendar now = Calendar.getInstance();
        final StringBuilder sb = new StringBuilder();
        sb.append(now.get(Calendar.YEAR));
        sb.append('/');
        sb.append(now.get(Calendar.MONTH) + 1);
        sb.append('/');
        sb.append(now.get(Calendar.DAY_OF_MONTH));
        sb.append('/');
        sb.append(now.get(Calendar.HOUR_OF_DAY));
        sb.append('/');
        sb.append(now.get(Calendar.MINUTE));
        sb.append('/');
        sb.append(convTopic);
        sb.append('_');
        sb.append(Environment.APPLICATION_ID);
        sb.append('_');
        sb.append(jobCounter.getAndIncrement());

        return sb.toString();
    }

    public boolean isLocalJob(final String jobPath) {
        return jobPath.startsWith(this.localJobsPathWithSlash);
    }

    public boolean isJob(final String jobPath) {
        return jobPath.startsWith(this.jobsBasePathWithSlash);
    }

    public boolean isLock(final String lockPath) {
        return lockPath.startsWith(this.locksPathWithSlash);
    }

    public String getPreviousVersionAnonPath() {
        return this.previousVersionAnonPath;
    }

    public String getPreviousVersionIdentifiedPath() {
        return this.previousVersionIdentifiedPath;
    }

    public boolean disableDistribution() {
        return this.disabledDistribution;
    }
}
